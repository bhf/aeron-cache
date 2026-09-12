package com.bhf.aeroncache.integration.streaming;

import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
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
import static org.hamcrest.Matchers.notNullValue;

/**
 * Shared end-to-end coverage of the command-over-socket half of the bidirectional websocket protocol:
 * commands are issued over the {@code /api/ws/v1/bidi} connection and correlated responses are asserted.
 * This is the part the HTTP-mutation abstract suites cannot cover (they mutate over HTTP). Mirrors the
 * scenario matrix of the gateway's {@code GatewayClientLoopbackTest} over the websocket transport.
 *
 * <p>Concrete subclasses supply the backend flavour via {@code @BackendTestConfig} (clustered vs
 * ephemeral) and a {@link #cacheIdPrefix()} that namespaces cache ids so the two runs never collide.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
public abstract class AbstractBidiCommandTests {

    private static final long TIMEOUT_SECONDS = 60;

    private BidiWsHelper helper;

    /**
     * A prefix that namespaces the cache ids used by this suite, so clustered and ephemeral variants
     * (which may share a backend lifecycle) do not collide.
     *
     * @return the cache id prefix.
     */
    protected abstract String cacheIdPrefix();

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
    @DisplayName("Should create a cache, add an entry and read it back over the BIDI socket")
    protected void shouldRoundTripEntryOverSocket() {
        // Arrange
        var cacheId = cacheIdPrefix() + "-roundtrip";
        var createId = helper.newCorrelationId();
        var addId = helper.newCorrelationId();
        var getId = helper.newCorrelationId();

        // Act
        helper.command(WsOp.CREATE_CACHE, createId, cacheId, null, null, 0, 0);
        var createResponse = awaitCommandResponse(createId);
        helper.command(WsOp.ADD_CACHE_ENTRY, addId, cacheId, "k1", "v1", 0, 0);
        var addResponse = awaitCommandResponse(addId);
        helper.command(WsOp.GET_CACHE_ENTRY, getId, cacheId, "k1", null, 0, 0);
        var getResponse = awaitCommandResponse(getId);

        // Assert
        assertThat(createResponse.get("status").asText(), is("SUCCESS"));
        assertThat(addResponse.get("status").asText(), is("SUCCESS"));
        assertThat(getResponse.get("status").asText(), is("SUCCESS"));
        assertThat(getResponse.get("key").asText(), is("k1"));
        assertThat(getResponse.get("value").asText(), is("v1"));
    }

    @Test
    @DisplayName("Should stream all entries in an end-of-batch frame for getEntries")
    protected void shouldStreamEntriesOverSocket() {
        // Arrange
        var cacheId = cacheIdPrefix() + "-entries";
        helper.command(WsOp.CREATE_CACHE, helper.newCorrelationId(), cacheId, null, null, 0, 0);
        var addA = helper.newCorrelationId();
        helper.command(WsOp.ADD_CACHE_ENTRY, addA, cacheId, "a", "1", 0, 0);
        awaitCommandResponse(addA);
        var addB = helper.newCorrelationId();
        helper.command(WsOp.ADD_CACHE_ENTRY, addB, cacheId, "b", "2", 0, 0);
        awaitCommandResponse(addB);
        var entriesId = helper.newCorrelationId();

        // Act
        helper.command(WsOp.GET_CACHE_ENTRIES, entriesId, cacheId, null, null, 0, 0);

        // Assert
        await().atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> helper.firstFrame(entriesId, "entries")
                        .filter(f -> f.get("endOfBatch").asBoolean())
                        .isPresent());
    }

    @Test
    @DisplayName("Should return cache stats in an end-of-batch frame for getStats")
    protected void shouldReturnStatsOverSocket() {
        // Arrange
        var statsId = helper.newCorrelationId();

        // Act
        helper.command(WsOp.GET_CACHE_STATS, statsId, null, null, null, 0, 0);

        // Assert
        await().atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> helper.firstFrame(statsId, "stats")
                        .filter(f -> f.get("endOfBatch").asBoolean())
                        .isPresent());
    }

    @Test
    @DisplayName("Should increment a counter over the BIDI socket")
    protected void shouldIncrementCounterOverSocket() {
        // Arrange
        var cacheId = cacheIdPrefix() + "-counter";
        var createId = helper.newCorrelationId();
        var addId = helper.newCorrelationId();
        var incId = helper.newCorrelationId();

        // Act
        helper.command(WsOp.CREATE_COUNTER_CACHE, createId, cacheId, null, null, 0, 0);
        var createResponse = awaitCommandResponse(createId);
        // Counters must be initialised before they can be incremented (see the increment streaming suite).
        helper.command(WsOp.ADD_COUNTER_ENTRY, addId, cacheId, "hits", null, 0, 0);
        var addResponse = awaitCommandResponse(addId);
        helper.command(WsOp.INCREMENT_COUNTER_ENTRY, incId, cacheId, "hits", null, 0, 3);
        var incResponse = awaitCommandResponse(incId);

        // Assert
        assertThat(createResponse.get("status").asText(), is("SUCCESS"));
        assertThat(addResponse.get("status").asText(), is("SUCCESS"));
        assertThat(incResponse.get("status").asText(), is("SUCCESS"));
        assertThat(incResponse.get("value"), notNullValue());
    }

    @Test
    @DisplayName("Should return an error frame for a malformed frame")
    protected void shouldReturnErrorForMalformedFrame() {
        // Arrange
        var malformedFrame = "{ this is not valid json";

        // Act
        helper.send(malformedFrame);

        // Assert
        await().atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> helper.frames().stream().anyMatch(f -> "error".equals(f.path("type").asText(null))));
    }

    private JsonNode awaitCommandResponse(String correlationId) {
        await().atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> helper.firstFrame(correlationId, "commandResponse").isPresent());
        var response = helper.firstFrame(correlationId, "commandResponse").orElseThrow();
        assertThat(response, notNullValue());
        return response;
    }
}
