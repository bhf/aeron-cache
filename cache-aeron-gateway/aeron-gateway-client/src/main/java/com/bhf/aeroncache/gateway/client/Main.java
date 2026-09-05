package com.bhf.aeroncache.gateway.client;

import com.bhf.aeroncache.gateway.messages.OperationStatus;
import com.bhf.aeroncache.gateway.messages.UpdateEventType;
import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import lombok.extern.log4j.Log4j2;
import org.agrona.CloseHelper;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.SleepingMillisIdleStrategy;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Standalone local driver for manual testing against a running gateway server.
 * <p>
 * Connects a {@link GatewayClient} (over its own embedded {@link MediaDriver}) to a gateway,
 * creates a randomly named cache and then publishes a random key/value pair to it on a fixed
 * interval, logging every response. Intended for eyeballing the gateway end to end while it is
 * running locally (e.g. via {@code cache-monolith} or the standalone gateway server).
 * <p>
 * Configuration (system property, else environment variable, else default):
 * <ul>
 *   <li>{@code gateway.request.endpoint} / {@code GATEWAY_REQUEST_ENDPOINT} - default {@code localhost:7075}</li>
 *   <li>{@code gateway.response.control} / {@code GATEWAY_RESPONSE_CONTROL_ENDPOINT} - default {@code localhost:7076}</li>
 *   <li>{@code gateway.request.streamId} - default {@code 100}</li>
 *   <li>{@code gateway.response.streamId} - default {@code 101}</li>
 *   <li>{@code gateway.publish.interval.ms} - default {@code 1000}</li>
 *   <li>{@code gateway.cacheId} - default a random {@code cache-<uuid>}</li>
 * </ul>
 */
@Log4j2
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        final String requestEndpoint = config("gateway.request.endpoint", "GATEWAY_REQUEST_ENDPOINT", "localhost:7075");
        final String responseControl = config("gateway.response.control", "GATEWAY_RESPONSE_CONTROL_ENDPOINT", "localhost:7076");
        final int requestStreamId = Integer.parseInt(config("gateway.request.streamId", "GATEWAY_REQUEST_STREAM_ID", "100"));
        final int responseStreamId = Integer.parseInt(config("gateway.response.streamId", "GATEWAY_RESPONSE_STREAM_ID", "101"));
        final long intervalMs = Long.parseLong(config("gateway.publish.interval.ms", "GATEWAY_PUBLISH_INTERVAL_MS", "1000"));
        final String cacheId = config("gateway.cacheId", "GATEWAY_CACHE_ID", "cache-" + shortId());

        log.info("Gateway test publisher starting: requestEndpoint={}, responseControl={}, requestStreamId={}, " +
                "responseStreamId={}, intervalMs={}, cacheId={}",
                requestEndpoint, responseControl, requestStreamId, responseStreamId, intervalMs, cacheId);

        final String aeronDir = Files.createTempDirectory("gateway-client-main-aeron").toString();
        final MediaDriver mediaDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()
                .aeronDirectoryName(aeronDir)
                .threadingMode(ThreadingMode.SHARED)
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true));

        final Aeron aeron = Aeron.connect(new Aeron.Context()
                .aeronDirectoryName(mediaDriver.aeronDirectoryName()));

        final GatewayClient client = new GatewayClient(aeron, requestEndpoint, requestStreamId,
                responseControl, responseStreamId, new LoggingListener());

        final AgentRunner clientRunner = new AgentRunner(new SleepingMillisIdleStrategy(1),
                Throwable::printStackTrace, null, client);
        AgentRunner.startOnThread(clientRunner);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down gateway test publisher");
            CloseHelper.quietCloseAll(clientRunner, aeron, mediaDriver);
        }));

        awaitConnected(client);
        log.info("Connected to gateway; creating cache {}", cacheId);
        client.createCache(UUID.randomUUID().toString(), cacheId);

        long published = 0;
        while (true) {
            final String key = "key-" + shortId();
            final String value = "value-" + ThreadLocalRandom.current().nextInt(1_000_000);
            final long result = client.addEntry(UUID.randomUUID().toString(), cacheId, key, value, 0L);
            published++;
            log.info("Published #{} to {} -> {}={} (offer result {})", published, cacheId, key, value, result);
            Thread.sleep(intervalMs);
        }
    }

    /**
     * The gateway creates a client's response publication lazily on receipt of the first request frame,
     * so we must send something before {@link GatewayClient#isConnected()} can become true. Repeatedly
     * send a harmless stats probe until both the request and response channels are connected.
     */
    private static void awaitConnected(GatewayClient client) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + 60_000L;
        while (!client.isConnected()) {
            client.getStats("connection-warmup");
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("Timed out waiting to connect to the gateway");
            }
            Thread.sleep(250L);
        }
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String config(String systemProperty, String envVar, String defaultValue) {
        final String fromProperty = System.getProperty(systemProperty);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty;
        }
        final String fromEnv = System.getenv(envVar);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return defaultValue;
    }

    /**
     * Logs every response, streamed batch and error received from the gateway.
     */
    private static final class LoggingListener implements GatewayClientListener {

        @Override
        public void onCommandResponse(String correlationId, OperationStatus status, String cacheId, String key, String value) {
            log.info("Response [{}] status={} cacheId={} key={} value={}", correlationId, status, cacheId, key, value);
        }

        @Override
        public void onEntries(String correlationId, OperationStatus status, String cacheId, Map<String, String> items, boolean endOfBatch) {
            log.info("Entries [{}] status={} cacheId={} items={} endOfBatch={}", correlationId, status, cacheId, items, endOfBatch);
        }

        @Override
        public void onStats(String correlationId, OperationStatus status, List<GatewayStat> stats, boolean endOfBatch) {
            log.info("Stats [{}] status={} stats={} endOfBatch={}", correlationId, status, stats, endOfBatch);
        }

        @Override
        public void onStreamUpdate(String correlationId, UpdateEventType eventType, String cacheId, String key, String value) {
            log.info("Stream update [{}] type={} cacheId={} key={} value={}", correlationId, eventType, cacheId, key, value);
        }

        @Override
        public void onError(String correlationId, OperationStatus status, String message) {
            log.warn("Error [{}] status={} message={}", correlationId, status, message);
        }
    }
}
