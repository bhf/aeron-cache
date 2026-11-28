package com.bhf.aeroncache.fanout.aggregate;

import com.bhf.aeroncache.fanout.client.GatewayResult;
import com.bhf.aeroncache.http.responses.AggregatedPurgeResponse;
import com.bhf.aeroncache.http.responses.CacheDetails;
import com.bhf.aeroncache.http.responses.CacheStats;
import com.bhf.aeroncache.http.responses.ClusterToolsResponse;
import com.bhf.aeroncache.http.responses.GetTimersResponse;
import com.bhf.aeroncache.http.responses.TimerInfo;
import com.bhf.aeroncache.models.counters.AeronCountersFanoutResponse;
import com.bhf.aeroncache.models.counters.AeronCountersResponse;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.snapshot.SnapshotInfoResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AggregatorsTest {

    @Test
    void mergeCacheDetailsUnionsAcrossGatewaysAndDedupes() {
        // Arrange
        var a = GatewayResult.ok("g1", List.of(new CacheDetails("orders", 3), new CacheDetails("trades", 5)));
        var b = GatewayResult.ok("g2", List.of(new CacheDetails("positions", 2), new CacheDetails("orders", 9)));
        var failed = GatewayResult.<List<CacheDetails>>failure("g3", "down");

        // Act
        var merged = Aggregators.mergeCacheDetails(List.of(a, b, failed));

        // Assert - union with the first "orders" kept, failed gateway excluded.
        assertEquals(3, merged.size());
        assertEquals(List.of("orders", "trades", "positions"), merged.stream().map(CacheDetails::cacheId).toList());
        assertEquals(3, merged.get(0).itemCount());
    }

    @Test
    void mergeStatsSumsFieldsAcrossSuccessfulGateways() {
        // Arrange
        var a = GatewayResult.ok("g1", new CacheStats(10, 2, 100, 1));
        var b = GatewayResult.ok("g2", new CacheStats(5, 3, 50, 0));
        var failed = GatewayResult.<CacheStats>failure("g3", "timeout");

        // Act
        var merged = Aggregators.mergeStats(List.of(a, b, failed));

        // Assert
        assertEquals(new CacheStats(15, 5, 150, 1), merged);
    }

    @Test
    void mergeTimersConcatenatesAndDowngradesStatusOnFailure() {
        // Arrange
        var a = GatewayResult.ok("g1", new GetTimersResponse(CacheOperationStatus.SUCCESS,
                List.of(new TimerInfo("cache", "orders", "k1", 123L))));
        var failed = GatewayResult.<GetTimersResponse>failure("g2", "down");

        // Act
        var allOk = Aggregators.mergeTimers(List.of(a, GatewayResult.ok("g2",
                new GetTimersResponse(CacheOperationStatus.SUCCESS, List.of()))));
        var withFailure = Aggregators.mergeTimers(List.of(a, failed));

        // Assert
        assertEquals(CacheOperationStatus.SUCCESS, allOk.operationStatus());
        assertEquals(1, allOk.timers().size());
        assertEquals(CacheOperationStatus.ERROR, withFailure.operationStatus());
        assertEquals(1, withFailure.timers().size());
    }

    @Test
    void mergeAeronCountersFlattensNodesAndMarksUnreachableGateways() {
        // Arrange
        var counters = new AeronCountersResponse("/dev/shm/aeron", 1, 0L, 0, List.of());
        var g1 = GatewayResult.ok("g1", new AeronCountersFanoutResponse("CLUSTERED", 1,
                List.of(new AeronCountersFanoutResponse.NodeCounters("node-a", true, counters))));
        var g2down = GatewayResult.<AeronCountersFanoutResponse>failure("http://g2:7070", "refused");

        // Act
        var merged = Aggregators.mergeAeronCounters(List.of(g1, g2down));

        // Assert - one real node plus one synthetic unreachable entry for the failed gateway.
        assertEquals(2, merged.nodeCount());
        assertEquals(2, merged.nodes().size());
        assertTrue(merged.nodes().get(0).reachable());
        assertFalse(merged.nodes().get(1).reachable());
        assertEquals("http://g2:7070", merged.nodes().get(1).host());
        assertEquals("CLUSTERED", merged.mode());
    }

    @Test
    void mergeAeronCountersReportsMixedMode() {
        // Arrange
        var a = GatewayResult.ok("g1", new AeronCountersFanoutResponse("CLUSTERED", 0, List.of()));
        var b = GatewayResult.ok("g2", new AeronCountersFanoutResponse("EPHEMERAL", 0, List.of()));

        // Act
        var merged = Aggregators.mergeAeronCounters(List.of(a, b));

        // Assert
        assertEquals("MIXED", merged.mode());
    }

    @Test
    void mergeSnapshotAndPurgeSumsBytesAndFailsIfAnyGatewayFails() {
        // Arrange
        var ok = GatewayResult.ok("g1", new AggregatedPurgeResponse(true, 100L, "ok",
                List.of(new AggregatedPurgeResponse.NodePurgeResult("node-a", true, 100L, "done"))));
        var failed = GatewayResult.<AggregatedPurgeResponse>failure("http://g2:7070", "refused");

        // Act
        var merged = Aggregators.mergeSnapshotAndPurge(List.of(ok, failed));

        // Assert - reclaimed summed, overall failure, failed gateway surfaced as a failed node.
        assertFalse(merged.success());
        assertEquals(100L, merged.reclaimedBytes());
        assertEquals(2, merged.nodes().size());
        assertFalse(merged.nodes().get(1).success());
        assertEquals("http://g2:7070", merged.nodes().get(1).host());
    }

    @Test
    void representativeClusterToolsSucceedsOnlyWhenAllSucceed() {
        // Arrange
        var ok1 = GatewayResult.ok("g1", new ClusterToolsResponse("snapshot", "node0/cluster", 0));
        var ok2 = GatewayResult.ok("g2", new ClusterToolsResponse("snapshot", "node0/cluster", 0));
        var bad = GatewayResult.ok("g3", new ClusterToolsResponse("snapshot", "node0/cluster", 1));

        // Act
        var allOk = Aggregators.representativeClusterTools(List.of(ok1, ok2), "snapshot", null);
        var withBad = Aggregators.representativeClusterTools(List.of(ok1, bad), "snapshot", null);

        // Assert
        assertEquals(0, allOk.exitCode());
        assertEquals("snapshot", allOk.tool());
        assertEquals("node0/cluster", allOk.clusterFolder());
        assertEquals(-1, withBad.exitCode());
    }

    @Test
    void representativeSnapshotInfoPrefersPresentThenRespondedThenEmpty() {
        // Arrange
        var noSnap = GatewayResult.ok("g1", new SnapshotInfoResponse(false, -1, -1, -1, -1, 0, 0, null));
        var present = GatewayResult.ok("g2", new SnapshotInfoResponse(true, 10, 5, 1L, 42, 3, 2048, null));
        var failed = GatewayResult.<SnapshotInfoResponse>failure("g3", "down");

        // Act
        var pickPresent = Aggregators.representativeSnapshotInfo(List.of(noSnap, present));
        var pickResponded = Aggregators.representativeSnapshotInfo(List.of(noSnap, failed));
        var pickEmpty = Aggregators.representativeSnapshotInfo(List.of(failed));

        // Assert
        assertTrue(pickPresent.snapshotPresent());
        assertEquals(42, pickPresent.recordingId());
        assertFalse(pickResponded.snapshotPresent());
        assertFalse(pickEmpty.snapshotPresent());
        assertEquals(-1, pickEmpty.recordingId());
    }
}
