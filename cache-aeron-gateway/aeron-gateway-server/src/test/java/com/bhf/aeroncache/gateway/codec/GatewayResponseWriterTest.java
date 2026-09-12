package com.bhf.aeroncache.gateway.codec;

import com.bhf.aeroncache.gateway.messages.BooleanType;
import com.bhf.aeroncache.gateway.messages.GatewayCommandResponseDecoder;
import com.bhf.aeroncache.gateway.messages.GatewayEntriesDecoder;
import com.bhf.aeroncache.gateway.messages.GatewayErrorDecoder;
import com.bhf.aeroncache.gateway.messages.GatewayStatsDecoder;
import com.bhf.aeroncache.gateway.messages.GatewaySubscribeAckDecoder;
import com.bhf.aeroncache.gateway.messages.MessageHeaderDecoder;
import com.bhf.aeroncache.gateway.messages.OperationStatus;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import io.aeron.Publication;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that frames produced by {@link GatewayResponseWriter} decode correctly with the
 * generated SBE decoders, with particular focus on the batch/end-of-batch streaming contract
 * used for {@code getEntries}/{@code getStats}.
 */
class GatewayResponseWriterTest {

    private GatewayResponseWriter writer;
    private Publication publication;
    private byte[] captured;

    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();

    @BeforeEach
    void setUp() {
        writer = new GatewayResponseWriter();
        captured = null;
        publication = mock(Publication.class);
        when(publication.isConnected()).thenReturn(true);
        when(publication.offer(any(DirectBuffer.class), anyInt(), anyInt())).thenAnswer(inv -> {
            DirectBuffer buf = inv.getArgument(0);
            int offset = inv.getArgument(1);
            int length = inv.getArgument(2);
            captured = new byte[length];
            buf.getBytes(offset, captured, 0, length);
            return (long) length;
        });
    }

    private UnsafeBuffer lastFrame() {
        assertTrue(captured != null && captured.length > 0, "no frame was offered");
        return new UnsafeBuffer(captured);
    }

    private int wrapHeader(UnsafeBuffer buf) {
        headerDecoder.wrap(buf, 0);
        return headerDecoder.encodedLength();
    }

    @Test
    @DisplayName("Should encode an entries batch with items and end-of-batch flag that round-trips through the decoder")
    void shouldEncodeEntriesBatchWithItemsAndEndOfBatch() {
        // Arrange
        Map<String, String> items = new LinkedHashMap<>();
        items.put("k1", "v1");
        items.put("k2", "v2");

        // Act
        writer.writeEntries(publication, "corr-1", CacheOperationStatus.SUCCESS, "cacheA", items, true);

        // Assert
        UnsafeBuffer buf = lastFrame();
        int offset = wrapHeader(buf);
        assertEquals(GatewayEntriesDecoder.TEMPLATE_ID, headerDecoder.templateId());

        GatewayEntriesDecoder decoder = new GatewayEntriesDecoder();
        decoder.wrap(buf, offset, headerDecoder.blockLength(), headerDecoder.version());

        assertEquals(OperationStatus.SUCCESS, decoder.status());
        assertEquals(BooleanType.T, decoder.endOfBatch());

        GatewayEntriesDecoder.ItemsDecoder itemsDecoder = decoder.items();
        assertEquals(2, itemsDecoder.count());

        Map<String, String> decoded = new HashMap<>();
        while (itemsDecoder.hasNext()) {
            itemsDecoder.next();
            decoded.put(itemsDecoder.key(), itemsDecoder.value());
        }
        assertEquals(items, decoded);

        assertEquals("corr-1", decoder.correlationId());
        assertEquals("cacheA", decoder.cacheId());
    }

    @Test
    @DisplayName("Should encode an empty terminal batch with end-of-batch set and no items")
    void shouldEncodeEmptyTerminalBatch() {
        // Arrange
        // (no items in the terminal batch)

        // Act
        writer.writeEntries(publication, "corr-2", CacheOperationStatus.SUCCESS, "cacheB", Map.of(), true);

        // Assert
        UnsafeBuffer buf = lastFrame();
        int offset = wrapHeader(buf);
        GatewayEntriesDecoder decoder = new GatewayEntriesDecoder();
        decoder.wrap(buf, offset, headerDecoder.blockLength(), headerDecoder.version());

        assertEquals(BooleanType.T, decoder.endOfBatch());
        assertEquals(0, decoder.items().count());
        assertEquals("corr-2", decoder.correlationId());
        assertEquals("cacheB", decoder.cacheId());
    }

    @Test
    @DisplayName("Should preserve end-of-batch=false for a non-terminal batch")
    void shouldPreserveEndOfBatchFalseForNonTerminalBatch() {
        // Arrange
        Map<String, String> items = Map.of("only", "one");

        // Act
        writer.writeEntries(publication, "corr-3", CacheOperationStatus.SUCCESS, "cacheC", items, false);

        // Assert
        UnsafeBuffer buf = lastFrame();
        int offset = wrapHeader(buf);
        GatewayEntriesDecoder decoder = new GatewayEntriesDecoder();
        decoder.wrap(buf, offset, headerDecoder.blockLength(), headerDecoder.version());

        assertEquals(BooleanType.F, decoder.endOfBatch());
    }

    @Test
    @DisplayName("Should map a null entry value to an empty string when encoding")
    void shouldMapNullEntryValueToEmptyString() {
        // Arrange
        Map<String, String> items = new HashMap<>();
        items.put("nullVal", null);

        // Act
        writer.writeEntries(publication, "corr-4", CacheOperationStatus.SUCCESS, "cacheD", items, true);

        // Assert
        UnsafeBuffer buf = lastFrame();
        int offset = wrapHeader(buf);
        GatewayEntriesDecoder decoder = new GatewayEntriesDecoder();
        decoder.wrap(buf, offset, headerDecoder.blockLength(), headerDecoder.version());

        GatewayEntriesDecoder.ItemsDecoder itemsDecoder = decoder.items();
        assertEquals(1, itemsDecoder.count());
        itemsDecoder.next();
        assertEquals("nullVal", itemsDecoder.key());
        assertEquals("", itemsDecoder.value());
    }

    @Test
    @DisplayName("Should carry independent end-of-batch flags across multiple streamed batches")
    void shouldCarryIndependentEndOfBatchFlagsAcrossBatches() {
        // Arrange
        Map<String, String> firstBatch = Map.of("a", "1");
        Map<String, String> finalBatch = Map.of("b", "2");

        // Act
        writer.writeEntries(publication, "corr-5", CacheOperationStatus.SUCCESS, "cacheE", firstBatch, false);
        BooleanType firstEndOfBatch = decodeEndOfBatch(lastFrame());
        writer.writeEntries(publication, "corr-5", CacheOperationStatus.SUCCESS, "cacheE", finalBatch, true);
        BooleanType finalEndOfBatch = decodeEndOfBatch(lastFrame());

        // Assert
        assertEquals(BooleanType.F, firstEndOfBatch);
        assertEquals(BooleanType.T, finalEndOfBatch);
    }

    private BooleanType decodeEndOfBatch(UnsafeBuffer buf) {
        int offset = wrapHeader(buf);
        GatewayEntriesDecoder decoder = new GatewayEntriesDecoder();
        decoder.wrap(buf, offset, headerDecoder.blockLength(), headerDecoder.version());
        decoder.status();
        return decoder.endOfBatch();
    }

    @Test
    @DisplayName("Should encode a stats group and correlation id that round-trip through the decoder")
    void shouldEncodeStatsGroupAndCorrelationId() {
        // Arrange
        List<GatewayResponseWriter.StatEntry> stats = List.of(
                new GatewayResponseWriter.StatEntry("cacheA", 10L, 2L, 1L, 8L),
                new GatewayResponseWriter.StatEntry("cacheB", 5L, 0L, 0L, 5L));

        // Act
        writer.writeStats(publication, "corr-stats", CacheOperationStatus.SUCCESS, stats, true);

        // Assert
        UnsafeBuffer buf = lastFrame();
        int offset = wrapHeader(buf);
        assertEquals(GatewayStatsDecoder.TEMPLATE_ID, headerDecoder.templateId());

        GatewayStatsDecoder decoder = new GatewayStatsDecoder();
        decoder.wrap(buf, offset, headerDecoder.blockLength(), headerDecoder.version());

        assertEquals(OperationStatus.SUCCESS, decoder.status());
        assertEquals(BooleanType.T, decoder.endOfBatch());

        GatewayStatsDecoder.StatsDecoder statsDecoder = decoder.stats();
        assertEquals(2, statsDecoder.count());

        statsDecoder.next();
        assertEquals(10L, statsDecoder.addedCount());
        assertEquals(2L, statsDecoder.removedCount());
        assertEquals(1L, statsDecoder.clearedCount());
        assertEquals(8L, statsDecoder.size());
        assertEquals("cacheA", statsDecoder.cacheId());

        statsDecoder.next();
        assertEquals(5L, statsDecoder.addedCount());
        assertEquals(0L, statsDecoder.removedCount());
        assertEquals(0L, statsDecoder.clearedCount());
        assertEquals(5L, statsDecoder.size());
        assertEquals("cacheB", statsDecoder.cacheId());

        assertEquals("corr-stats", decoder.correlationId());
    }

    @Test
    @DisplayName("Should encode a command response that round-trips through the decoder")
    void shouldEncodeCommandResponseThatRoundTrips() {
        // Arrange
        // (inputs supplied directly to the writer)

        // Act
        writer.writeCommandResponse(publication, "corr-cmd", CacheOperationStatus.UNKNOWN_CACHE,
                "cacheX", "key1", "value1");

        // Assert
        UnsafeBuffer buf = lastFrame();
        int offset = wrapHeader(buf);
        assertEquals(GatewayCommandResponseDecoder.TEMPLATE_ID, headerDecoder.templateId());

        GatewayCommandResponseDecoder decoder = new GatewayCommandResponseDecoder();
        decoder.wrap(buf, offset, headerDecoder.blockLength(), headerDecoder.version());

        assertEquals(OperationStatus.UNKNOWN_CACHE, decoder.status());
        assertEquals("corr-cmd", decoder.correlationId());
        assertEquals("cacheX", decoder.cacheId());
        assertEquals("key1", decoder.key());
        assertEquals("value1", decoder.value());
    }

    @Test
    @DisplayName("Should encode an error frame that round-trips through the decoder")
    void shouldEncodeErrorFrameThatRoundTrips() {
        // Arrange
        // (inputs supplied directly to the writer)

        // Act
        writer.writeError(publication, "corr-err", CacheOperationStatus.ERROR, "boom");

        // Assert
        UnsafeBuffer buf = lastFrame();
        int offset = wrapHeader(buf);
        assertEquals(GatewayErrorDecoder.TEMPLATE_ID, headerDecoder.templateId());

        GatewayErrorDecoder decoder = new GatewayErrorDecoder();
        decoder.wrap(buf, offset, headerDecoder.blockLength(), headerDecoder.version());

        assertEquals(OperationStatus.ERROR, decoder.status());
        assertEquals("corr-err", decoder.correlationId());
        assertEquals("boom", decoder.message());
    }

    @Test
    @DisplayName("Should encode a subscribe ack that round-trips through the decoder")
    void shouldEncodeSubscribeAckThatRoundTrips() {
        // Arrange
        // (inputs supplied directly to the writer)

        // Act
        writer.writeSubscribeAck(publication, "corr-sub", CacheOperationStatus.SUCCESS, List.of("cacheA", "cacheB"));

        // Assert
        UnsafeBuffer buf = lastFrame();
        int offset = wrapHeader(buf);
        assertEquals(GatewaySubscribeAckDecoder.TEMPLATE_ID, headerDecoder.templateId());

        GatewaySubscribeAckDecoder decoder = new GatewaySubscribeAckDecoder();
        decoder.wrap(buf, offset, headerDecoder.blockLength(), headerDecoder.version());

        assertEquals(OperationStatus.SUCCESS, decoder.status());
        List<String> cacheIds = new java.util.ArrayList<>();
        for (GatewaySubscribeAckDecoder.CacheIdsDecoder id : decoder.cacheIds()) {
            cacheIds.add(id.cacheId());
        }
        assertEquals(List.of("cacheA", "cacheB"), cacheIds);
        assertEquals("corr-sub", decoder.correlationId());
    }

    @Test
    @DisplayName("Should drop the frame when the response publication is not connected")
    void shouldDropFrameWhenPublicationNotConnected() {
        // Arrange
        Publication disconnected = mock(Publication.class);
        when(disconnected.isConnected()).thenReturn(false);

        // Act
        writer.writeError(disconnected, "corr", CacheOperationStatus.ERROR, "msg");

        // Assert
        assertFalse(captured != null && captured.length > 0, "frame should not have been offered");
    }
}
