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

    @Test
    @DisplayName("Should patch an entry, deep-merging the value, over the BIDI socket")
    protected void shouldPatchEntryOverSocket() {
        // Arrange
        var cacheId = cacheIdPrefix() + "-patch";
        var createId = helper.newCorrelationId();
        var addId = helper.newCorrelationId();
        var patchId = helper.newCorrelationId();
        var getId = helper.newCorrelationId();

        // Act
        helper.command(WsOp.CREATE_CACHE, createId, cacheId, null, null, 0, 0);
        awaitCommandResponse(createId);
        helper.command(WsOp.ADD_CACHE_ENTRY, addId, cacheId, "k1", "{\"a\":1,\"b\":2}", 0, 0);
        awaitCommandResponse(addId);
        helper.command(WsOp.PATCH_CACHE_ENTRY, patchId, cacheId, "k1", "{\"b\":3,\"c\":4}", 0, 0);
        var patchResponse = awaitCommandResponse(patchId);
        helper.command(WsOp.GET_CACHE_ENTRY, getId, cacheId, "k1", null, 0, 0);
        var getResponse = awaitCommandResponse(getId);

        // Assert: the patch merged into (not replaced) the stored value.
        assertThat(patchResponse.get("status").asText(), is("SUCCESS"));
        assertThat(getResponse.get("status").asText(), is("SUCCESS"));
        assertThat(getResponse.get("value").asText(), is("{\"a\":1,\"b\":3,\"c\":4}"));
    }

    @Test
    @DisplayName("Should remove an entry over the BIDI socket so it reads back as UNKNOWN_KEY")
    protected void shouldRemoveEntryOverSocket() {
        // Arrange
        var cacheId = cacheIdPrefix() + "-remove";
        var createId = helper.newCorrelationId();
        var addId = helper.newCorrelationId();
        var removeId = helper.newCorrelationId();
        var getId = helper.newCorrelationId();

        // Act
        helper.command(WsOp.CREATE_CACHE, createId, cacheId, null, null, 0, 0);
        awaitCommandResponse(createId);
        helper.command(WsOp.ADD_CACHE_ENTRY, addId, cacheId, "k1", "v1", 0, 0);
        awaitCommandResponse(addId);
        helper.command(WsOp.REMOVE_CACHE_ENTRY, removeId, cacheId, "k1", null, 0, 0);
        var removeResponse = awaitCommandResponse(removeId);
        helper.command(WsOp.GET_CACHE_ENTRY, getId, cacheId, "k1", null, 0, 0);
        var getResponse = awaitCommandResponse(getId);

        // Assert
        assertThat(removeResponse.get("status").asText(), is("SUCCESS"));
        assertThat(getResponse.get("status").asText(), is("UNKNOWN_KEY"));
    }

    @Test
    @DisplayName("Should clear a cache over the BIDI socket, leaving no entries")
    protected void shouldClearCacheOverSocket() {
        // Arrange
        var cacheId = cacheIdPrefix() + "-clear";
        var createId = helper.newCorrelationId();
        var addId = helper.newCorrelationId();
        var clearId = helper.newCorrelationId();
        var entriesId = helper.newCorrelationId();

        // Act
        helper.command(WsOp.CREATE_CACHE, createId, cacheId, null, null, 0, 0);
        awaitCommandResponse(createId);
        helper.command(WsOp.ADD_CACHE_ENTRY, addId, cacheId, "k1", "v1", 0, 0);
        awaitCommandResponse(addId);
        helper.command(WsOp.CLEAR_CACHE, clearId, cacheId, null, null, 0, 0);
        var clearResponse = awaitCommandResponse(clearId);
        helper.command(WsOp.GET_CACHE_ENTRIES, entriesId, cacheId, null, null, 0, 0);
        var entriesFrame = awaitEndOfBatchEntries(entriesId);

        // Assert
        assertThat(clearResponse.get("status").asText(), is("SUCCESS"));
        assertThat(entriesFrame.get("items").isEmpty(), is(true));
    }

    @Test
    @DisplayName("Should delete a cache over the BIDI socket")
    protected void shouldDeleteCacheOverSocket() {
        // Arrange
        var cacheId = cacheIdPrefix() + "-delete";
        var createId = helper.newCorrelationId();
        var addId = helper.newCorrelationId();
        var deleteId = helper.newCorrelationId();

        // Act
        helper.command(WsOp.CREATE_CACHE, createId, cacheId, null, null, 0, 0);
        awaitCommandResponse(createId);
        helper.command(WsOp.ADD_CACHE_ENTRY, addId, cacheId, "k1", "v1", 0, 0);
        awaitCommandResponse(addId);
        helper.command(WsOp.DELETE_CACHE, deleteId, cacheId, null, null, 0, 0);
        var deleteResponse = awaitCommandResponse(deleteId);

        // Assert
        assertThat(deleteResponse.get("status").asText(), is("SUCCESS"));
    }

    @Test
    @DisplayName("Should schedule a TTL removal and cancel it over the BIDI socket, keeping the entry")
    protected void shouldCancelScheduledRemovalOverSocket() {
        // Arrange
        var cacheId = cacheIdPrefix() + "-cancel-removal";
        var createId = helper.newCorrelationId();
        var addId = helper.newCorrelationId();
        var cancelId = helper.newCorrelationId();
        var getId = helper.newCorrelationId();

        // Act: add with a (long) scheduled removal, then cancel the pending timer.
        helper.command(WsOp.CREATE_CACHE, createId, cacheId, null, null, 0, 0);
        awaitCommandResponse(createId);
        helper.command(WsOp.ADD_CACHE_ENTRY, addId, cacheId, "k1", "v1", 60_000, 0);
        awaitCommandResponse(addId);
        helper.command(WsOp.CANCEL_CACHE_ITEM_REMOVAL, cancelId, cacheId, "k1", null, 0, 0);
        var cancelResponse = awaitCommandResponse(cancelId);
        helper.command(WsOp.GET_CACHE_ENTRY, getId, cacheId, "k1", null, 0, 0);
        var getResponse = awaitCommandResponse(getId);

        // Assert: the cancel succeeded (a removal was pending) and the entry survives.
        assertThat(cancelResponse.get("status").asText(), is("SUCCESS"));
        assertThat(getResponse.get("status").asText(), is("SUCCESS"));
        assertThat(getResponse.get("value").asText(), is("v1"));
    }

    @Test
    @DisplayName("Should read a counter entry back over the BIDI socket")
    protected void shouldGetCounterEntryOverSocket() {
        // Arrange
        var cacheId = cacheIdPrefix() + "-counter-get";
        var createId = helper.newCorrelationId();
        var addId = helper.newCorrelationId();
        var getId = helper.newCorrelationId();

        // Act
        helper.command(WsOp.CREATE_COUNTER_CACHE, createId, cacheId, null, null, 0, 0);
        awaitCommandResponse(createId);
        helper.command(WsOp.ADD_COUNTER_ENTRY, addId, cacheId, "hits", null, 0, 7);
        awaitCommandResponse(addId);
        helper.command(WsOp.GET_COUNTER_ENTRY, getId, cacheId, "hits", null, 0, 0);
        var getResponse = awaitCommandResponse(getId);

        // Assert
        assertThat(getResponse.get("status").asText(), is("SUCCESS"));
        assertThat(getResponse.get("value").asText(), is("7"));
    }

    @Test
    @DisplayName("Should decrement a counter over the BIDI socket")
    protected void shouldDecrementCounterOverSocket() {
        // Arrange
        var cacheId = cacheIdPrefix() + "-counter-dec";
        var createId = helper.newCorrelationId();
        var addId = helper.newCorrelationId();
        var decId = helper.newCorrelationId();

        // Act
        helper.command(WsOp.CREATE_COUNTER_CACHE, createId, cacheId, null, null, 0, 0);
        awaitCommandResponse(createId);
        helper.command(WsOp.ADD_COUNTER_ENTRY, addId, cacheId, "gauge", null, 0, 10);
        awaitCommandResponse(addId);
        helper.command(WsOp.DECREMENT_COUNTER_ENTRY, decId, cacheId, "gauge", null, 0, 4);
        var decResponse = awaitCommandResponse(decId);

        // Assert
        assertThat(decResponse.get("status").asText(), is("SUCCESS"));
        assertThat(decResponse.get("value").asText(), is("6"));
    }

    @Test
    @DisplayName("Should set a counter to an absolute value over the BIDI socket")
    protected void shouldSetCounterOverSocket() {
        // Arrange
        var cacheId = cacheIdPrefix() + "-counter-set";
        var createId = helper.newCorrelationId();
        var addId = helper.newCorrelationId();
        var setId = helper.newCorrelationId();

        // Act
        helper.command(WsOp.CREATE_COUNTER_CACHE, createId, cacheId, null, null, 0, 0);
        awaitCommandResponse(createId);
        helper.command(WsOp.ADD_COUNTER_ENTRY, addId, cacheId, "gauge", null, 0, 1);
        awaitCommandResponse(addId);
        helper.command(WsOp.SET_COUNTER_ENTRY, setId, cacheId, "gauge", null, 0, 42);
        var setResponse = awaitCommandResponse(setId);

        // Assert
        assertThat(setResponse.get("status").asText(), is("SUCCESS"));
        assertThat(setResponse.get("value").asText(), is("42"));
    }

    @Test
    @DisplayName("Should stream all counter entries in an end-of-batch frame over the BIDI socket")
    protected void shouldStreamCounterEntriesOverSocket() {
        // Arrange
        var cacheId = cacheIdPrefix() + "-counter-entries";
        var createId = helper.newCorrelationId();
        var addA = helper.newCorrelationId();
        var addB = helper.newCorrelationId();
        var entriesId = helper.newCorrelationId();

        // Act
        helper.command(WsOp.CREATE_COUNTER_CACHE, createId, cacheId, null, null, 0, 0);
        awaitCommandResponse(createId);
        helper.command(WsOp.ADD_COUNTER_ENTRY, addA, cacheId, "a", null, 0, 1);
        awaitCommandResponse(addA);
        helper.command(WsOp.ADD_COUNTER_ENTRY, addB, cacheId, "b", null, 0, 2);
        awaitCommandResponse(addB);
        helper.command(WsOp.GET_COUNTER_ENTRIES, entriesId, cacheId, null, null, 0, 0);
        var entriesFrame = awaitEndOfBatchEntries(entriesId);

        // Assert
        assertThat(entriesFrame.get("items").get("a").asText(), is("1"));
        assertThat(entriesFrame.get("items").get("b").asText(), is("2"));
    }

    @Test
    @DisplayName("Should return counter cache stats in an end-of-batch frame over the BIDI socket")
    protected void shouldReturnCounterStatsOverSocket() {
        // Arrange
        var statsId = helper.newCorrelationId();

        // Act
        helper.command(WsOp.GET_COUNTER_STATS, statsId, null, null, null, 0, 0);

        // Assert
        await().atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> helper.firstFrame(statsId, "stats")
                        .filter(f -> f.get("endOfBatch").asBoolean())
                        .isPresent());
    }

    @Test
    @DisplayName("Should remove a counter entry over the BIDI socket so it reads back as UNKNOWN_KEY")
    protected void shouldRemoveCounterEntryOverSocket() {
        // Arrange
        var cacheId = cacheIdPrefix() + "-counter-remove";
        var createId = helper.newCorrelationId();
        var addId = helper.newCorrelationId();
        var removeId = helper.newCorrelationId();
        var getId = helper.newCorrelationId();

        // Act
        helper.command(WsOp.CREATE_COUNTER_CACHE, createId, cacheId, null, null, 0, 0);
        awaitCommandResponse(createId);
        helper.command(WsOp.ADD_COUNTER_ENTRY, addId, cacheId, "hits", null, 0, 7);
        awaitCommandResponse(addId);
        helper.command(WsOp.REMOVE_COUNTER_ENTRY, removeId, cacheId, "hits", null, 0, 0);
        var removeResponse = awaitCommandResponse(removeId);
        helper.command(WsOp.GET_COUNTER_ENTRY, getId, cacheId, "hits", null, 0, 0);
        var getResponse = awaitCommandResponse(getId);

        // Assert
        assertThat(removeResponse.get("status").asText(), is("SUCCESS"));
        assertThat(getResponse.get("status").asText(), is("UNKNOWN_KEY"));
    }

    @Test
    @DisplayName("Should clear a counter cache over the BIDI socket, leaving no entries")
    protected void shouldClearCounterCacheOverSocket() {
        // Arrange
        var cacheId = cacheIdPrefix() + "-counter-clear";
        var createId = helper.newCorrelationId();
        var addId = helper.newCorrelationId();
        var clearId = helper.newCorrelationId();
        var entriesId = helper.newCorrelationId();

        // Act
        helper.command(WsOp.CREATE_COUNTER_CACHE, createId, cacheId, null, null, 0, 0);
        awaitCommandResponse(createId);
        helper.command(WsOp.ADD_COUNTER_ENTRY, addId, cacheId, "hits", null, 0, 3);
        awaitCommandResponse(addId);
        helper.command(WsOp.CLEAR_COUNTER_CACHE, clearId, cacheId, null, null, 0, 0);
        var clearResponse = awaitCommandResponse(clearId);
        helper.command(WsOp.GET_COUNTER_ENTRIES, entriesId, cacheId, null, null, 0, 0);
        var entriesFrame = awaitEndOfBatchEntries(entriesId);

        // Assert
        assertThat(clearResponse.get("status").asText(), is("SUCCESS"));
        assertThat(entriesFrame.get("items").isEmpty(), is(true));
    }

    @Test
    @DisplayName("Should delete a counter cache over the BIDI socket")
    protected void shouldDeleteCounterCacheOverSocket() {
        // Arrange
        var cacheId = cacheIdPrefix() + "-counter-delete";
        var createId = helper.newCorrelationId();
        var addId = helper.newCorrelationId();
        var deleteId = helper.newCorrelationId();

        // Act
        helper.command(WsOp.CREATE_COUNTER_CACHE, createId, cacheId, null, null, 0, 0);
        awaitCommandResponse(createId);
        helper.command(WsOp.ADD_COUNTER_ENTRY, addId, cacheId, "hits", null, 0, 3);
        awaitCommandResponse(addId);
        helper.command(WsOp.DELETE_COUNTER_CACHE, deleteId, cacheId, null, null, 0, 0);
        var deleteResponse = awaitCommandResponse(deleteId);

        // Assert
        assertThat(deleteResponse.get("status").asText(), is("SUCCESS"));
    }

    @Test
    @DisplayName("Should apply a mixed batch of cache and counter operations in one bulk frame")
    protected void shouldApplyMixedBulkOperationsOverSocket() {
        // Arrange: a single bulk frame mixing regular-cache and counter operations, each with its own
        // requestId (echoed on the matching response entry; results also come back in request order).
        var regCache = cacheIdPrefix() + "-bulk-reg";
        var cntCache = cacheIdPrefix() + "-bulk-cnt";
        var bulkId = helper.newCorrelationId();

        // Note: a bulk GET_COUNTER is intentionally not exercised here — reading a counter inside a bulk
        // batch hits a pre-existing cluster limitation (the long counter value cannot be marshalled into
        // the bulk result's string value), independent of the gateway. The counter's value is instead
        // confirmed with a normal GET_COUNTER_ENTRY command after the batch.
        var operations = java.util.List.of(
                helper.bulkOp("CREATE_CACHE", "op-create", regCache, null, null, 0, 0),
                helper.bulkOp("ADD_ITEM", "op-add", regCache, "k1", "v1", 0, 0),
                helper.bulkOp("GET_ITEM", "op-get", regCache, "k1", null, 0, 0),
                helper.bulkOp("CREATE_COUNTER_CACHE", "op-create-cnt", cntCache, null, null, 0, 0),
                helper.bulkOp("ADD_COUNTER", "op-add-cnt", cntCache, "hits", null, 0, 5),
                helper.bulkOp("INCREMENT_COUNTER", "op-inc-cnt", cntCache, "hits", null, 0, 3));

        // Act
        helper.bulk(bulkId, operations);

        // Assert: a single bulk response with one result per operation, in request order, all successful.
        await().atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> helper.firstFrame(bulkId, "bulkResponse").isPresent());
        var response = helper.firstFrame(bulkId, "bulkResponse").orElseThrow();
        var opResponses = response.get("operationResponses");
        assertThat(opResponses.size(), is(operations.size()));
        for (var opResponse : opResponses) {
            assertThat("bulk op " + opResponse.path("requestId").asText() + " did not succeed",
                    opResponse.get("status").asText(), is("SUCCESS"));
        }
        // Per-operation requestIds are echoed in order.
        assertThat(opResponses.get(0).get("requestId").asText(), is("op-create"));
        assertThat(opResponses.get(5).get("requestId").asText(), is("op-inc-cnt"));
        // The regular GET_ITEM read the value written earlier in the same batch.
        assertThat(opResponses.get(2).get("value").asText(), is("v1"));

        // The counter reflects the batch's add (5) then increment (+3), read back with a normal command.
        var getCounterId = helper.newCorrelationId();
        helper.command(WsOp.GET_COUNTER_ENTRY, getCounterId, cntCache, "hits", null, 0, 0);
        var getCounterResponse = awaitCommandResponse(getCounterId);
        assertThat(getCounterResponse.get("value").asText(), is("8"));
    }

    private JsonNode awaitCommandResponse(String correlationId) {
        await().atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> helper.firstFrame(correlationId, "commandResponse").isPresent());
        var response = helper.firstFrame(correlationId, "commandResponse").orElseThrow();
        assertThat(response, notNullValue());
        return response;
    }

    private JsonNode awaitEndOfBatchEntries(String correlationId) {
        await().atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> helper.firstFrame(correlationId, "entries")
                        .filter(f -> f.get("endOfBatch").asBoolean())
                        .isPresent());
        return helper.firstFrame(correlationId, "entries").orElseThrow();
    }
}
