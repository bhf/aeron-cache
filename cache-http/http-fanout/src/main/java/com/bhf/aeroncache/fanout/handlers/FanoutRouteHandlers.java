package com.bhf.aeroncache.fanout.handlers;

import com.bhf.aeroncache.fanout.aggregate.Aggregators;
import com.bhf.aeroncache.fanout.client.GatewayFanoutClient;
import com.bhf.aeroncache.http.responses.AggregatedPurgeResponse;
import com.bhf.aeroncache.http.responses.CacheDetails;
import com.bhf.aeroncache.http.responses.CacheStats;
import com.bhf.aeroncache.http.responses.ClusterToolsResponse;
import com.bhf.aeroncache.http.responses.GetTimersResponse;
import com.bhf.aeroncache.models.counters.AeronCountersFanoutResponse;
import com.bhf.aeroncache.models.snapshot.SnapshotInfoResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import io.javalin.http.Context;
import lombok.extern.log4j.Log4j2;

import java.util.List;

/**
 * Javalin handlers for the fan-out aggregator. Each handler scatters the matching request to every
 * downstream gateway via {@link GatewayFanoutClient} and gathers the responses with {@link Aggregators}
 * into a single result that keeps the same JSON shape the single {@code http-server-javalin} serves.
 */
@Log4j2
public class FanoutRouteHandlers {

    private static final TypeReference<List<CacheDetails>> CACHE_DETAILS_LIST = new TypeReference<>() {
    };
    private static final TypeReference<CacheStats> CACHE_STATS = new TypeReference<>() {
    };
    private static final TypeReference<GetTimersResponse> TIMERS = new TypeReference<>() {
    };
    private static final TypeReference<SnapshotInfoResponse> SNAPSHOT_INFO = new TypeReference<>() {
    };
    private static final TypeReference<AeronCountersFanoutResponse> AERON_COUNTERS = new TypeReference<>() {
    };
    private static final TypeReference<ClusterToolsResponse> CLUSTER_TOOLS = new TypeReference<>() {
    };
    private static final TypeReference<AggregatedPurgeResponse> AGGREGATED_PURGE = new TypeReference<>() {
    };

    private final GatewayFanoutClient client;

    /**
     * @param client the fan-out client wired to the configured gateways.
     */
    public FanoutRouteHandlers(GatewayFanoutClient client) {
        this.client = client;
    }

    /** GET /api/v1/caches - union of every gateway's caches. */
    public void handleGetCaches(Context ctx) {
        ctx.json(Aggregators.mergeCacheDetails(client.getAll("/api/v1/caches", CACHE_DETAILS_LIST)));
    }

    /** GET /api/v1/counters-caches - union of every gateway's counter caches. */
    public void handleGetCounterCaches(Context ctx) {
        ctx.json(Aggregators.mergeCacheDetails(client.getAll("/api/v1/counters-caches", CACHE_DETAILS_LIST)));
    }

    /** GET /api/v1/stats - field-wise sum of every gateway's cache stats. */
    public void handleGetStats(Context ctx) {
        ctx.json(Aggregators.mergeStats(client.getAll("/api/v1/stats", CACHE_STATS)));
    }

    /** GET /api/v1/counters-stats - field-wise sum of every gateway's counter stats. */
    public void handleGetCounterStats(Context ctx) {
        ctx.json(Aggregators.mergeStats(client.getAll("/api/v1/counters-stats", CACHE_STATS)));
    }

    /** GET /api/v1/timers - union of every gateway's pending timers. */
    public void handleGetTimers(Context ctx) {
        ctx.json(Aggregators.mergeTimers(client.getAll("/api/v1/timers", TIMERS)));
    }

    /** GET /api/v1/snapshot-info - a representative snapshot-info across gateways. */
    public void handleGetSnapshotInfo(Context ctx) {
        ctx.json(Aggregators.representativeSnapshotInfo(client.getAll("/api/v1/snapshot-info", SNAPSHOT_INFO)));
    }

    /**
     * GET /api/v1/multi/snapshot-info - each gateway's own snapshot-info as a per-backend list,
     * {@code [{gateway, ok, value, error}, ...]}, rather than the single representative picked by
     * {@link #handleGetSnapshotInfo}. Lets a multi-cache client show every backend's snapshot state.
     */
    public void handleGetSnapshotInfoMulti(Context ctx) {
        ctx.json(client.getAll("/api/v1/snapshot-info", SNAPSHOT_INFO));
    }

    /** GET /api/v1/aeron-counters - every gateway's per-node driver counters, flattened. */
    public void handleGetAeronCounters(Context ctx) {
        ctx.json(Aggregators.mergeAeronCounters(client.getAll("/api/v1/aeron-counters", AERON_COUNTERS)));
    }

    /** POST /api/v1/snapshot - trigger a snapshot on every gateway's cluster. */
    public void handleSnapshot(Context ctx) {
        ctx.json(Aggregators.representativeClusterTools(
                client.postAll("/api/v1/snapshot", "", CLUSTER_TOOLS), "snapshot", null));
    }

    /**
     * POST /api/v1/multi/snapshot - trigger a snapshot on every gateway's cluster and return the
     * per-backend clustertools results as a list, {@code [{gateway, ok, value, error}, ...]}, rather
     * than the single representative collapsed by {@link #handleSnapshot}, so a multi-cache client can
     * see which backend failed to snapshot.
     */
    public void handleSnapshotMulti(Context ctx) {
        ctx.json(client.postAll("/api/v1/snapshot", "", CLUSTER_TOOLS));
    }

    /** POST /api/v1/shutdown - shut down every gateway's cluster. */
    public void handleShutdown(Context ctx) {
        ctx.json(Aggregators.representativeClusterTools(
                client.postAll("/api/v1/shutdown", "", CLUSTER_TOOLS), "shutdown", null));
    }

    /** POST /api/v1/snapshot-and-purge - snapshot-and-purge every gateway's cluster, aggregated. */
    public void handleSnapshotAndPurge(Context ctx) {
        ctx.json(Aggregators.mergeSnapshotAndPurge(
                client.postAll("/api/v1/snapshot-and-purge", "", AGGREGATED_PURGE)));
    }

    /**
     * POST /api/v1/multi/snapshot-and-purge - snapshot-and-purge every gateway's cluster and return
     * each backend's own aggregated purge response as a per-backend list,
     * {@code [{gateway, ok, value, error}, ...]}, rather than the single merged result collapsed by
     * {@link #handleSnapshotAndPurge}, so a multi-cache client can see each backend's reclaimed bytes
     * and node breakdown separately.
     */
    public void handleSnapshotAndPurgeMulti(Context ctx) {
        ctx.json(client.postAll("/api/v1/snapshot-and-purge", "", AGGREGATED_PURGE));
    }
}
