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

    @Test
    @DisplayName("Should read a single entry back by key, and report UNKNOWN_KEY for a missing key")
    void shouldGetSingleEntryByKey() {
        // Arrange
        var cacheId = uniqueCache("get-entry");
        var createCorr = correlationId();
        var addCorr = correlationId();
        var getCorr = correlationId();
        var missingCorr = correlationId();

        client.createCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        client.addEntry(addCorr, cacheId, "k1", "v1", TTL_NONE);
        awaitCommandSuccess(addCorr);

        // Act: read the entry that exists...
        client.getEntry(getCorr, cacheId, "k1");
        await().atMost(30, SECONDS).until(() -> listener.commandResponses.containsKey(getCorr));

        // ...and one that does not.
        client.getEntry(missingCorr, cacheId, "missing");
        await().atMost(30, SECONDS).until(() -> listener.commandResponses.containsKey(missingCorr));

        // Assert
        var found = listener.commandResponses.get(getCorr);
        assertEquals(OperationStatus.SUCCESS, found.status());
        assertEquals("k1", found.key());
        assertEquals("v1", found.value());

        assertEquals(OperationStatus.UNKNOWN_KEY, listener.commandResponses.get(missingCorr).status());
    }

    @Test
    @DisplayName("Should remove a single entry, stream a REMOVE_ITEM update and leave the others intact")
    void shouldRemoveEntryAndStreamRemoveItem() {
        // Arrange
        var cacheId = uniqueCache("remove");
        var createCorr = correlationId();
        var addCorr1 = correlationId();
        var addCorr2 = correlationId();
        var subCorr = correlationId();
        var removeCorr = correlationId();

        client.createCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        client.addEntry(addCorr1, cacheId, "k1", "v1", TTL_NONE);
        awaitCommandSuccess(addCorr1);
        client.addEntry(addCorr2, cacheId, "k2", "v2", TTL_NONE);
        awaitCommandSuccess(addCorr2);

        client.subscribe(subCorr, List.of(cacheId), false, false);
        awaitSubscribeAck(subCorr);

        // Act
        client.removeEntry(removeCorr, cacheId, "k1");
        awaitCommandSuccess(removeCorr);

        // Assert: subscribers see a REMOVE_ITEM update...
        await().atMost(30, SECONDS).until(() -> streamed(UpdateEventType.REMOVE_ITEM, cacheId, "k1"));
        // ...and only the removed entry is gone; the sibling survives.
        assertEquals(Map.of("k2", "v2"), fetchEntries(cacheId));
    }

    @Test
    @DisplayName("Should clear a cache, stream a CLEAR_CACHE update and empty the cache")
    void shouldClearCacheAndStreamClearCacheEvent() {
        // Arrange
        var cacheId = uniqueCache("clear");
        var createCorr = correlationId();
        var addCorr = correlationId();
        var subCorr = correlationId();
        var clearCorr = correlationId();

        client.createCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        client.addEntry(addCorr, cacheId, "k1", "v1", TTL_NONE);
        awaitCommandSuccess(addCorr);

        client.subscribe(subCorr, List.of(cacheId), false, false);
        awaitSubscribeAck(subCorr);

        // Act
        client.clearCache(clearCorr, cacheId);
        awaitCommandSuccess(clearCorr);

        // Assert: subscribers see a CLEAR_CACHE update, and the cache is emptied...
        await().atMost(30, SECONDS).until(() -> streamedCacheEvent(UpdateEventType.CLEAR_CACHE, cacheId));
        assertEquals(Map.of(), fetchEntries(cacheId));

        // ...but the cache itself still exists, now with zero entries.
        var statsCorr = correlationId();
        client.getStats(statsCorr);
        await().atMost(30, SECONDS).until(() -> listener.statsComplete.containsKey(statsCorr));
        var stat = statFor(statsCorr, cacheId);
        assertTrue(stat.isPresent(), "expected stats to include the cleared cache " + cacheId);
        assertEquals(0L, stat.get().size(), "cleared cache should have zero entries");
    }

    @Test
    @DisplayName("Should delete a cache, stream a DELETE_CACHE update and drop it from cache stats")
    void shouldDeleteCacheAndStreamDeleteCacheEvent() {
        // Arrange
        var cacheId = uniqueCache("delete");
        var createCorr = correlationId();
        var addCorr = correlationId();
        var subCorr = correlationId();
        var deleteCorr = correlationId();
        var statsCorr = correlationId();

        client.createCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        client.addEntry(addCorr, cacheId, "k1", "v1", TTL_NONE);
        awaitCommandSuccess(addCorr);

        client.subscribe(subCorr, List.of(cacheId), false, false);
        awaitSubscribeAck(subCorr);

        // Act
        client.deleteCache(deleteCorr, cacheId);
        awaitCommandSuccess(deleteCorr);

        // Assert: subscribers see a DELETE_CACHE update...
        await().atMost(30, SECONDS).until(() -> streamedCacheEvent(UpdateEventType.DELETE_CACHE, cacheId));
        // ...and the deleted cache is absent from stats.
        client.getStats(statsCorr);
        await().atMost(30, SECONDS).until(() -> listener.statsComplete.containsKey(statsCorr));
        assertTrue(statFor(statsCorr, cacheId).isEmpty(),
                "expected deleted cache " + cacheId + " to be absent from stats");
    }

    @Test
    @DisplayName("Should acknowledge a subscription with the caches it now covers")
    void shouldAcknowledgeSubscription() {
        // Arrange
        var cacheId = uniqueCache("sub-ack");
        var createCorr = correlationId();
        var subCorr = correlationId();

        client.createCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);

        // Act
        client.subscribe(subCorr, List.of(cacheId), false, false);
        awaitSubscribeAck(subCorr);

        // Assert
        assertEquals(List.of(cacheId), listener.subscribeAcks.get(subCorr).cacheIds());
    }

    @Test
    @DisplayName("Should stop delivering streaming updates after an unsubscribe")
    void shouldStopStreamingAfterUnsubscribe() {
        // Arrange
        var cacheId = uniqueCache("unsub");
        var createCorr = correlationId();
        var subCorr = correlationId();
        var addLiveCorr = correlationId();
        var unsubCorr = correlationId();
        var addAfterCorr = correlationId();

        client.createCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);

        client.subscribe(subCorr, List.of(cacheId), false, false);
        awaitSubscribeAck(subCorr);

        // Confirm the subscription is genuinely live before unsubscribing.
        client.addEntry(addLiveCorr, cacheId, "live", "1", TTL_NONE);
        awaitCommandSuccess(addLiveCorr);
        await().atMost(30, SECONDS).until(() -> streamed(UpdateEventType.ADD_ITEM, cacheId, "live"));

        // Act: unsubscribe, then mutate again. The unsubscribe frame is processed by the gateway ingress
        // (which removes the session's subscription locally) before the following add is even forwarded to
        // the backend, so the add's update can never be dispatched to this now-unsubscribed session.
        client.unsubscribe(unsubCorr, cacheId, false);
        client.addEntry(addAfterCorr, cacheId, "afterUnsub", "2", TTL_NONE);
        awaitCommandSuccess(addAfterCorr);

        // Assert: the mutation applied (it is present in the cache)...
        assertTrue(fetchEntries(cacheId).containsKey("afterUnsub"));
        // ...but no streaming update for it ever reached this client.
        assertTrue(listener.streamUpdates.stream()
                        .noneMatch(u -> u.cacheId().equals(cacheId) && "afterUnsub".equals(u.key())),
                "expected no stream update after unsubscribe");
    }

    @Test
    @DisplayName("Should schedule a TTL-based removal and then cancel it, keeping the entry")
    void shouldScheduleTtlRemovalThenCancelIt() {
        // Arrange
        var cacheId = uniqueCache("ttl-cancel");
        var createCorr = correlationId();
        var addCorr = correlationId();
        var cancelCorr = correlationId();

        client.createCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);

        // Add with a (long) scheduled removal so the timer is pending when we cancel it.
        client.addEntry(addCorr, cacheId, "k1", "v1", TTL_SCHEDULED);
        awaitCommandSuccess(addCorr);

        // Act: cancel the pending removal.
        client.cancelItemRemoval(cancelCorr, cacheId, "k1");
        await().atMost(30, SECONDS).until(() -> listener.commandResponses.containsKey(cancelCorr));

        // Assert: the cancel succeeded (a removal was pending) and the entry survives.
        assertEquals(OperationStatus.SUCCESS, listener.commandResponses.get(cancelCorr).status());
        assertEquals(Map.of("k1", "v1"), fetchEntries(cacheId));
    }

    @Test
    @DisplayName("Should report UNKNOWN_KEY when cancelling a removal that was never scheduled")
    void shouldReportUnknownKeyWhenNoRemovalScheduled() {
        // Arrange
        var cacheId = uniqueCache("cancel-none");
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
    @DisplayName("Should schedule a TTL-based counter removal and then cancel it, keeping the counter")
    void shouldScheduleCounterTtlRemovalThenCancelIt() {
        // Arrange
        var cacheId = uniqueCache("counter-ttl-cancel");
        var createCorr = correlationId();
        var addCorr = correlationId();
        var cancelCorr = correlationId();
        var getCorr = correlationId();

        client.createCounterCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        // Add with a (long) scheduled removal so the timer is pending when we cancel it.
        client.addCounterEntry(addCorr, cacheId, "hits", 5L, TTL_SCHEDULED);
        awaitCommandSuccess(addCorr);

        // Act
        client.cancelCounterItemRemoval(cancelCorr, cacheId, "hits");
        awaitCommandSuccess(cancelCorr);

        // Assert: the counter survives because its scheduled removal was cancelled.
        client.getCounterEntry(getCorr, cacheId, "hits");
        await().atMost(30, SECONDS).until(() -> listener.commandResponses.containsKey(getCorr));
        assertEquals("5", listener.commandResponses.get(getCorr).value());
    }

    @Test
    @DisplayName("Should increment a counter, return the new value and stream it to subscribers")
    void shouldIncrementCounterAndStreamUpdate() {
        // Arrange
        var cacheId = uniqueCache("counter-inc");
        var createCorr = correlationId();
        var addCorr = correlationId();
        var subCorr = correlationId();
        var incCorr = correlationId();

        client.createCounterCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        client.addCounterEntry(addCorr, cacheId, "hits", 0L, TTL_NONE);
        awaitCommandSuccess(addCorr);

        client.subscribe(subCorr, List.of(cacheId), false, true);
        awaitSubscribeAck(subCorr);

        // Act
        client.incrementCounter(incCorr, cacheId, "hits", 5L, TTL_NONE);
        await().atMost(30, SECONDS).until(() -> listener.commandResponses.containsKey(incCorr));

        // Assert: the command reports the accumulated value...
        var response = listener.commandResponses.get(incCorr);
        assertEquals(OperationStatus.SUCCESS, response.status());
        assertEquals("5", response.value());
        // ...and the new value is streamed to counter subscribers as an ADD_ITEM update.
        await().atMost(30, SECONDS).until(() -> listener.streamUpdates.stream()
                .anyMatch(u -> u.eventType() == UpdateEventType.ADD_ITEM
                        && u.cacheId().equals(cacheId)
                        && u.key().equals("hits")
                        && u.value().equals("5")));
    }

    @Test
    @DisplayName("Should decrement a counter and return the new value")
    void shouldDecrementCounter() {
        // Arrange
        var cacheId = uniqueCache("counter-dec");
        var createCorr = correlationId();
        var addCorr = correlationId();
        var decCorr = correlationId();

        client.createCounterCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        client.addCounterEntry(addCorr, cacheId, "gauge", 10L, TTL_NONE);
        awaitCommandSuccess(addCorr);

        // Act
        client.decrementCounter(decCorr, cacheId, "gauge", 4L, TTL_NONE);
        await().atMost(30, SECONDS).until(() -> listener.commandResponses.containsKey(decCorr));

        // Assert
        var response = listener.commandResponses.get(decCorr);
        assertEquals(OperationStatus.SUCCESS, response.status());
        assertEquals("6", response.value());
    }

    @Test
    @DisplayName("Should set a counter to an absolute value and return it")
    void shouldSetCounter() {
        // Arrange
        var cacheId = uniqueCache("counter-set");
        var createCorr = correlationId();
        var addCorr = correlationId();
        var setCorr = correlationId();

        client.createCounterCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        client.addCounterEntry(addCorr, cacheId, "gauge", 1L, TTL_NONE);
        awaitCommandSuccess(addCorr);

        // Act
        client.setCounter(setCorr, cacheId, "gauge", 42L, TTL_NONE);
        await().atMost(30, SECONDS).until(() -> listener.commandResponses.containsKey(setCorr));

        // Assert
        var response = listener.commandResponses.get(setCorr);
        assertEquals(OperationStatus.SUCCESS, response.status());
        assertEquals("42", response.value());
    }

    @Test
    @DisplayName("Should stream all counter entries and report counter cache stats")
    void shouldStreamCounterEntriesAndStats() {
        // Arrange
        var cacheId = uniqueCache("counter-entries");
        var createCorr = correlationId();
        var addCorr1 = correlationId();
        var addCorr2 = correlationId();
        var entriesCorr = correlationId();
        var statsCorr = correlationId();

        client.createCounterCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        client.addCounterEntry(addCorr1, cacheId, "a", 1L, TTL_NONE);
        awaitCommandSuccess(addCorr1);
        client.addCounterEntry(addCorr2, cacheId, "b", 2L, TTL_NONE);
        awaitCommandSuccess(addCorr2);

        // Act: stream the counter entries...
        client.getCounterEntries(entriesCorr, cacheId);
        await().atMost(30, SECONDS).until(() -> listener.entriesComplete.containsKey(entriesCorr));

        // ...and read the counter cache stats.
        client.getCounterStats(statsCorr);
        await().atMost(30, SECONDS).until(() -> listener.statsComplete.containsKey(statsCorr));

        // Assert
        assertEquals(OperationStatus.SUCCESS, listener.entriesStatus.get(entriesCorr));
        assertEquals(Map.of("a", "1", "b", "2"), listener.entriesAccumulated.get(entriesCorr));
        assertTrue(statFor(statsCorr, cacheId).isPresent(),
                "expected counter stats to include the created counter cache " + cacheId);
    }

    @Test
    @DisplayName("Should remove a single counter entry, leaving the others intact")
    void shouldRemoveCounterEntry() {
        // Arrange
        var cacheId = uniqueCache("counter-remove");
        var createCorr = correlationId();
        var addCorr1 = correlationId();
        var addCorr2 = correlationId();
        var removeCorr = correlationId();
        var getCorr = correlationId();
        var entriesCorr = correlationId();

        client.createCounterCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        client.addCounterEntry(addCorr1, cacheId, "hits", 7L, TTL_NONE);
        awaitCommandSuccess(addCorr1);
        client.addCounterEntry(addCorr2, cacheId, "misses", 9L, TTL_NONE);
        awaitCommandSuccess(addCorr2);

        // Act
        client.removeCounterEntry(removeCorr, cacheId, "hits");
        awaitCommandSuccess(removeCorr);

        // Assert: the removed counter reads back as UNKNOWN_KEY...
        client.getCounterEntry(getCorr, cacheId, "hits");
        await().atMost(30, SECONDS).until(() -> listener.commandResponses.containsKey(getCorr));
        assertEquals(OperationStatus.UNKNOWN_KEY, listener.commandResponses.get(getCorr).status());

        // ...while the sibling counter survives.
        client.getCounterEntries(entriesCorr, cacheId);
        await().atMost(30, SECONDS).until(() -> listener.entriesComplete.containsKey(entriesCorr));
        assertEquals(Map.of("misses", "9"), listener.entriesAccumulated.get(entriesCorr));
    }

    @Test
    @DisplayName("Should clear and then delete a counter cache")
    void shouldClearAndDeleteCounterCache() {
        // Arrange
        var cacheId = uniqueCache("counter-clear-delete");
        var createCorr = correlationId();
        var addCorr = correlationId();
        var clearCorr = correlationId();
        var entriesCorr = correlationId();
        var deleteCorr = correlationId();

        client.createCounterCache(createCorr, cacheId);
        awaitCommandSuccess(createCorr);
        client.addCounterEntry(addCorr, cacheId, "hits", 3L, TTL_NONE);
        awaitCommandSuccess(addCorr);

        // Act: clear empties the counter cache...
        client.clearCounterCache(clearCorr, cacheId);
        awaitCommandSuccess(clearCorr);

        client.getCounterEntries(entriesCorr, cacheId);
        await().atMost(30, SECONDS).until(() -> listener.entriesComplete.containsKey(entriesCorr));

        // ...and delete removes the cache itself.
        client.deleteCounterCache(deleteCorr, cacheId);
        awaitCommandSuccess(deleteCorr);

        // Assert: the clear emptied the cache...
        assertEquals(Map.of(), listener.entriesAccumulated.getOrDefault(entriesCorr, Map.of()));

        // ...and the deleted counter cache is absent from counter stats.
        var statsCorr = correlationId();
        client.getCounterStats(statsCorr);
        await().atMost(30, SECONDS).until(() -> listener.statsComplete.containsKey(statsCorr));
        assertTrue(statFor(statsCorr, cacheId).isEmpty(),
                "expected deleted counter cache " + cacheId + " to be absent from counter stats");
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

    /** Blocks until the subscription for the given correlation id is confirmed live by a subscribe ack. */
    protected void awaitSubscribeAck(String correlationId) {
        await().atMost(30, SECONDS).until(() -> listener.subscribeAcks.containsKey(correlationId));
        assertEquals(OperationStatus.SUCCESS, listener.subscribeAcks.get(correlationId).status(),
                "subscription " + correlationId + " was not acknowledged");
    }

    /** True once a stream update of the given type carrying the key for the cache has arrived. */
    protected boolean streamed(UpdateEventType eventType, String cacheId, String key) {
        return listener.streamUpdates.stream()
                .anyMatch(u -> u.eventType() == eventType
                        && u.cacheId().equals(cacheId)
                        && key.equals(u.key()));
    }

    /** True once a cache-wide stream update of the given type (no key) has arrived for the cache. */
    protected boolean streamedCacheEvent(UpdateEventType eventType, String cacheId) {
        return listener.streamUpdates.stream()
                .anyMatch(u -> u.eventType() == eventType && u.cacheId().equals(cacheId));
    }

    /** Fetches the current entries for a cache and blocks until the end-of-batch frame lands. */
    protected Map<String, String> fetchEntries(String cacheId) {
        var getCorr = correlationId();
        client.getEntries(getCorr, cacheId);
        await().atMost(30, SECONDS).until(() -> listener.entriesComplete.containsKey(getCorr));
        assertEquals(OperationStatus.SUCCESS, listener.entriesStatus.get(getCorr));
        return listener.entriesAccumulated.getOrDefault(getCorr, Map.of());
    }

    protected static String uniqueCache(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    protected static String correlationId() {
        return UUID.randomUUID().toString();
    }
}
