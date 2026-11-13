package com.bhf.aeroncache.integration.streaming.near;

import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.streaming.BidiWsHelper;
import com.bhf.aeroncache.ws.bidi.messages.WsOp;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Near cache read coverage for the command-over-socket half of the bidirectional websocket endpoint
 * ({@code /api/ws/v1/bidi}). When the near cache is enabled the bidi {@code GET_CACHE_ENTRY} /
 * {@code GET_CACHE_ENTRIES} commands are served strictly from the local copy with the same semantics as
 * the HTTP near cache read routes ({@code NearCacheRouteHandlers}): a warm hit returns the local value,
 * and any miss primes the cache (idempotently) and returns {@code UNKNOWN_KEY} / {@code UNKNOWN_CACHE}
 * without ever reading through to the cluster. Mutations are issued over the same bidi socket (they go to
 * the cluster); the local copy is warmed by the prime that a miss kicks off.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true,
        useTestContainersEnvironment = true, streamingNearCacheEnabled = true)
class ClusteredBidiNearGetTests {

    private static final long RESPONSE_TIMEOUT_SECONDS = 10;
    private static final long WARM_TIMEOUT_SECONDS = 30;
    private static final String CACHE_PREFIX = "clustered-near-get-bidi";

    private BidiWsHelper helper;

    @BeforeEach
    void setUp(BackendTestResource backend) {
        helper = new BidiWsHelper().connect(backend);
    }

    @AfterEach
    void tearDown() {
        if (helper != null) {
            helper.close();
        }
    }

    @Test
    @DisplayName("Point GET is served from the near cache once a miss has primed and warmed it")
    void shouldServePointGetFromNearCacheOnceWarm() {
        // Arrange: create a cache and add an entry over the bidi socket (the write goes to the cluster; the
        // near cache for this cache is not warm yet).
        var cacheId = CACHE_PREFIX + "-point";
        createCacheAndEntry(cacheId, "k1", "v1");

        // Act + Assert: the first near read of the not-yet-warm key is a miss that kicks off priming (no
        // cluster read-through); each poll re-issues the GET, and once the warming subscription has populated
        // the local copy the key is served locally with its value.
        await().atMost(WARM_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var response = issueGetAndAwait(WsOp.GET_CACHE_ENTRY, cacheId, "k1");
            assertThat(response.get("status").asText(), is("SUCCESS"));
            assertThat(response.get("key").asText(), is("k1"));
            assertThat(response.get("value").asText(), is("v1"));
        });
    }

    @Test
    @DisplayName("Point GET on an unknown cache is a near-cache miss (UNKNOWN_KEY), never a read-through")
    void shouldReturnUnknownKeyAsNearMiss() {
        // Arrange: a cache id that was never created, so the near cache can never warm it.
        var unknownCacheId = CACHE_PREFIX + "-missing";

        // Act: read a key from it over the bidi socket.
        var response = issueGetAndAwait(WsOp.GET_CACHE_ENTRY, unknownCacheId, "nope");

        // Assert: served as a near-cache miss, not a cluster read-through unknown-cache sentinel.
        assertThat(response.get("status").asText(), is("UNKNOWN_KEY"));
    }

    @Test
    @DisplayName("Whole-cache GET is served from the near cache once a miss has primed and warmed it")
    void shouldServeWholeCacheGetFromNearCacheOnceWarm() {
        // Arrange: create a cache and add an entry over the bidi socket.
        var cacheId = CACHE_PREFIX + "-whole";
        createCacheAndEntry(cacheId, "a", "1");

        // Act + Assert: the whole-cache near read warms on a miss and, once warm, returns a single
        // end-of-batch SUCCESS entries frame carrying the local snapshot.
        await().atMost(WARM_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var entriesId = helper.newCorrelationId();
            helper.command(WsOp.GET_CACHE_ENTRIES, entriesId, cacheId, null, null, 0, 0);
            await().atMost(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .until(() -> helper.firstFrame(entriesId, "entries").isPresent());
            var frame = helper.firstFrame(entriesId, "entries").orElseThrow();
            assertThat(frame.get("status").asText(), is("SUCCESS"));
            assertThat(frame.get("endOfBatch").asBoolean(), is(true));
            assertThat(frame.get("items").path("a").asText(), is("1"));
        });
    }

    @Test
    @DisplayName("Whole-cache GET on an unknown cache is a near-cache miss (UNKNOWN_CACHE)")
    void shouldReturnUnknownCacheAsNearMiss() {
        // Arrange: a cache id that was never created.
        var unknownCacheId = CACHE_PREFIX + "-missing-whole";
        var entriesId = helper.newCorrelationId();

        // Act: request the whole cache over the bidi socket.
        helper.command(WsOp.GET_CACHE_ENTRIES, entriesId, unknownCacheId, null, null, 0, 0);
        await().atMost(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> helper.firstFrame(entriesId, "entries").isPresent());

        // Assert: an empty end-of-batch UNKNOWN_CACHE frame, with no cluster read-through.
        var frame = helper.firstFrame(entriesId, "entries").orElseThrow();
        assertThat(frame.get("status").asText(), is("UNKNOWN_CACHE"));
        assertThat(frame.get("endOfBatch").asBoolean(), is(true));
    }

    private void createCacheAndEntry(String cacheId, String key, String value) {
        var createId = helper.newCorrelationId();
        helper.command(WsOp.CREATE_CACHE, createId, cacheId, null, null, 0, 0);
        awaitCommandResponse(createId);
        var addId = helper.newCorrelationId();
        helper.command(WsOp.ADD_CACHE_ENTRY, addId, cacheId, key, value, 0, 0);
        awaitCommandResponse(addId);
    }

    private JsonNode issueGetAndAwait(WsOp op, String cacheId, String key) {
        var correlationId = helper.newCorrelationId();
        helper.command(op, correlationId, cacheId, key, null, 0, 0);
        return awaitCommandResponse(correlationId);
    }

    private JsonNode awaitCommandResponse(String correlationId) {
        await().atMost(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> helper.firstFrame(correlationId, "commandResponse").isPresent());
        return helper.firstFrame(correlationId, "commandResponse").orElseThrow();
    }
}
