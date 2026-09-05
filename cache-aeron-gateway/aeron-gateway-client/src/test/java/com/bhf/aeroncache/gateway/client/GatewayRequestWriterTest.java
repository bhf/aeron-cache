package com.bhf.aeroncache.gateway.client;

import com.bhf.aeroncache.gateway.messages.BooleanType;
import com.bhf.aeroncache.gateway.messages.GatewayCommandDecoder;
import com.bhf.aeroncache.gateway.messages.GatewaySubscribeDecoder;
import com.bhf.aeroncache.gateway.messages.GatewayUnsubscribeDecoder;
import com.bhf.aeroncache.gateway.messages.MessageHeaderDecoder;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that request frames produced by {@link GatewayRequestWriter} decode correctly with the
 * generated SBE decoders (the same decoders the gateway server uses to read client requests).
 */
class GatewayRequestWriterTest {

    private final GatewayRequestWriter writer = new GatewayRequestWriter();
    private final MutableDirectBuffer buffer = new ExpandableArrayBuffer(1024);
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();

    private int wrapHeader() {
        headerDecoder.wrap(buffer, 0);
        return headerDecoder.encodedLength();
    }

    @Test
    @DisplayName("Should encode a command frame that round-trips through the decoder")
    void shouldEncodeCommandThatRoundTrips() {
        // Arrange
        // (inputs supplied directly to the writer)

        // Act
        int length = writer.encodeCommand(buffer, 2, 5000L, 42L, "corr-1", "cacheA", "key1", "value1");

        // Assert
        int offset = wrapHeader();
        assertEquals(GatewayCommandDecoder.TEMPLATE_ID, headerDecoder.templateId());

        GatewayCommandDecoder decoder = new GatewayCommandDecoder();
        decoder.wrap(buffer, offset, headerDecoder.blockLength(), headerDecoder.version());

        assertEquals(2, decoder.msgType());
        assertEquals(5000L, decoder.ttl());
        assertEquals(42L, decoder.counterValue());
        assertEquals("corr-1", decoder.correlationId());
        assertEquals("cacheA", decoder.cacheId());
        assertEquals("key1", decoder.key());
        assertEquals("value1", decoder.value());

        assertEquals(decoder.limit(), length);
    }

    @Test
    @DisplayName("Should map null command strings to empty strings when encoding")
    void shouldMapNullCommandStringsToEmpty() {
        // Arrange
        // (null key and value supplied to the writer)

        // Act
        writer.encodeCommand(buffer, 1, 0L, 0L, "corr-2", "cacheB", null, null);

        // Assert
        int offset = wrapHeader();
        GatewayCommandDecoder decoder = new GatewayCommandDecoder();
        decoder.wrap(buffer, offset, headerDecoder.blockLength(), headerDecoder.version());

        assertEquals("corr-2", decoder.correlationId());
        assertEquals("cacheB", decoder.cacheId());
        assertEquals("", decoder.key());
        assertEquals("", decoder.value());
    }

    @Test
    @DisplayName("Should encode a subscribe frame with a cache id group that round-trips through the decoder")
    void shouldEncodeSubscribeWithCacheIdGroup() {
        // Arrange
        List<String> cacheIds = List.of("cacheA", "cacheB", "cacheC");

        // Act
        writer.encodeSubscribe(buffer, "corr-sub", cacheIds, true, false);

        // Assert
        int offset = wrapHeader();
        assertEquals(GatewaySubscribeDecoder.TEMPLATE_ID, headerDecoder.templateId());

        GatewaySubscribeDecoder decoder = new GatewaySubscribeDecoder();
        decoder.wrap(buffer, offset, headerDecoder.blockLength(), headerDecoder.version());

        assertEquals(BooleanType.T, decoder.sendSnapshot());
        assertEquals(BooleanType.F, decoder.counters());

        GatewaySubscribeDecoder.CacheIdsDecoder group = decoder.cacheIds();
        assertEquals(3, group.count());

        List<String> decodedIds = new ArrayList<>();
        while (group.hasNext()) {
            group.next();
            decodedIds.add(group.cacheId());
        }
        assertEquals(cacheIds, decodedIds);

        assertEquals("corr-sub", decoder.correlationId());
    }

    @Test
    @DisplayName("Should encode a subscribe frame with an empty cache id group")
    void shouldEncodeSubscribeWithEmptyCacheIdGroup() {
        // Arrange
        // (empty cache id list supplied to the writer)

        // Act
        writer.encodeSubscribe(buffer, "corr-empty", List.of(), false, true);

        // Assert
        int offset = wrapHeader();
        GatewaySubscribeDecoder decoder = new GatewaySubscribeDecoder();
        decoder.wrap(buffer, offset, headerDecoder.blockLength(), headerDecoder.version());

        assertEquals(BooleanType.F, decoder.sendSnapshot());
        assertEquals(BooleanType.T, decoder.counters());
        assertEquals(0, decoder.cacheIds().count());
        assertEquals("corr-empty", decoder.correlationId());
    }

    @Test
    @DisplayName("Should encode an unsubscribe frame that round-trips through the decoder")
    void shouldEncodeUnsubscribeThatRoundTrips() {
        // Arrange
        // (inputs supplied directly to the writer)

        // Act
        writer.encodeUnsubscribe(buffer, "corr-unsub", "cacheZ", true);

        // Assert
        int offset = wrapHeader();
        assertEquals(GatewayUnsubscribeDecoder.TEMPLATE_ID, headerDecoder.templateId());

        GatewayUnsubscribeDecoder decoder = new GatewayUnsubscribeDecoder();
        decoder.wrap(buffer, offset, headerDecoder.blockLength(), headerDecoder.version());

        assertEquals(BooleanType.T, decoder.counters());
        assertEquals("corr-unsub", decoder.correlationId());
        assertEquals("cacheZ", decoder.cacheId());
    }
}
