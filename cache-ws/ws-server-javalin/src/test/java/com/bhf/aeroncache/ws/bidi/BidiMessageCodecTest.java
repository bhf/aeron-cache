package com.bhf.aeroncache.ws.bidi;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.models.CacheRequestMessageTypes;
import com.bhf.aeroncache.models.requests.SubscriptionMode;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.ws.bidi.messages.BidiCommand;
import com.bhf.aeroncache.ws.bidi.messages.BidiCommandResponse;
import com.bhf.aeroncache.ws.bidi.messages.BidiEntries;
import com.bhf.aeroncache.ws.bidi.messages.BidiError;
import com.bhf.aeroncache.ws.bidi.messages.BidiStats;
import com.bhf.aeroncache.ws.bidi.messages.BidiStreamUpdate;
import com.bhf.aeroncache.ws.bidi.messages.BidiSubscribe;
import com.bhf.aeroncache.ws.bidi.messages.BidiUnsubscribe;
import com.bhf.aeroncache.ws.bidi.messages.WsOp;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the bidirectional websocket JSON codec: every inbound frame decodes to the right
 * record, every outbound frame serializes to the expected JSON shape, and malformed input is rejected.
 * Modelled on the gateway's {@code GatewayRequestWriterTest}/{@code GatewayResponseWriterTest}.
 */
class BidiMessageCodecTest {

    private final BidiMessageCodec codec = new BidiMessageCodec();
    private final ObjectMapper mapper = new ObjectMapper();

    // ------------------------------------------------------------------ inbound

    @Test
    void shouldParseCommandFrame() {
        // Arrange
        var json = """
                {"type":"command","op":"ADD_CACHE_ENTRY","correlationId":"c1",
                 "cacheId":"7","key":"k","value":"v","ttl":5000}""";

        // Act
        var command = assertInstanceOf(BidiCommand.class, codec.parse(json));

        // Assert
        assertEquals("c1", command.correlationId());
        assertEquals(WsOp.ADD_CACHE_ENTRY, command.op());
        assertEquals("7", command.cacheId());
        assertEquals("k", command.key());
        assertEquals("v", command.value());
        assertEquals(5000L, command.ttl());
        assertEquals(0L, command.counterValue());
    }

    @Test
    void shouldParseCounterCommandFrame() {
        // Arrange
        var json = """
                {"type":"command","op":"INCREMENT_COUNTER_ENTRY","correlationId":"c2",
                 "cacheId":"counters","key":"hits","counterValue":3}""";

        // Act
        var command = assertInstanceOf(BidiCommand.class, codec.parse(json));

        // Assert
        assertEquals(WsOp.INCREMENT_COUNTER_ENTRY, command.op());
        assertTrue(command.op().counters());
        assertEquals(3L, command.counterValue());
    }

    @Test
    void shouldParseSubscribeFrame() {
        // Arrange
        var json = """
                {"type":"subscribe","correlationId":"s1","counters":false,"sendSnapshot":true,
                 "caches":[{"cacheId":"7","key":null,"mode":"FULL"},
                           {"cacheId":"8","key":"k","mode":"PATCH"}]}""";

        // Act
        var subscribe = assertInstanceOf(BidiSubscribe.class, codec.parse(json));

        // Assert
        assertEquals("s1", subscribe.correlationId());
        assertTrue(subscribe.sendSnapshot());
        assertEquals(2, subscribe.caches().size());
        assertEquals("7", subscribe.caches().get(0).cacheId());
        assertNull(subscribe.caches().get(0).key());
        assertEquals(SubscriptionMode.FULL, subscribe.caches().get(0).mode());
        assertEquals(SubscriptionMode.PATCH, subscribe.caches().get(1).mode());
        assertEquals("k", subscribe.caches().get(1).key());
    }

    @Test
    void shouldParseUnsubscribeFrame() {
        // Arrange
        var json = """
                {"type":"unsubscribe","correlationId":"u1","counters":true,"cacheId":"9"}""";

        // Act
        var unsubscribe = assertInstanceOf(BidiUnsubscribe.class, codec.parse(json));

        // Assert
        assertEquals("u1", unsubscribe.correlationId());
        assertTrue(unsubscribe.counters());
        assertEquals("9", unsubscribe.cacheId());
    }

    @Test
    void shouldRejectMalformedJson() {
        // Arrange
        var json = "{not json";

        // Act / Assert
        assertThrows(BidiProtocolException.class, () -> codec.parse(json));
    }

    @Test
    void shouldRejectMissingType() {
        // Arrange
        var json = "{\"correlationId\":\"c1\"}";

        // Act / Assert
        assertThrows(BidiProtocolException.class, () -> codec.parse(json));
    }

    @Test
    void shouldRejectUnknownType() {
        // Arrange
        var json = "{\"type\":\"nonsense\",\"correlationId\":\"c1\"}";

        // Act / Assert
        assertThrows(BidiProtocolException.class, () -> codec.parse(json));
    }

    @Test
    void shouldRejectUnknownOp() {
        // Arrange
        var json = "{\"type\":\"command\",\"op\":\"FLY_TO_THE_MOON\",\"correlationId\":\"c1\"}";

        // Act / Assert
        assertThrows(BidiProtocolException.class, () -> codec.parse(json));
    }

    // ------------------------------------------------------------------ outbound

    @Test
    void shouldWriteCommandResponse() throws Exception {
        // Arrange
        var frame = BidiCommandResponse.of("c1", CacheOperationStatus.SUCCESS, "7", "k", "v");

        // Act
        var node = mapper.readTree(codec.write(frame));

        // Assert
        assertEquals("commandResponse", node.get("type").asText());
        assertEquals("c1", node.get("correlationId").asText());
        assertEquals("SUCCESS", node.get("status").asText());
        assertEquals("7", node.get("cacheId").asText());
        assertEquals("k", node.get("key").asText());
        assertEquals("v", node.get("value").asText());
    }

    @Test
    void shouldDefaultNullStatusToNoneOnCommandResponse() throws Exception {
        // Arrange
        var frame = BidiCommandResponse.of("c1", null, "7", null, null);

        // Act
        var node = mapper.readTree(codec.write(frame));

        // Assert
        assertEquals("NONE", node.get("status").asText());
        assertTrue(node.get("key").isNull());
        assertTrue(node.get("value").isNull());
    }

    @Test
    void shouldWriteStreamUpdateFromCacheUpdateEvent() throws Exception {
        // Arrange
        var event = new CacheUpdateEvent<>("7", CacheUpdateEvent.EventType.ADD_ITEM, "k", "v", "sub1");

        // Act
        var node = mapper.readTree(codec.write(BidiStreamUpdate.from(event)));

        // Assert
        assertEquals("streamUpdate", node.get("type").asText());
        assertEquals("sub1", node.get("correlationId").asText());
        assertEquals("ADD_ITEM", node.get("eventType").asText());
        assertEquals("7", node.get("cacheId").asText());
        assertEquals("k", node.get("key").asText());
        assertEquals("v", node.get("value").asText());
    }

    @Test
    void shouldWriteError() throws Exception {
        // Arrange
        var frame = BidiError.of("c1", CacheOperationStatus.UNKNOWN_CACHE, "no such cache");

        // Act
        var node = mapper.readTree(codec.write(frame));

        // Assert
        assertEquals("error", node.get("type").asText());
        assertEquals("c1", node.get("correlationId").asText());
        assertEquals("UNKNOWN_CACHE", node.get("status").asText());
        assertEquals("no such cache", node.get("message").asText());
    }

    @Test
    void shouldWriteEntriesBatch() throws Exception {
        // Arrange
        Map<String, String> items = new LinkedHashMap<>();
        items.put("a", "1");
        items.put("b", "2");
        var frame = BidiEntries.of("c4", CacheOperationStatus.SUCCESS, "7", items, true);

        // Act
        var node = mapper.readTree(codec.write(frame));

        // Assert
        assertEquals("entries", node.get("type").asText());
        assertEquals("c4", node.get("correlationId").asText());
        assertEquals("7", node.get("cacheId").asText());
        assertTrue(node.get("endOfBatch").asBoolean());
        assertEquals("1", node.get("items").get("a").asText());
        assertEquals("2", node.get("items").get("b").asText());
    }

    @Test
    void shouldWriteStatsBatch() throws Exception {
        // Arrange
        var stats = List.of(new BidiStats.StatEntry("7", 10, 3, 1, 7));
        var frame = BidiStats.of("c5", CacheOperationStatus.SUCCESS, stats, true);

        // Act
        var node = mapper.readTree(codec.write(frame));

        // Assert
        assertEquals("stats", node.get("type").asText());
        assertEquals("c5", node.get("correlationId").asText());
        assertTrue(node.get("endOfBatch").asBoolean());
        var stat = node.get("stats").get(0);
        assertEquals("7", stat.get("cacheId").asText());
        assertEquals(10, stat.get("addedCount").asLong());
        assertEquals(3, stat.get("removedCount").asLong());
        assertEquals(1, stat.get("clearedCount").asLong());
        assertEquals(7, stat.get("size").asLong());
    }

    // ------------------------------------------------------------------ vocabulary

    @Test
    void wsOpShouldMapOntoCacheRequestMessageTypes() {
        // Arrange / Act / Assert
        assertEquals(CacheRequestMessageTypes.ADD_CACHE_ENTRY_MSG_ID, WsOp.ADD_CACHE_ENTRY.msgType());
        assertEquals(CacheRequestMessageTypes.SET_COUNTER_ENTRY_MSG_ID, WsOp.SET_COUNTER_ENTRY.msgType());
        assertTrue(WsOp.GET_COUNTER_STATS.counters());
        assertTrue(WsOp.CREATE_CACHE.msgType() < CacheRequestMessageTypes.CREATE_COUNTER_CACHE_MSG_ID);
    }
}
