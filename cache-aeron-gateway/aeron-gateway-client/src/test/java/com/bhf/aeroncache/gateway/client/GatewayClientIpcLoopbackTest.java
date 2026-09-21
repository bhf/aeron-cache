package com.bhf.aeroncache.gateway.client;

import com.bhf.aeroncache.gateway.messages.OperationStatus;
import com.bhf.aeroncache.gateway.messages.UpdateEventType;
import com.bhf.aeroncache.transport.TransportMedia;
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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Mirror of {@link GatewayClientLoopbackTest} but over the IPC transport media instead of UDP,
 * proving that Aeron response channels ({@code control-mode=response} + {@code response-correlation-id})
 * round-trip over {@code aeron:ipc} with no endpoints. The client and {@link LoopbackGatewayServer}
 * share the one embedded media driver, as IPC requires.
 */
class GatewayClientIpcLoopbackTest {

    private static final int REQUEST_STREAM_ID = 2000;
    private static final int RESPONSE_STREAM_ID = 2001;
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
        mediaDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()
                .threadingMode(ThreadingMode.SHARED)
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true));
        aeron = Aeron.connect(new Aeron.Context().aeronDirectoryName(mediaDriver.aeronDirectoryName()));

        listener = new RecordingListener();
        // Endpoints are ignored for IPC, so pass null to prove they are never used.
        client = new GatewayClient(aeron, TransportMedia.IPC, null, REQUEST_STREAM_ID, null, RESPONSE_STREAM_ID, listener);
        server = new LoopbackGatewayServer(aeron, TransportMedia.IPC, null, null, REQUEST_STREAM_ID, RESPONSE_STREAM_ID);

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

    @Test
    @DisplayName("Should route a command over IPC response channels and deliver the acknowledgement to the client")
    void shouldRoundTripCommandOverIpc() {
        // Arrange
        final String correlationId = "ipc-create-1";

        // Act
        await().atMost(TIMEOUT).until(() -> client.createCache(correlationId, "cacheA") > 0);

        // Assert
        await().atMost(TIMEOUT).until(() -> listener.commandResponses.containsKey(correlationId));
        assertEquals(OperationStatus.SUCCESS, listener.commandResponses.get(correlationId));
        await().atMost(TIMEOUT).until(client::isConnected);
    }

    /**
     * Minimal listener that records only what this test asserts on: the status of each command response
     * keyed by correlation id.
     */
    private static final class RecordingListener implements GatewayClientListener {

        private final Map<String, OperationStatus> commandResponses = new ConcurrentHashMap<>();

        @Override
        public void onCommandResponse(String correlationId, OperationStatus status, String cacheId, String key, String value) {
            commandResponses.put(correlationId, status);
        }

        @Override
        public void onEntries(String correlationId, OperationStatus status, String cacheId, Map<String, String> items, boolean endOfBatch) {
        }

        @Override
        public void onStats(String correlationId, OperationStatus status, List<GatewayStat> stats, boolean endOfBatch) {
        }

        @Override
        public void onSubscribeAck(String correlationId, OperationStatus status, List<String> cacheIds) {
        }

        @Override
        public void onStreamUpdate(String correlationId, UpdateEventType eventType, String cacheId, String key, String value) {
        }

        @Override
        public void onError(String correlationId, OperationStatus status, String message) {
        }
    }
}
