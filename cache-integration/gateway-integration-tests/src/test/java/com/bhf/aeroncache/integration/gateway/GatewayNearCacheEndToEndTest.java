package com.bhf.aeroncache.integration.gateway;

import com.bhf.aeroncache.application.ClusterLauncher;
import com.bhf.aeroncache.gateway.application.GatewayApplication;
import com.bhf.aeroncache.gateway.client.GatewayClient;
import com.bhf.aeroncache.gateway.messages.OperationStatus;
import com.bhf.aeroncache.gateway.messages.UpdateEventType;
import com.bhf.aeroncache.utils.ClusterUtils;
import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.agrona.CloseHelper;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Full-stack embedded end-to-end test for the Aeron gateway's near cache, backed by a real in-process
 * RAFT cluster and a real {@link GatewayClient} ({@code NEAR_CACHE_ENABLED=true} via {@code test.env}).
 * <p>
 * The near cache is transparent on the subscription path: a whole-cache hydrating subscribe is served
 * exactly as before, but the first such subscriber warms a local copy that keeps itself in sync with
 * live updates (so later subscribers hydrate locally without a per-subscriber cluster snapshot). This
 * exercises that it warms, stays in sync, is listed/dropped via the near cache HTTP management endpoints
 * on the gateway's health server, and that hydration remains correct throughout &mdash; the gateway
 * counterpart of {@code AbstractStreamingNearCacheTests} for the WS/SSE interfaces.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Gateway near cache embedded end-to-end")
class GatewayNearCacheEndToEndTest {

    private static final String REQUEST_ENDPOINT = "localhost:7075";
    private static final String RESPONSE_CONTROL_ENDPOINT = "localhost:7076";
    private static final int REQUEST_STREAM_ID = 100;
    private static final int RESPONSE_STREAM_ID = 101;
    private static final int CLUSTER_NODES = 3;
    private static final long TTL_NONE = 0L;

    private static final String NEAR_CACHES_ENDPOINT = "/api/v1/near/caches";
    private static final String NEAR_CACHE_PREFIX = "/api/v1/near/cache/";
    private static final String NEAR_COUNTERS_CACHES_ENDPOINT = "/api/v1/near/counters-caches";
    private static final String NEAR_COUNTERS_PREFIX = "/api/v1/near/counters/";

    private MediaDriver clientMediaDriver;
    private Aeron clientAeron;
    private AgentRunner clientRunner;

    private GatewayClient client;
    private RecordingListener listener;
    private int gatewayHttpPort;

    @BeforeAll
    void startBackendAndClient() throws Exception {
        // Start the in-process RAFT cluster (each node launches its own media driver).
        ClusterLauncher.launchTestCluster(CLUSTER_NODES, "gateway_near_e2e");

        // Start the real gateway (0 => ephemeral HTTP health port). It reads NEAR_CACHE_ENABLED from the
        // environment (set true by test.env), connects to the cluster and stands up the ingress.
        gatewayHttpPort = GatewayApplication.start(0);

        var clientAeronDir = Files.createTempDirectory("gw-near-client-aeron").toString();
        clientMediaDriver = MediaDriver.launchEmbedded(ClusterUtils.applyConfiguredTermLength(new MediaDriver.Context()
                .aeronDirectoryName(clientAeronDir)
                .threadingMode(ThreadingMode.SHARED)
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true)));
        clientAeron = Aeron.connect(new Aeron.Context()
                .aeronDirectoryName(clientMediaDriver.aeronDirectoryName()));

        listener = new RecordingListener();
        client = new GatewayClient(clientAeron, REQUEST_ENDPOINT, REQUEST_STREAM_ID,
                RESPONSE_CONTROL_ENDPOINT, RESPONSE_STREAM_ID, listener);
        clientRunner = new AgentRunner(new SleepingMillisIdleStrategy(1), Throwable::printStackTrace, null, client);
        AgentRunner.startOnThread(clientRunner);

        // The gateway creates a client's response publication lazily on its first request frame, so probe
        // until both channels connect.
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
        // The in-process cluster and gateway are torn down when the (forked) test JVM exits.
    }

    @Test
    @DisplayName("Should warm the near cache on a hydrating subscription, keep it in sync, list it, hydrate correctly, then drop it on stop")
    void shouldWarmSyncListHydrateAndStop() {
        var cacheId = uniqueCache("gateway-near-cache");

        // Seed the source cache.
        createCache(cacheId);
        addEntry(cacheId, "Key1", "Value1");
        addEntry(cacheId, "Key2", "Value2");

        // A whole-cache hydrating subscription warms the near cache and is itself hydrated with both items.
        var subCorr = correlationId();
        client.subscribe(subCorr, List.of(cacheId), true, false);
        awaitSubscribeAck(subCorr);
        await().atMost(60, SECONDS).until(() -> streamed(UpdateEventType.ADD_ITEM, cacheId, "Key1", "Value1"));
        await().atMost(60, SECONDS).until(() -> streamed(UpdateEventType.ADD_ITEM, cacheId, "Key2", "Value2"));

        // The cache is now listed as warm with its two local items.
        await().atMost(60, SECONDS).untilAsserted(() ->
                RestAssured.given().port(gatewayHttpPort).accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.hasItem(cacheId))
                        .body("find { it.cacheId == '" + cacheId + "' }.itemCount", Matchers.equalTo(2)));

        // A live mutation after warming is kept in sync by the internal near cache session: the local item
        // count grows to three without any new hydrating subscription.
        addEntry(cacheId, "Key3", "Value3");
        await().atMost(60, SECONDS).untilAsserted(() ->
                RestAssured.given().port(gatewayHttpPort).accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("find { it.cacheId == '" + cacheId + "' }.itemCount", Matchers.equalTo(3)));

        // Stop near caching the cache: the local copy is dropped.
        RestAssured.given().port(gatewayHttpPort).accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", Matchers.comparesEqualTo(cacheId))
                .body("wasCached", Matchers.equalTo(true))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // It should no longer be listed as warm.
        await().atMost(60, SECONDS).untilAsserted(() ->
                RestAssured.given().port(gatewayHttpPort).accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.not(Matchers.hasItem(cacheId))));
    }

    @Test
    @DisplayName("Should prime a cold near cache up front so a later hydrating subscription is served locally, then drop it on stop")
    void shouldPrimeWarmHydrateAndStop() {
        var cacheId = uniqueCache("gateway-near-primed");

        // Seed the source cache.
        createCache(cacheId);
        addEntry(cacheId, "Key1", "Value1");
        addEntry(cacheId, "Key2", "Value2");

        // Prime the near cache up front, with no subscription yet: reports it was not already warm.
        RestAssured.given().port(gatewayHttpPort).accept(ContentType.JSON)
                .when().post(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", Matchers.comparesEqualTo(cacheId))
                .body("wasAlreadyWarm", Matchers.equalTo(false))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // The snapshot populates the local copy asynchronously; it is then listed warm with both items.
        await().atMost(60, SECONDS).untilAsserted(() ->
                RestAssured.given().port(gatewayHttpPort).accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.hasItem(cacheId))
                        .body("find { it.cacheId == '" + cacheId + "' }.itemCount", Matchers.equalTo(2)));

        // Priming an already-warm cache is an idempotent no-op that reports it was already warm.
        RestAssured.given().port(gatewayHttpPort).accept(ContentType.JSON)
                .when().post(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("wasAlreadyWarm", Matchers.equalTo(true))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // A hydrating subscription arriving after priming is served the full state from the warm local copy.
        var subCorr = correlationId();
        client.subscribe(subCorr, List.of(cacheId), true, false);
        awaitSubscribeAck(subCorr);
        await().atMost(60, SECONDS).until(() -> streamed(UpdateEventType.ADD_ITEM, cacheId, "Key1", "Value1"));
        await().atMost(60, SECONDS).until(() -> streamed(UpdateEventType.ADD_ITEM, cacheId, "Key2", "Value2"));

        // Stop near caching the cache: the local copy is dropped.
        RestAssured.given().port(gatewayHttpPort).accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("wasCached", Matchers.equalTo(true))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));
    }

    @Test
    @DisplayName("Should succeed with wasCached=false when stopping a cache that was never warm")
    void shouldReportNotCachedWhenStoppingUnwarmedCache() {
        RestAssured.given().port(gatewayHttpPort).accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + uniqueCache("never-warmed"))
                .then().assertThat()
                .statusCode(200)
                .body("wasCached", Matchers.equalTo(false))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));
    }

    @Test
    @DisplayName("Should warm, list and stop the counter near cache independently of the regular near cache")
    void shouldWarmListAndStopCounterNearCache() {
        var cacheId = uniqueCache("gateway-near-counter");

        createCounterCache(cacheId);
        addCounterEntry(cacheId, "hits", 5L);

        // A whole-cache hydrating counter subscription warms the counter near cache.
        var subCorr = correlationId();
        client.subscribe(subCorr, List.of(cacheId), true, true);
        awaitSubscribeAck(subCorr);
        await().atMost(60, SECONDS).until(() -> streamed(UpdateEventType.ADD_ITEM, cacheId, "hits", "5"));

        // Listed under the counter near cache management endpoint with its single item.
        await().atMost(60, SECONDS).untilAsserted(() ->
                RestAssured.given().port(gatewayHttpPort).accept(ContentType.JSON)
                        .when().get(NEAR_COUNTERS_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.hasItem(cacheId))
                        .body("find { it.cacheId == '" + cacheId + "' }.itemCount", Matchers.equalTo(1)));

        // Stopping it drops the local copy.
        RestAssured.given().port(gatewayHttpPort).accept(ContentType.JSON)
                .when().delete(NEAR_COUNTERS_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("wasCached", Matchers.equalTo(true))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        await().atMost(60, SECONDS).untilAsserted(() ->
                RestAssured.given().port(gatewayHttpPort).accept(ContentType.JSON)
                        .when().get(NEAR_COUNTERS_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.not(Matchers.hasItem(cacheId))));
    }

    // ------------------------------------------------------------------ helpers

    private boolean streamed(UpdateEventType eventType, String cacheId, String key, String value) {
        return listener.streamUpdates.stream()
                .anyMatch(u -> u.eventType() == eventType
                        && u.cacheId().equals(cacheId)
                        && key.equals(u.key())
                        && value.equals(u.value()));
    }

    private void createCache(String cacheId) {
        var corr = correlationId();
        client.createCache(corr, cacheId);
        awaitCommandSuccess(corr);
    }

    private void addEntry(String cacheId, String key, String value) {
        var corr = correlationId();
        client.addEntry(corr, cacheId, key, value, TTL_NONE);
        awaitCommandSuccess(corr);
    }

    private void createCounterCache(String cacheId) {
        var corr = correlationId();
        client.createCounterCache(corr, cacheId);
        awaitCommandSuccess(corr);
    }

    private void addCounterEntry(String cacheId, String key, long value) {
        var corr = correlationId();
        client.addCounterEntry(corr, cacheId, key, value, TTL_NONE);
        awaitCommandSuccess(corr);
    }

    private void awaitCommandSuccess(String correlationId) {
        await().atMost(30, SECONDS).until(() -> listener.commandResponses.containsKey(correlationId));
        assertEquals(OperationStatus.SUCCESS, listener.commandResponses.get(correlationId).status(),
                "command " + correlationId + " did not succeed");
    }

    private void awaitSubscribeAck(String correlationId) {
        await().atMost(30, SECONDS).until(() -> listener.subscribeAcks.containsKey(correlationId));
        assertEquals(OperationStatus.SUCCESS, listener.subscribeAcks.get(correlationId).status(),
                "subscription " + correlationId + " was not acknowledged");
    }

    private static String uniqueCache(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private static String correlationId() {
        return UUID.randomUUID().toString();
    }
}
