package com.bhf.aeroncache.integration.gateway;

import com.bhf.aeroncache.application.ClusterLauncher;
import com.bhf.aeroncache.gateway.application.GatewayApplication;
import com.bhf.aeroncache.gateway.messages.OperationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full-stack embedded end-to-end test for the Aeron gateway backed by a real in-process RAFT cluster.
 * <p>
 * Mirrors the in-process wiring of {@code cache-monolith}'s {@code Main.java}: an embedded media
 * driver, a real in-process RAFT cluster ({@link ClusterLauncher#launchTestCluster}), and the real
 * {@link GatewayApplication} server edge, driven by a real {@link com.bhf.aeroncache.gateway.client.GatewayClient}.
 * <p>
 * The shared client harness and the streaming/subscription/CRUD-read suite live in
 * {@link AbstractGatewayEndToEndTest}; this class only stands up the clustered backend and adds the
 * cluster-specific coverage (clear/delete, counter arithmetic and scheduled-removal timers).
 * <p>
 * Endpoints and cluster addresses are supplied via {@code src/test/resources/test.env} (loaded into
 * the test JVM environment by the build). The gateway binds {@code localhost:7075} (request) and
 * {@code localhost:7076} (response control) so the in-process client can reach it deterministically.
 */
@DisplayName("Gateway embedded end-to-end")
class GatewayEndToEndTest extends AbstractGatewayEndToEndTest {

    private static final int CLUSTER_NODES = 3;

    @Override
    protected void startBackend() throws Exception {
        // Start the in-process RAFT cluster (each node launches its own media driver).
        ClusterLauncher.launchTestCluster(CLUSTER_NODES, "gateway_e2e");

        // Start the real gateway: it launches its own embedded media driver and connects to the
        // cluster, standing up the ingress on the endpoints configured in test.env.
        GatewayApplication.start(0);
    }

    @Test
    @DisplayName("Should clear a cache so its entries are removed and its size drops to zero")
    void shouldClearCache() {
        // Arrange
        var cacheId = uniqueCache("clear");
        var createCorr = correlationId();
        var addCorr1 = correlationId();
        var addCorr2 = correlationId();
        var clearCorr = correlationId();
        var statsCorr = correlationId();

        client.createCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        client.addEntry(addCorr1, cacheId, "k1", "v1", TTL_NONE);
        awaitCommandSuccess(addCorr1);
        client.addEntry(addCorr2, cacheId, "k2", "v2", TTL_NONE);
        awaitCommandSuccess(addCorr2);

        // Act
        client.clearCache(clearCorr, cacheId);
        awaitCommandSuccess(clearCorr);

        // Assert: the cache still exists but is now empty.
        client.getStats(statsCorr);
        await().atMost(30, SECONDS).until(() -> listener.statsComplete.containsKey(statsCorr));
        var stat = statFor(statsCorr, cacheId);
        assertTrue(stat.isPresent(), "expected stats to include the cleared cache " + cacheId);
        assertEquals(0L, stat.get().size(), "cleared cache should have zero entries");
    }

    @Test
    @DisplayName("Should delete a single entry from a cache while leaving the others intact")
    void shouldDeleteSingleEntry() {
        // Arrange
        var cacheId = uniqueCache("delete-item");
        var createCorr = correlationId();
        var addCorr1 = correlationId();
        var addCorr2 = correlationId();
        var removeCorr = correlationId();
        var getCorr = correlationId();

        client.createCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        client.addEntry(addCorr1, cacheId, "k1", "v1", TTL_NONE);
        awaitCommandSuccess(addCorr1);
        client.addEntry(addCorr2, cacheId, "k2", "v2", TTL_NONE);
        awaitCommandSuccess(addCorr2);

        // Act
        client.removeEntry(removeCorr, cacheId, "k1");
        awaitCommandSuccess(removeCorr);

        // Assert: only the surviving entry is streamed back.
        client.getEntries(getCorr, cacheId);
        await().atMost(30, SECONDS).until(() -> listener.entriesComplete.containsKey(getCorr));
        assertEquals(OperationStatus.SUCCESS, listener.entriesStatus.get(getCorr));
        assertEquals(Map.of("k2", "v2"), listener.entriesAccumulated.get(getCorr));
    }

    @Test
    @DisplayName("Should delete a cache so it no longer appears in cache stats")
    void shouldDeleteCache() {
        // Arrange
        var cacheId = uniqueCache("delete-cache");
        var createCorr = correlationId();
        var addCorr = correlationId();
        var deleteCorr = correlationId();
        var statsCorr = correlationId();

        client.createCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        client.addEntry(addCorr, cacheId, "k1", "v1", TTL_NONE);
        awaitCommandSuccess(addCorr);

        // Act
        client.deleteCache(deleteCorr, cacheId);
        awaitCommandSuccess(deleteCorr);

        // Assert: the deleted cache is absent from stats.
        client.getStats(statsCorr);
        await().atMost(30, SECONDS).until(() -> listener.statsComplete.containsKey(statsCorr));
        assertTrue(statFor(statsCorr, cacheId).isEmpty(),
                "expected deleted cache " + cacheId + " to be absent from stats");
    }

    @Test
    @DisplayName("Should increment, decrement and set a counter entry and read back the running value")
    void shouldIncrementDecrementAndSetCounter() {
        // Arrange
        var cacheId = uniqueCache("counter-math");
        var createCorr = correlationId();
        var addCorr = correlationId();
        var incCorr = correlationId();
        var decCorr = correlationId();
        var setCorr = correlationId();
        var getCorr = correlationId();

        client.createCounterCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        client.addCounterEntry(addCorr, cacheId, "hits", 5L, TTL_NONE);
        awaitCommandSuccess(addCorr);

        // Act + Assert: increment 5 -> 8
        client.incrementCounter(incCorr, cacheId, "hits", 3L, TTL_NONE);
        await().atMost(30, SECONDS).until(() -> listener.commandResponses.containsKey(incCorr));
        assertEquals(OperationStatus.SUCCESS, listener.commandResponses.get(incCorr).status());
        assertEquals("8", listener.commandResponses.get(incCorr).value());

        // Act + Assert: decrement 8 -> 6
        client.decrementCounter(decCorr, cacheId, "hits", 2L, TTL_NONE);
        await().atMost(30, SECONDS).until(() -> listener.commandResponses.containsKey(decCorr));
        assertEquals(OperationStatus.SUCCESS, listener.commandResponses.get(decCorr).status());
        assertEquals("6", listener.commandResponses.get(decCorr).value());

        // Act + Assert: set to 10
        client.setCounter(setCorr, cacheId, "hits", 10L, TTL_NONE);
        await().atMost(30, SECONDS).until(() -> listener.commandResponses.containsKey(setCorr));
        assertEquals(OperationStatus.SUCCESS, listener.commandResponses.get(setCorr).status());
        assertEquals("10", listener.commandResponses.get(setCorr).value());

        // Act + Assert: a fresh read confirms the final value.
        client.getCounterEntry(getCorr, cacheId, "hits");
        await().atMost(30, SECONDS).until(() -> listener.commandResponses.containsKey(getCorr));
        assertEquals("10", listener.commandResponses.get(getCorr).value());
    }

    @Test
    @DisplayName("Should delete a single counter entry while leaving the others intact")
    void shouldDeleteCounterEntry() {
        // Arrange
        var cacheId = uniqueCache("delete-counter");
        var createCorr = correlationId();
        var addCorr1 = correlationId();
        var addCorr2 = correlationId();
        var removeCorr = correlationId();
        var getCorr = correlationId();

        client.createCounterCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        client.addCounterEntry(addCorr1, cacheId, "hits", 5L, TTL_NONE);
        awaitCommandSuccess(addCorr1);
        client.addCounterEntry(addCorr2, cacheId, "misses", 9L, TTL_NONE);
        awaitCommandSuccess(addCorr2);

        // Act
        client.removeCounterEntry(removeCorr, cacheId, "hits");
        awaitCommandSuccess(removeCorr);

        // Assert: only the surviving counter is streamed back.
        client.getCounterEntries(getCorr, cacheId);
        await().atMost(30, SECONDS).until(() -> listener.entriesComplete.containsKey(getCorr));
        assertEquals(OperationStatus.SUCCESS, listener.entriesStatus.get(getCorr));
        assertEquals(Map.of("misses", "9"), listener.entriesAccumulated.get(getCorr));
    }

    @Test
    @DisplayName("Should delete a counter cache so it no longer appears in counter stats")
    void shouldDeleteCounterCache() {
        // Arrange
        var cacheId = uniqueCache("delete-counter-cache");
        var createCorr = correlationId();
        var addCorr = correlationId();
        var deleteCorr = correlationId();
        var statsCorr = correlationId();

        client.createCounterCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        client.addCounterEntry(addCorr, cacheId, "hits", 5L, TTL_NONE);
        awaitCommandSuccess(addCorr);

        // Act
        client.deleteCounterCache(deleteCorr, cacheId);
        awaitCommandSuccess(deleteCorr);

        // Assert: the deleted counter cache is absent from counter stats.
        client.getCounterStats(statsCorr);
        await().atMost(30, SECONDS).until(() -> listener.statsComplete.containsKey(statsCorr));
        assertTrue(statFor(statsCorr, cacheId).isEmpty(),
                "expected deleted counter cache " + cacheId + " to be absent from stats");
    }

    @Test
    @DisplayName("Should cancel a scheduled item removal so the entry is not evicted")
    void shouldCancelScheduledItemRemoval() {
        // Arrange
        var cacheId = uniqueCache("cancel-removal");
        var createCorr = correlationId();
        var addCorr = correlationId();
        var cancelCorr = correlationId();
        var getCorr = correlationId();

        client.createCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        // Add with a ttl so a removal timer is scheduled for the key.
        client.addEntry(addCorr, cacheId, "k1", "v1", TTL_SCHEDULED);
        awaitCommandSuccess(addCorr);

        // Act
        client.cancelItemRemoval(cancelCorr, cacheId, "k1");
        awaitCommandSuccess(cancelCorr);

        // Assert: the entry is still present because its scheduled removal was cancelled.
        client.getEntries(getCorr, cacheId);
        await().atMost(30, SECONDS).until(() -> listener.entriesComplete.containsKey(getCorr));
        assertEquals(OperationStatus.SUCCESS, listener.entriesStatus.get(getCorr));
        assertEquals(Map.of("k1", "v1"), listener.entriesAccumulated.get(getCorr));
    }

    @Test
    @DisplayName("Should report UNKNOWN_KEY when cancelling a removal that was never scheduled")
    void shouldReportUnknownKeyWhenNoRemovalScheduled() {
        // Arrange
        var cacheId = uniqueCache("cancel-removal-none");
        var createCorr = correlationId();
        var addCorr = correlationId();
        var cancelCorr = correlationId();

        client.createCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        // Add without a ttl, so no removal timer is scheduled.
        client.addEntry(addCorr, cacheId, "k1", "v1", TTL_NONE);
        awaitCommandSuccess(addCorr);

        // Act
        client.cancelItemRemoval(cancelCorr, cacheId, "k1");
        await().atMost(30, SECONDS).until(() -> listener.commandResponses.containsKey(cancelCorr));

        // Assert
        assertEquals(OperationStatus.UNKNOWN_KEY, listener.commandResponses.get(cancelCorr).status(),
                "cancelling a removal that was never scheduled should report UNKNOWN_KEY");
    }

    @Test
    @DisplayName("Should cancel a scheduled counter item removal so the counter is not evicted")
    void shouldCancelScheduledCounterItemRemoval() {
        // Arrange
        var cacheId = uniqueCache("cancel-counter-removal");
        var createCorr = correlationId();
        var addCorr = correlationId();
        var cancelCorr = correlationId();
        var getCorr = correlationId();

        client.createCounterCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        // Add with a ttl so a removal timer is scheduled for the counter.
        client.addCounterEntry(addCorr, cacheId, "hits", 5L, TTL_SCHEDULED);
        awaitCommandSuccess(addCorr);

        // Act
        client.cancelCounterItemRemoval(cancelCorr, cacheId, "hits");
        awaitCommandSuccess(cancelCorr);

        // Assert: the counter is still present because its scheduled removal was cancelled.
        client.getCounterEntry(getCorr, cacheId, "hits");
        await().atMost(30, SECONDS).until(() -> listener.commandResponses.containsKey(getCorr));
        assertEquals("5", listener.commandResponses.get(getCorr).value());
    }
}
