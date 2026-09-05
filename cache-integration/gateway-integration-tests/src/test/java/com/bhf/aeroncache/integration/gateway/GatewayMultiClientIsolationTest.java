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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Multi-client end-to-end test for the Aeron gateway, focused on <em>response isolation</em>.
 * <p>
 * Several independent {@link GatewayClient}s (each on its own media driver, mirroring separate
 * client processes) connect concurrently to a single real {@link GatewayApplication} in front of a
 * real in-process RAFT cluster. All clients share the gateway's request/response endpoints; the
 * gateway distinguishes them by the request {@link io.aeron.Image} {@code correlationId()} and
 * routes each response over that session's own Aeron response channel
 * ({@code control-mode=response}).
 * <p>
 * The tests assert that a client only ever observes callbacks for the requests <em>it</em> issued
 * (command responses, batched entries) and that streaming updates reach only subscribed clients —
 * i.e. there is no cross-talk between sessions.
 */
@DisplayName("Gateway multi-client response isolation")
class GatewayMultiClientIsolationTest {

    private static final String REQUEST_ENDPOINT = "localhost:7075";
    private static final String RESPONSE_CONTROL_ENDPOINT = "localhost:7076";
    private static final int REQUEST_STREAM_ID = 100;
    private static final int RESPONSE_STREAM_ID = 101;

    private static final int CLUSTER_NODES = 3;
    private static final long TTL_NONE = 0L;

    private static final List<TestClient> clients = new ArrayList<>();
    private static TestClient clientA;
    private static TestClient clientB;
    private static TestClient clientC;

    @BeforeAll
    static void startBackendAndClients() throws Exception {
        // Real 3-node RAFT cluster and the real gateway server edge, all in this JVM.
        ClusterLauncher.launchTestCluster(CLUSTER_NODES, "gateway_multiclient");
        GatewayApplication.start(0);

        // Three fully independent clients, each with its own media driver (as separate processes would be).
        clientA = TestClient.connect("A");
        clientB = TestClient.connect("B");
        clientC = TestClient.connect("C");
        clients.add(clientA);
        clients.add(clientB);
        clients.add(clientC);
    }

    @AfterAll
    static void stopClients() {
        clients.forEach(TestClient::close);
        clients.clear();
        // The in-process cluster and gateway are torn down when the (forked) test JVM exits.
    }

    @Test
    @DisplayName("Each client only receives command responses for the requests it sent")
    void shouldIsolateCommandResponsesPerClient() {
        // Arrange: give every client its own cache and its own pair of correlation ids.
        var caches = new HashMap<TestClient, String>();
        var createCorr = new HashMap<TestClient, String>();
        var addCorr = new HashMap<TestClient, String>();
        for (TestClient c : clients) {
            caches.put(c, uniqueCache("iso-" + c.name));
            createCorr.put(c, correlationId());
            addCorr.put(c, correlationId());
        }

        // Act: interleave the sends across all clients so responses race back concurrently.
        for (TestClient c : clients) {
            c.client.createCache(createCorr.get(c), caches.get(c));
        }
        for (TestClient c : clients) {
            c.awaitCommandSuccess(createCorr.get(c));
            c.client.addEntry(addCorr.get(c), caches.get(c), "k", "v", TTL_NONE);
        }
        for (TestClient c : clients) {
            c.awaitCommandSuccess(addCorr.get(c));
        }

        // Assert: each client saw its own correlation ids and none belonging to another client.
        // Correlation ids are globally-unique UUIDs, so any foreign id in a client's callbacks is a routing leak.
        for (TestClient c : clients) {
            Set<String> mine = Set.of(createCorr.get(c), addCorr.get(c));
            Set<String> seen = c.listener.commandResponses.keySet();
            assertTrue(seen.containsAll(mine), c.name + " is missing responses to its own requests");

            Set<String> foreign = new HashSet<>();
            for (TestClient other : clients) {
                if (other != c) {
                    foreign.add(createCorr.get(other));
                    foreign.add(addCorr.get(other));
                }
            }
            assertTrue(Collections.disjoint(seen, foreign),
                    c.name + " received a command response meant for another client");
        }
    }

    @Test
    @DisplayName("Concurrent getEntries return each client only the contents of its own cache")
    void shouldIsolateGetEntriesPerClient() {
        // Arrange: distinct caches with distinct contents, one per client.
        var cacheA = uniqueCache("entries-A");
        var cacheB = uniqueCache("entries-B");
        var cacheC = uniqueCache("entries-C");
        var contentA = Map.of("a1", "va1", "a2", "va2");
        var contentB = Map.of("b1", "vb1");
        var contentC = Map.of("c1", "vc1", "c2", "vc2", "c3", "vc3");
        createAndFill(clientA, cacheA, contentA);
        createAndFill(clientB, cacheB, contentB);
        createAndFill(clientC, cacheC, contentC);

        // Act: each client reads its own cache concurrently, with its own correlation id.
        var getA = correlationId();
        var getB = correlationId();
        var getC = correlationId();
        clientA.client.getEntries(getA, cacheA);
        clientB.client.getEntries(getB, cacheB);
        clientC.client.getEntries(getC, cacheC);
        clientA.awaitEntries(getA);
        clientB.awaitEntries(getB);
        clientC.awaitEntries(getC);

        // Assert: each client got exactly its own cache's contents...
        assertEquals(contentA, clientA.listener.entriesAccumulated.get(getA));
        assertEquals(contentB, clientB.listener.entriesAccumulated.get(getB));
        assertEquals(contentC, clientC.listener.entriesAccumulated.get(getC));

        // ...and no client observed another client's getEntries correlation id.
        assertFalse(clientB.listener.entriesAccumulated.containsKey(getA), "B saw A's entries response");
        assertFalse(clientC.listener.entriesAccumulated.containsKey(getA), "C saw A's entries response");
        assertFalse(clientA.listener.entriesAccumulated.containsKey(getB), "A saw B's entries response");
        assertFalse(clientC.listener.entriesAccumulated.containsKey(getB), "C saw B's entries response");
        assertFalse(clientA.listener.entriesAccumulated.containsKey(getC), "A saw C's entries response");
        assertFalse(clientB.listener.entriesAccumulated.containsKey(getC), "B saw C's entries response");
    }

    @Test
    @DisplayName("Streaming updates are delivered only to the subscribed client")
    void shouldRouteStreamUpdatesToSubscribersOnly() {
        // Arrange: clientA creates a cache and subscribes to it; B and C do not subscribe.
        var cacheId = uniqueCache("stream");
        var createCorr = correlationId();
        clientA.client.createCache(createCorr, cacheId);
        clientA.awaitCommandSuccess(createCorr);

        var subCorr = correlationId();
        clientA.client.subscribe(subCorr, List.of(cacheId), false, false);
        // Ensure the gateway has processed clientA's subscribe before anyone writes. Frames on a single
        // client's publication are ordered, so a subsequent round-trip completing proves the subscribe
        // frame was already consumed by the ingress agent (which handles all sessions on one thread).
        var probeCorr = correlationId();
        clientA.client.getStats(probeCorr);
        clientA.awaitStats(probeCorr);

        // Act: a different, non-subscribed client (B) writes to the cache.
        var addCorr = correlationId();
        clientB.client.addEntry(addCorr, cacheId, "sk", "sv", TTL_NONE);
        clientB.awaitCommandSuccess(addCorr);

        // Assert: the subscriber (A) receives the ADD_ITEM update...
        await().atMost(30, SECONDS).until(() -> clientA.listener.streamUpdates.stream()
                .anyMatch(u -> u.eventType() == UpdateEventType.ADD_ITEM
                        && u.cacheId().equals(cacheId)
                        && u.key().equals("sk")));

        // ...and the non-subscribed clients never receive a streaming update for that cache.
        assertTrue(clientB.listener.streamUpdates.stream().noneMatch(u -> u.cacheId().equals(cacheId)),
                "non-subscribed client B received a streaming update");
        assertTrue(clientC.listener.streamUpdates.stream().noneMatch(u -> u.cacheId().equals(cacheId)),
                "non-subscribed client C received a streaming update");
    }

    // ------------------------------------------------------------------ helpers

    private static void createAndFill(TestClient c, String cacheId, Map<String, String> entries) {
        var createCorr = correlationId();
        c.client.createCache(createCorr, cacheId);
        c.awaitCommandSuccess(createCorr);
        entries.forEach((k, v) -> {
            var corr = correlationId();
            c.client.addEntry(corr, cacheId, k, v, TTL_NONE);
            c.awaitCommandSuccess(corr);
        });
    }

    private static String uniqueCache(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private static String correlationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * A single gateway client bundled with its own media driver, Aeron client, agent runner and
     * recording listener, so each behaves like an independent client process.
     */
    private static final class TestClient {

        final String name;
        final MediaDriver mediaDriver;
        final Aeron aeron;
        final GatewayClient client;
        final AgentRunner runner;
        final RecordingListener listener;

        private TestClient(String name, MediaDriver mediaDriver, Aeron aeron,
                           GatewayClient client, AgentRunner runner, RecordingListener listener) {
            this.name = name;
            this.mediaDriver = mediaDriver;
            this.aeron = aeron;
            this.client = client;
            this.runner = runner;
            this.listener = listener;
        }

        static TestClient connect(String name) throws Exception {
            var aeronDir = Files.createTempDirectory("gw-client-" + name + "-aeron").toString();
            var mediaDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()
                    .aeronDirectoryName(aeronDir)
                    .threadingMode(ThreadingMode.SHARED)
                    .dirDeleteOnStart(true)
                    .dirDeleteOnShutdown(true));
            var aeron = Aeron.connect(new Aeron.Context()
                    .aeronDirectoryName(mediaDriver.aeronDirectoryName()));
            var listener = new RecordingListener();
            var client = new GatewayClient(aeron, REQUEST_ENDPOINT, REQUEST_STREAM_ID,
                    RESPONSE_CONTROL_ENDPOINT, RESPONSE_STREAM_ID, listener);
            var runner = new AgentRunner(new SleepingMillisIdleStrategy(1),
                    Throwable::printStackTrace, null, client);
            AgentRunner.startOnThread(runner);

            var testClient = new TestClient(name, mediaDriver, aeron, client, runner, listener);
            testClient.awaitConnected();
            return testClient;
        }

        void awaitConnected() {
            // The gateway creates this client's response publication lazily on its first request frame,
            // so drive a harmless probe until both request and response channels are connected.
            await().atMost(120, SECONDS).pollInterval(250, MILLISECONDS).until(() -> {
                client.getStats("warmup-" + name);
                return client.isConnected();
            });
        }

        void awaitCommandSuccess(String correlationId) {
            await().atMost(30, SECONDS).until(() -> listener.commandResponses.containsKey(correlationId));
            assertEquals(OperationStatus.SUCCESS, listener.commandResponses.get(correlationId).status(),
                    name + " command " + correlationId + " did not succeed");
        }

        void awaitEntries(String correlationId) {
            await().atMost(30, SECONDS).until(() -> listener.entriesComplete.containsKey(correlationId));
        }

        void awaitStats(String correlationId) {
            await().atMost(30, SECONDS).until(() -> listener.statsComplete.containsKey(correlationId));
        }

        void close() {
            CloseHelper.quietClose(runner);
            CloseHelper.quietClose(aeron);
            CloseHelper.quietClose(mediaDriver);
        }
    }

    /**
     * Records callbacks from a {@link GatewayClient} for assertion. Entries and stats are accumulated
     * per correlation id across batches until an end-of-batch frame is seen.
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
