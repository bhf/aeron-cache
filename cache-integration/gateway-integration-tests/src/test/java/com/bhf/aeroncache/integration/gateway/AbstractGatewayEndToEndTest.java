package com.bhf.aeroncache.integration.gateway;

import com.bhf.aeroncache.gateway.client.GatewayClient;
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
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shared full-stack embedded end-to-end test harness for the Aeron gateway.
 * <p>
 * Concrete subclasses supply the backend the gateway runs against by implementing
 * {@link #startBackend()} (a real in-process RAFT cluster, or a single in-process ephemeral cache).
 * Everything downstream of that — the dedicated client media driver, the {@link GatewayClient}, the
 * {@link RecordingListener} used for assertions, the connection warm-up, and the shared suite of
 * end-to-end {@link Test}s — lives here so both deployments exercise the identical production path:
 * client request encoding, gateway ingress decode, backend round-trip, and the batch/end-of-batch
 * streaming responses over real Aeron response channels.
 * <p>
 * The gateway binds {@code localhost:7075} (request) and {@code localhost:7076} (response control)
 * as configured by each module's {@code src/test/resources/test.env}, so the in-process client can
 * reach it deterministically.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractGatewayEndToEndTest {

    protected static final String REQUEST_ENDPOINT = "localhost:7075";
    protected static final String RESPONSE_CONTROL_ENDPOINT = "localhost:7076";
    protected static final int REQUEST_STREAM_ID = 100;
    protected static final int RESPONSE_STREAM_ID = 101;

    protected static final long TTL_NONE = 0L;
    protected static final long TTL_SCHEDULED = 60_000L;

    private MediaDriver clientMediaDriver;
    private Aeron clientAeron;
    private AgentRunner clientRunner;

    protected GatewayClient client;
    protected RecordingListener listener;

    /**
     * Starts the backend the gateway will front (cluster or ephemeral cache) and the
     * {@link com.bhf.aeroncache.gateway.application.GatewayApplication} itself. Called once, before
     * the shared client harness is stood up.
     */
    protected abstract void startBackend() throws Exception;

    @BeforeAll
    void startBackendAndClient() throws Exception {
        startBackend();

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
    void stopClient() {
        CloseHelper.quietClose(clientRunner);
        CloseHelper.quietClose(clientAeron);
        CloseHelper.quietClose(clientMediaDriver);
        // The in-process backend and gateway are torn down when the (forked) test JVM exits.
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
    @DisplayName("Should hydrate a new subscriber with a snapshot of the cache's existing entries")
    void shouldHydrateSubscriberWithExistingEntriesOnSubscribe() {
        // Arrange: populate the cache *before* anyone subscribes.
        var cacheId = uniqueCache("hydrate");
        var createCorr = correlationId();
        var addCorr1 = correlationId();
        var addCorr2 = correlationId();
        var subCorr = correlationId();

        client.createCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        client.addEntry(addCorr1, cacheId, "k1", "v1", TTL_NONE);
        awaitCommandSuccess(addCorr1);
        client.addEntry(addCorr2, cacheId, "k2", "v2", TTL_NONE);
        awaitCommandSuccess(addCorr2);

        // Act: subscribe with sendSnapshot=true. The pre-existing entries are replayed to this
        // subscriber as individual ADD_ITEM stream updates (initial hydration).
        client.subscribe(subCorr, List.of(cacheId), true, false);

        // Assert: both existing entries are hydrated to the subscriber.
        await().atMost(30, SECONDS).until(() -> hydrated(cacheId, "k1", "v1"));
        await().atMost(30, SECONDS).until(() -> hydrated(cacheId, "k2", "v2"));
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
    @DisplayName("Should receive a PATCH_ITEM stream update when subscribed in patch mode")
    void shouldReceivePatchItemEventWhenSubscribedInPatchMode() {
        // Arrange
        var cacheId = uniqueCache("patch-sub");
        var createCorr = correlationId();
        var addCorr = correlationId();
        var subCorr = correlationId();
        var patchCorr = correlationId();

        client.createCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);

        client.addEntry(addCorr, cacheId, "patchKey", "{\"a\":1,\"b\":2}", TTL_NONE);
        awaitCommandSuccess(addCorr);

        // Subscribe to the whole cache in patch mode (null key = whole cache).
        client.subscribe(subCorr, List.of(cacheId), Collections.singletonList(null), true, false, false);

        // Act
        client.patchEntry(patchCorr, cacheId, "patchKey", "{\"b\":3,\"c\":4}");
        awaitCommandSuccess(patchCorr);

        // Assert: a patch-mode subscriber receives a PATCH_ITEM event carrying the patch delta.
        await().atMost(30, SECONDS).until(() -> listener.streamUpdates.stream()
                .anyMatch(u -> u.eventType() == UpdateEventType.PATCH_ITEM
                        && u.cacheId().equals(cacheId)
                        && u.key().equals("patchKey")
                        && u.value().equals("{\"b\":3,\"c\":4}")));
    }

    @Test
    @DisplayName("Should only stream updates for the specific key a client subscribed to")
    void shouldOnlyStreamUpdatesForSubscribedKey() {
        // Arrange
        var cacheId = uniqueCache("key-sub");
        var createCorr = correlationId();
        var subCorr = correlationId();
        var addOtherCorr = correlationId();
        var addSubscribedCorr = correlationId();

        client.createCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);

        // Subscribe to a single key ("k1") within the cache.
        client.subscribe(subCorr, List.of(cacheId), List.of("k1"), false, false, false);

        // Act: add the non-subscribed key first, then the subscribed key. Because the cluster
        // processes and streams these in order, seeing "k1" guarantees "k2" would already have
        // arrived if it were ever going to.
        client.addEntry(addOtherCorr, cacheId, "k2", "otherValue", TTL_NONE);
        awaitCommandSuccess(addOtherCorr);
        client.addEntry(addSubscribedCorr, cacheId, "k1", "subscribedValue", TTL_NONE);
        awaitCommandSuccess(addSubscribedCorr);

        // Assert: the subscribed key is streamed...
        await().atMost(30, SECONDS).until(() -> listener.streamUpdates.stream()
                .anyMatch(u -> u.eventType() == UpdateEventType.ADD_ITEM
                        && u.cacheId().equals(cacheId)
                        && u.key().equals("k1")));

        // ...while the non-subscribed key is never streamed to this client.
        assertTrue(listener.streamUpdates.stream()
                        .noneMatch(u -> u.cacheId().equals(cacheId) && u.key().equals("k2")),
                "expected no stream update for the unsubscribed key k2");
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

    // ------------------------------------------------------------------ helpers

    /** True once an ADD_ITEM stream update carrying the given key/value for the cache has arrived. */
    protected boolean hydrated(String cacheId, String key, String value) {
        return listener.streamUpdates.stream()
                .anyMatch(u -> u.eventType() == UpdateEventType.ADD_ITEM
                        && u.cacheId().equals(cacheId)
                        && u.key().equals(key)
                        && u.value().equals(value));
    }

    protected Optional<GatewayStat> statFor(String statsCorr, String cacheId) {
        return listener.statsAccumulated.getOrDefault(statsCorr, List.of()).stream()
                .filter(s -> s.cacheId().equals(cacheId))
                .findFirst();
    }

    protected void awaitCommandSuccess(String correlationId) {
        await().atMost(30, SECONDS).until(() -> listener.commandResponses.containsKey(correlationId));
        assertEquals(OperationStatus.SUCCESS, listener.commandResponses.get(correlationId).status(),
                "command " + correlationId + " did not succeed");
    }

    protected static String uniqueCache(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    protected static String correlationId() {
        return UUID.randomUUID().toString();
    }
}
