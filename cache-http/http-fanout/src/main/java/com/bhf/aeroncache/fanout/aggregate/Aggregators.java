package com.bhf.aeroncache.fanout.aggregate;

import com.bhf.aeroncache.fanout.client.GatewayResult;
import com.bhf.aeroncache.http.responses.AggregatedPurgeResponse;
import com.bhf.aeroncache.http.responses.CacheDetails;
import com.bhf.aeroncache.http.responses.CacheStats;
import com.bhf.aeroncache.http.responses.ClusterToolsResponse;
import com.bhf.aeroncache.http.responses.GetTimersResponse;
import com.bhf.aeroncache.http.responses.TimerInfo;
import com.bhf.aeroncache.models.counters.AeronCountersFanoutResponse;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.snapshot.SnapshotInfoResponse;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure functions that merge the per-gateway {@link GatewayResult}s into a single response with the
 * same JSON shape the single {@code http-server-javalin} gateway already serves.
 *
 * <p>The topology is partitioned: a given {@code cacheId} lives on exactly one gateway, so lists are
 * concatenated (unioned) and scalar stats are summed. A failed gateway is excluded from sums, or
 * surfaced as an unreachable/failed contributor where the response shape carries per-node detail.
 */
public final class Aggregators {

    private Aggregators() {
    }

    /**
     * Union the caches reported by every gateway, de-duplicating by {@code cacheId} defensively (the
     * partitioned topology should already keep them disjoint).
     *
     * @param results the per-gateway cache listings.
     * @return the merged cache list.
     */
    public static List<CacheDetails> mergeCacheDetails(List<GatewayResult<List<CacheDetails>>> results) {
        List<CacheDetails> merged = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (var result : results) {
            if (!result.ok() || result.value() == null) {
                continue;
            }
            for (var details : result.value()) {
                if (seen.add(details.cacheId())) {
                    merged.add(details);
                }
            }
        }
        return merged;
    }

    /**
     * Sum the cache stats across every gateway field-by-field.
     *
     * @param results the per-gateway stats.
     * @return the summed stats.
     */
    public static CacheStats mergeStats(List<GatewayResult<CacheStats>> results) {
        int ops = 0;
        int caches = 0;
        int items = 0;
        int errors = 0;
        for (var result : results) {
            if (!result.ok() || result.value() == null) {
                continue;
            }
            var s = result.value();
            ops += s.totalOpsCount();
            caches += s.totalCachesCount();
            items += s.totalItemsCount();
            errors += s.errorCount();
        }
        return new CacheStats(ops, caches, items, errors);
    }

    /**
     * Union the pending timers across every gateway. The aggregate status is {@code SUCCESS} only
     * when every gateway responded and each reported {@code SUCCESS}; otherwise {@code ERROR}.
     *
     * @param results the per-gateway timer responses.
     * @return the merged timers response.
     */
    public static GetTimersResponse mergeTimers(List<GatewayResult<GetTimersResponse>> results) {
        List<TimerInfo> timers = new ArrayList<>();
        boolean allOk = !results.isEmpty();
        for (var result : results) {
            if (!result.ok() || result.value() == null) {
                allOk = false;
                continue;
            }
            var response = result.value();
            if (response.operationStatus() != CacheOperationStatus.SUCCESS) {
                allOk = false;
            }
            if (response.timers() != null) {
                timers.addAll(response.timers());
            }
        }
        return new GetTimersResponse(allOk ? CacheOperationStatus.SUCCESS : CacheOperationStatus.ERROR, timers);
    }

    /**
     * Flatten the per-node Aeron counters from every gateway into one node list. A gateway that could
     * not be reached contributes a single unreachable node entry keyed by its base URL, so the
     * aggregate still accounts for it.
     *
     * @param results the per-gateway fan-out responses.
     * @return the merged fan-out response.
     */
    public static AeronCountersFanoutResponse mergeAeronCounters(
            List<GatewayResult<AeronCountersFanoutResponse>> results) {
        List<AeronCountersFanoutResponse.NodeCounters> nodes = new ArrayList<>();
        Set<String> modes = new LinkedHashSet<>();
        for (var result : results) {
            if (!result.ok() || result.value() == null) {
                nodes.add(new AeronCountersFanoutResponse.NodeCounters(result.gateway(), false, null));
                continue;
            }
            var response = result.value();
            if (response.mode() != null) {
                modes.add(response.mode());
            }
            if (response.nodes() != null) {
                nodes.addAll(response.nodes());
            }
        }
        String mode = modes.size() == 1 ? modes.iterator().next() : "MIXED";
        return new AeronCountersFanoutResponse(mode, nodes.size(), nodes);
    }

    /**
     * Combine the per-gateway snapshot-and-purge results. Every gateway's node results are
     * concatenated and reclaimed bytes summed; a gateway that failed outright contributes a failed
     * node entry keyed by its base URL. The aggregate succeeds only when every gateway succeeded.
     *
     * @param results the per-gateway aggregated purge responses.
     * @return the merged aggregated purge response.
     */
    public static AggregatedPurgeResponse mergeSnapshotAndPurge(
            List<GatewayResult<AggregatedPurgeResponse>> results) {
        List<AggregatedPurgeResponse.NodePurgeResult> nodes = new ArrayList<>();
        long reclaimed = 0L;
        boolean allOk = !results.isEmpty();
        for (var result : results) {
            if (!result.ok() || result.value() == null) {
                allOk = false;
                nodes.add(new AggregatedPurgeResponse.NodePurgeResult(result.gateway(), false, 0L,
                        "gateway unreachable: " + result.error()));
                continue;
            }
            var response = result.value();
            if (!response.success()) {
                allOk = false;
            }
            reclaimed += response.reclaimedBytes();
            if (response.nodes() != null) {
                nodes.addAll(response.nodes());
            }
        }
        String message = allOk
                ? "Snapshot taken and purged across " + results.size() + " gateway(s); reclaimed " + reclaimed + " bytes"
                : "Snapshot-and-purge completed with errors on one or more gateways; reclaimed " + reclaimed + " bytes";
        return new AggregatedPurgeResponse(allOk, reclaimed, message, nodes);
    }

    /**
     * Collapse the per-gateway clustertools responses (snapshot / shutdown) into one representative
     * response. The aggregate exit code is 0 only when every gateway responded with exit code 0;
     * otherwise -1.
     *
     * @param results      the per-gateway responses.
     * @param tool         the clustertools command name to report.
     * @param folderFallback the cluster folder to report when no gateway supplied one.
     * @return the representative response.
     */
    public static ClusterToolsResponse representativeClusterTools(List<GatewayResult<ClusterToolsResponse>> results,
                                                                  String tool, String folderFallback) {
        boolean allOk = !results.isEmpty();
        String folder = folderFallback;
        for (var result : results) {
            if (!result.ok() || result.value() == null) {
                allOk = false;
                continue;
            }
            if (result.value().exitCode() != 0) {
                allOk = false;
            }
            if (folder == null && result.value().clusterFolder() != null) {
                folder = result.value().clusterFolder();
            }
        }
        return new ClusterToolsResponse(tool, folder, allOk ? 0 : -1);
    }

    /**
     * Pick a representative snapshot-info response across gateways (each fronts a distinct cluster, so
     * a single {@link SnapshotInfoResponse} cannot describe all of them). Prefers the first gateway
     * reporting a present snapshot, then the first that responded, else an empty "no snapshot" result
     * - matching the single gateway's own per-node selection.
     *
     * @param results the per-gateway snapshot-info responses.
     * @return the representative response.
     */
    public static SnapshotInfoResponse representativeSnapshotInfo(List<GatewayResult<SnapshotInfoResponse>> results) {
        SnapshotInfoResponse firstPresent = null;
        SnapshotInfoResponse firstResponded = null;
        for (var result : results) {
            if (!result.ok() || result.value() == null) {
                continue;
            }
            if (firstResponded == null) {
                firstResponded = result.value();
            }
            if (result.value().snapshotPresent()) {
                firstPresent = result.value();
                break;
            }
        }
        if (firstPresent != null) {
            return firstPresent;
        }
        if (firstResponded != null) {
            return firstResponded;
        }
        return new SnapshotInfoResponse(false, -1, -1, -1, -1, 0, 0, null);
    }
}
