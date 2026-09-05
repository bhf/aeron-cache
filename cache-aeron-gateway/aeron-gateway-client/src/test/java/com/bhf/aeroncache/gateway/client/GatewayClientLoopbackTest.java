package com.bhf.aeroncache.gateway.client;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.DatagramSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end transport test for {@link GatewayClient} over a real (embedded) Aeron media driver,
 * driving a {@link LoopbackGatewayServer} through Aeron response channels. Verifies request routing,
 * response-channel connectivity and the client's decode/dispatch of every response frame type,
 * including the batch/end-of-batch streaming contract for {@code getEntries}.
 */
class GatewayClientLoopbackTest {

    private static final int REQUEST_STREAM_ID = 1000;
    private static final int RESPONSE_STREAM_ID = 1001;
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private static MediaDriver mediaDriver;
    private static Aeron aeron;
    private static GatewayClient client;
    private static LoopbackGatewayServer server;
    private static AgentRunner clientRunner;
    private static AgentRunner serverRunner;
    private static RecordingListener listener;

    @BeforeAll
    static void startEnvironment() {
        final String requestEndpoint = "127.0.0.1:" + freeUdpPort();
        final String responseControl = "127.0.0.1:" + freeUdpPort();

        mediaDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()
                .threadingMode(ThreadingMode.SHARED)
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true));
        aeron = Aeron.connect(new Aeron.Context().aeronDirectoryName(mediaDriver.aeronDirectoryName()));

        listener = new RecordingListener();
        client = new GatewayClient(aeron, requestEndpoint, REQUEST_STREAM_ID, responseControl, RESPONSE_STREAM_ID, listener);
        server = new LoopbackGatewayServer(aeron, requestEndpoint, responseControl, REQUEST_STREAM_ID, RESPONSE_STREAM_ID);

        serverRunner = new AgentRunner(new SleepingMillisIdleStrategy(1), Throwable::printStackTrace, null, server);
        clientRunner = new AgentRunner(new SleepingMillisIdleStrategy(1), Throwable::printStackTrace, null, client);
        AgentRunner.startOnThread(serverRunner);
        AgentRunner.startOnThread(clientRunner);
    }

    @AfterAll
    static void stopEnvironment() {
        CloseHelper.quietCloseAll(clientRunner, serverRunner);
        CloseHelper.quietClose(aeron);
        CloseHelper.quietClose(mediaDriver);
    }

    @BeforeEach
    void resetRecorder() {
        listener.clear();
    }

    @Test
    @DisplayName("Should route a command over response channels and deliver the acknowledgement to the client")
    void shouldRouteCommandAndDeliverAcknowledgement() {
        // Arrange
        final String correlationId = "create-1";

        // Act
        await().atMost(TIMEOUT).until(() -> client.createCache(correlationId, "cacheA") > 0);

        // Assert
        await().atMost(TIMEOUT).until(() -> listener.commandResponse(correlationId) != null);
        RecordingListener.CommandResponse response = listener.commandResponse(correlationId);
        assertEquals(OperationStatus.SUCCESS, response.status());
        assertEquals("cacheA", response.cacheId());
        await().atMost(TIMEOUT).until(client::isConnected);
    }

    @Test
    @DisplayName("Should stream entries as batches and deliver them to the client until end-of-batch")
    void shouldStreamEntriesInBatchesUntilEndOfBatch() {
        // Arrange
        final String correlationId = "entries-1";

        // Act
        await().atMost(TIMEOUT).until(() -> client.getEntries(correlationId, "cacheA") > 0);

        // Assert
        await().atMost(TIMEOUT).until(() -> listener.hasTerminalEntriesBatch(correlationId));
        List<RecordingListener.EntriesBatch> batches = listener.entriesBatches(correlationId);

        Map<String, String> merged = new LinkedHashMap<>();
        batches.forEach(batch -> merged.putAll(batch.items()));

        assertEquals(Map.of("a", "1", "b", "2", "c", "3"), merged);
        assertFalse(batches.get(0).endOfBatch());
        assertTrue(batches.get(batches.size() - 1).endOfBatch());
        assertEquals(OperationStatus.SUCCESS, batches.get(0).status());
    }

    @Test
    @DisplayName("Should stream cache stats and deliver the decoded group to the client")
    void shouldStreamStats() {
        // Arrange
        final String correlationId = "stats-1";

        // Act
        await().atMost(TIMEOUT).until(() -> client.getStats(correlationId) > 0);

        // Assert
        await().atMost(TIMEOUT).until(() -> listener.statsBatch(correlationId) != null);
        RecordingListener.StatsBatch batch = listener.statsBatch(correlationId);
        assertTrue(batch.endOfBatch());
        assertEquals(1, batch.stats().size());
        GatewayStat stat = batch.stats().get(0);
        assertEquals("cacheStats", stat.cacheId());
        assertEquals(7L, stat.addedCount());
        assertEquals(2L, stat.removedCount());
        assertEquals(1L, stat.clearedCount());
        assertEquals(4L, stat.size());
    }

    @Test
    @DisplayName("Should deliver a streaming update to a subscribed client")
    void shouldDeliverStreamUpdateToSubscriber() {
        // Arrange
        final String correlationId = "sub-1";

        // Act
        await().atMost(TIMEOUT).until(() -> client.subscribe(correlationId, List.of("cacheA"), false, false) > 0);

        // Assert
        await().atMost(TIMEOUT).until(() -> listener.streamUpdate(correlationId) != null);
        RecordingListener.StreamUpdate update = listener.streamUpdate(correlationId);
        assertEquals(UpdateEventType.ADD_ITEM, update.eventType());
        assertEquals("cacheA", update.cacheId());
        assertEquals("k", update.key());
        assertEquals("v", update.value());
    }

    @Test
    @DisplayName("Should deliver an error frame to the client for an unsupported command")
    void shouldDeliverErrorForUnsupportedCommand() {
        // Arrange
        final String correlationId = "err-1";

        // Act
        await().atMost(TIMEOUT).until(() -> client.clearCache(correlationId, "cacheA") > 0);

        // Assert
        await().atMost(TIMEOUT).until(() -> listener.error(correlationId) != null);
        RecordingListener.ErrorRecord error = listener.error(correlationId);
        assertEquals(OperationStatus.ERROR, error.status());
        assertTrue(error.message().startsWith("Unknown msgType"));
    }

    private static int freeUdpPort() {
        try (DatagramSocket socket = new DatagramSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException("Unable to allocate a free UDP port", e);
        }
    }

    /**
     * Thread-safe recorder of all {@link GatewayClientListener} callbacks, queryable by correlation id.
     */
    private static final class RecordingListener implements GatewayClientListener {

        private final List<CommandResponse> commandResponses = new CopyOnWriteArrayList<>();
        private final List<EntriesBatch> entriesBatches = new CopyOnWriteArrayList<>();
        private final List<StatsBatch> statsBatches = new CopyOnWriteArrayList<>();
        private final List<StreamUpdate> streamUpdates = new CopyOnWriteArrayList<>();
        private final List<ErrorRecord> errors = new CopyOnWriteArrayList<>();

        void clear() {
            commandResponses.clear();
            entriesBatches.clear();
            statsBatches.clear();
            streamUpdates.clear();
            errors.clear();
        }

        @Override
        public void onCommandResponse(String correlationId, OperationStatus status, String cacheId, String key, String value) {
            commandResponses.add(new CommandResponse(correlationId, status, cacheId, key, value));
        }

        @Override
        public void onEntries(String correlationId, OperationStatus status, String cacheId, Map<String, String> items, boolean endOfBatch) {
            entriesBatches.add(new EntriesBatch(correlationId, status, cacheId, new LinkedHashMap<>(items), endOfBatch));
        }

        @Override
        public void onStats(String correlationId, OperationStatus status, List<GatewayStat> stats, boolean endOfBatch) {
            statsBatches.add(new StatsBatch(correlationId, status, new ArrayList<>(stats), endOfBatch));
        }

        @Override
        public void onStreamUpdate(String correlationId, UpdateEventType eventType, String cacheId, String key, String value) {
            streamUpdates.add(new StreamUpdate(correlationId, eventType, cacheId, key, value));
        }

        @Override
        public void onError(String correlationId, OperationStatus status, String message) {
            errors.add(new ErrorRecord(correlationId, status, message));
        }

        CommandResponse commandResponse(String correlationId) {
            return commandResponses.stream().filter(r -> r.correlationId().equals(correlationId)).findFirst().orElse(null);
        }

        List<EntriesBatch> entriesBatches(String correlationId) {
            return entriesBatches.stream().filter(b -> b.correlationId().equals(correlationId)).toList();
        }

        boolean hasTerminalEntriesBatch(String correlationId) {
            return entriesBatches.stream().anyMatch(b -> b.correlationId().equals(correlationId) && b.endOfBatch());
        }

        StatsBatch statsBatch(String correlationId) {
            return statsBatches.stream().filter(b -> b.correlationId().equals(correlationId)).findFirst().orElse(null);
        }

        StreamUpdate streamUpdate(String correlationId) {
            return streamUpdates.stream().filter(u -> u.correlationId().equals(correlationId)).findFirst().orElse(null);
        }

        ErrorRecord error(String correlationId) {
            return errors.stream().filter(e -> e.correlationId().equals(correlationId)).findFirst().orElse(null);
        }

        record CommandResponse(String correlationId, OperationStatus status, String cacheId, String key, String value) {
        }

        record EntriesBatch(String correlationId, OperationStatus status, String cacheId, Map<String, String> items, boolean endOfBatch) {
        }

        record StatsBatch(String correlationId, OperationStatus status, List<GatewayStat> stats, boolean endOfBatch) {
        }

        record StreamUpdate(String correlationId, UpdateEventType eventType, String cacheId, String key, String value) {
        }

        record ErrorRecord(String correlationId, OperationStatus status, String message) {
        }
    }
}
