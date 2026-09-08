package com.bhf.aeroncache.integration.gateway;

import com.bhf.aeroncache.application.ClusterLauncher;
import com.bhf.aeroncache.gateway.application.GatewayApplication;
import com.bhf.aeroncache.gateway.client.GatewayClient;
import com.bhf.aeroncache.gateway.client.GatewayClientListener;
import com.bhf.aeroncache.gateway.client.GatewayStat;
import com.bhf.aeroncache.gateway.messages.OperationStatus;
import com.bhf.aeroncache.gateway.messages.UpdateEventType;
import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.CloseHelper;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full-stack embedded end-to-end test for the Aeron gateway.
 * <p>
 * Mirrors the in-process wiring of {@code cache-monolith}'s {@code Main.java}: an embedded media
 * driver, a real in-process RAFT cluster ({@link ClusterLauncher#launchTestCluster}), and the real
 * {@link GatewayApplication} server edge, driven by a real {@link GatewayClient}. This exercises the
 * production path end-to-end: client request encoding, the gateway ingress decode, the cluster
 * round-trip, and the batch/end-of-batch streaming responses, over real Aeron response channels.
 * <p>
 * Endpoints and cluster addresses are supplied via {@code src/test/resources/test.env} (loaded into
 * the test JVM environment by the build). The gateway binds {@code localhost:7075} (request) and
 * {@code localhost:7076} (response control) so the in-process client can reach it deterministically.
 */
@DisplayName("Gateway embedded end-to-end")
class GatewayEndToEndTest {

    private static final String REQUEST_ENDPOINT = "localhost:7075";
    private static final String RESPONSE_CONTROL_ENDPOINT = "localhost:7076";
    private static final int REQUEST_STREAM_ID = 100;
    private static final int RESPONSE_STREAM_ID = 101;

    private static final int CLUSTER_NODES = 3;
    private static final long TTL_NONE = 0L;
    private static final long TTL_SCHEDULED = 60_000L;

    private static MediaDriver clientMediaDriver;
    private static Aeron clientAeron;
    private static GatewayClient client;
    private static AgentRunner clientRunner;
    private static RecordingListener listener;

    @BeforeAll
    static void startBackendAndClient() throws Exception {
        // Start the in-process RAFT cluster (each node launches its own media driver).
        ClusterLauncher.launchTestCluster(CLUSTER_NODES, "gateway_e2e");

        // Start the real gateway: it launches its own embedded media driver and connects to the
        // cluster, standing up the ingress on the endpoints configured in test.env.
        GatewayApplication.start(0);

        // The client uses its own dedicated media driver to avoid colliding with the gateway's.
        var clientAeronDir = Files.createTempDirectory("gw-client-aeron").toString();
        clientMediaDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()
                .aeronDirectoryName(clientAeronDir)
                .threadingMode(ThreadingMode.SHARED)
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true));
        clientAeron = Aeron.connect(new Aeron.Context()
                .aeronDirectoryName(clientMediaDriver.aeronDirectoryName()));

        listener = new RecordingListener();
        client = new GatewayClient(clientAeron, REQUEST_ENDPOINT, REQUEST_STREAM_ID,
                RESPONSE_CONTROL_ENDPOINT, RESPONSE_STREAM_ID, listener);
        clientRunner = new AgentRunner(new SleepingMillisIdleStrategy(1),
                Throwable::printStackTrace, null, client);
        AgentRunner.startOnThread(clientRunner);

        // The gateway creates a client's response publication lazily, on receipt of that client's first
        // request frame. So the response subscription (and hence isConnected()) cannot come up until we
        // send something. Repeatedly send a harmless probe until both request and response channels connect.
        await().atMost(120, SECONDS).pollInterval(250, MILLISECONDS).until(() -> {
            client.getStats("connection-warmup");
            return client.isConnected();
        });
    }

    @AfterAll
    static void stopClient() {
        CloseHelper.quietClose(clientRunner);
        CloseHelper.quietClose(clientAeron);
        CloseHelper.quietClose(clientMediaDriver);
        // The in-process cluster and gateway are torn down when the test JVM exits.
    }

    @Test
    @DisplayName("Should create a cache, add entries and stream them back with end-of-batch")
    void shouldCreateCacheAddEntriesAndStreamThemBack() {
        // Arrange
        var cacheId = uniqueCache("entries");
        var createCorr = correlationId();
        var addCorr1 = correlationId();
        var addCorr2 = correlationId();
        var getCorr = correlationId();

        // Act
        client.createCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);

        client.addEntry(addCorr1, cacheId, "k1", "v1", TTL_NONE);
        awaitCommandSuccess(addCorr1);
        client.addEntry(addCorr2, cacheId, "k2", "v2", TTL_NONE);
        awaitCommandSuccess(addCorr2);

        client.getEntries(getCorr, cacheId);
        await().atMost(30, SECONDS).until(() -> listener.entriesComplete.containsKey(getCorr));

        // Assert
        assertEquals(OperationStatus.SUCCESS, listener.entriesStatus.get(getCorr));
        assertEquals(Map.of("k1", "v1", "k2", "v2"), listener.entriesAccumulated.get(getCorr));
    }

    @Test
    @DisplayName("Should return cache stats streamed back with end-of-batch")
    void shouldReturnCacheStats() {
        // Arrange
        var cacheId = uniqueCache("stats");
        var createCorr = correlationId();
        var addCorr = correlationId();
        var statsCorr = correlationId();

        client.createCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        client.addEntry(addCorr, cacheId, "sk", "sv", TTL_NONE);
        awaitCommandSuccess(addCorr);

        // Act
        client.getStats(statsCorr);
        await().atMost(30, SECONDS).until(() -> listener.statsComplete.containsKey(statsCorr));

        // Assert
        var stats = listener.statsAccumulated.get(statsCorr);
        assertTrue(stats.stream().anyMatch(s -> s.cacheId().equals(cacheId)),
                "expected stats to include the created cache " + cacheId);
    }

    @Test
    @DisplayName("Should push a streaming update to a subscribed client when an entry is added")
    void shouldPushStreamingUpdateOnSubscribe() {
        // Arrange
        var cacheId = uniqueCache("sub");
        var createCorr = correlationId();
        var subCorr = correlationId();
        var addCorr = correlationId();

        client.createCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);

        client.subscribe(subCorr, List.of(cacheId), false, false);

        // Act
        client.addEntry(addCorr, cacheId, "streamKey", "streamValue", TTL_NONE);
        awaitCommandSuccess(addCorr);

        // Assert
        await().atMost(30, SECONDS).until(() -> listener.streamUpdates.stream()
                .anyMatch(u -> u.eventType() == UpdateEventType.ADD_ITEM
                        && u.cacheId().equals(cacheId)
                        && u.key().equals("streamKey")));
    }

    @Test
    @DisplayName("Should patch an entry, deep-merging the value and streaming the merged value to subscribers")
    void shouldPatchEntryAndReceiveStreamingUpdate() {
        // Arrange
        var cacheId = uniqueCache("patch");
        var createCorr = correlationId();
        var addCorr = correlationId();
        var subCorr = correlationId();
        var patchCorr = correlationId();
        var getCorr = correlationId();

        client.createCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);

        client.addEntry(addCorr, cacheId, "patchKey", "{\"a\":1,\"b\":2}", TTL_NONE);
        awaitCommandSuccess(addCorr);

        client.subscribe(subCorr, List.of(cacheId), false, false);

        // Act
        client.patchEntry(patchCorr, cacheId, "patchKey", "{\"b\":3,\"c\":4}");
        awaitCommandSuccess(patchCorr);

        // Assert: a subscribed client receives the merged value as an ADD_ITEM stream update.
        await().atMost(30, SECONDS).until(() -> listener.streamUpdates.stream()
                .anyMatch(u -> u.eventType() == UpdateEventType.ADD_ITEM
                        && u.cacheId().equals(cacheId)
                        && u.key().equals("patchKey")
                        && u.value().equals("{\"a\":1,\"b\":3,\"c\":4}")));

        // Assert: the stored value was merged, not replaced.
        client.getEntries(getCorr, cacheId);
        await().atMost(30, SECONDS).until(() -> listener.entriesComplete.containsKey(getCorr));
        assertEquals(OperationStatus.SUCCESS, listener.entriesStatus.get(getCorr));
        assertEquals(Map.of("patchKey", "{\"a\":1,\"b\":3,\"c\":4}"), listener.entriesAccumulated.get(getCorr));
    }

    @Test
    @DisplayName("Should create a counter cache, add a counter entry and read it back")
    void shouldCreateCounterCacheAddAndReadEntry() {
        // Arrange
        var cacheId = uniqueCache("counter");
        var createCorr = correlationId();
        var addCorr = correlationId();
        var getCorr = correlationId();

        client.createCounterCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        client.addCounterEntry(addCorr, cacheId, "hits", 5L, TTL_NONE);
        awaitCommandSuccess(addCorr);

        // Act
        client.getCounterEntry(getCorr, cacheId, "hits");
        await().atMost(30, SECONDS).until(() -> listener.commandResponses.containsKey(getCorr));

        // Assert
        var response = listener.commandResponses.get(getCorr);
        assertEquals(OperationStatus.SUCCESS, response.status());
        assertEquals("5", response.value());
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

    // ------------------------------------------------------------------ helpers

    private static Optional<GatewayStat> statFor(String statsCorr, String cacheId) {
        return listener.statsAccumulated.getOrDefault(statsCorr, List.of()).stream()
                .filter(s -> s.cacheId().equals(cacheId))
                .findFirst();
    }

    private static void awaitCommandSuccess(String correlationId) {
        await().atMost(30, SECONDS).until(() -> listener.commandResponses.containsKey(correlationId));
        assertEquals(OperationStatus.SUCCESS, listener.commandResponses.get(correlationId).status(),
                "command " + correlationId + " did not succeed");
    }

    private static String uniqueCache(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private static String correlationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Records callbacks from the {@link GatewayClient} for assertion. Entries and stats are
     * accumulated per correlation id across batches until an end-of-batch frame is seen.
     */
    private static final class RecordingListener implements GatewayClientListener {

        record CommandResponse(OperationStatus status, String cacheId, String key, String value) {
        }

        record StreamUpdate(UpdateEventType eventType, String cacheId, String key, String value) {
        }

        final Map<String, CommandResponse> commandResponses = new ConcurrentHashMap<>();
        final Map<String, Map<String, String>> entriesAccumulated = new ConcurrentHashMap<>();
        final Map<String, OperationStatus> entriesStatus = new ConcurrentHashMap<>();
        final Map<String, Boolean> entriesComplete = new ConcurrentHashMap<>();
        final Map<String, List<GatewayStat>> statsAccumulated = new ConcurrentHashMap<>();
        final Map<String, Boolean> statsComplete = new ConcurrentHashMap<>();
        final Queue<StreamUpdate> streamUpdates = new ConcurrentLinkedQueue<>();
        final Map<String, String> errors = new ConcurrentHashMap<>();

        @Override
        public void onCommandResponse(String correlationId, OperationStatus status, String cacheId, String key, String value) {
            commandResponses.put(correlationId, new CommandResponse(status, cacheId, key, value));
        }

        @Override
        public void onEntries(String correlationId, OperationStatus status, String cacheId, Map<String, String> items, boolean endOfBatch) {
            entriesAccumulated.computeIfAbsent(correlationId, k -> new ConcurrentHashMap<>()).putAll(items);
            entriesStatus.put(correlationId, status);
            if (endOfBatch) {
                entriesComplete.put(correlationId, Boolean.TRUE);
            }
        }

        @Override
        public void onStats(String correlationId, OperationStatus status, List<GatewayStat> stats, boolean endOfBatch) {
            statsAccumulated.computeIfAbsent(correlationId, k -> new CopyOnWriteArrayList<>()).addAll(stats);
            if (endOfBatch) {
                statsComplete.put(correlationId, Boolean.TRUE);
            }
        }

        @Override
        public void onStreamUpdate(String correlationId, UpdateEventType eventType, String cacheId, String key, String value) {
            streamUpdates.add(new StreamUpdate(eventType, cacheId, key, value));
        }

        @Override
        public void onError(String correlationId, OperationStatus status, String message) {
            errors.put(correlationId, message);
        }
    }
}
