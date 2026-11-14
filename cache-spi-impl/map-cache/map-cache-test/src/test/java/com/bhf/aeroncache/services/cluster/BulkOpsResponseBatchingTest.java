package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseEncoder;
import com.bhf.aeroncache.models.consumer.HydratingPublicationConsumer;
import com.bhf.aeroncache.models.results.BulkCacheOpsResult;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that a bulk operations response larger than a single batch is encoded as multiple
 * frames, with {@code endOfBatch} set only on the final frame, and that decoding then accumulating those
 * frames reproduces every operation result in order. This is the encoder/decoder counterpart to the
 * end-of-batch accumulation the HTTP, WebSocket and gateway front-ends perform for bulk responses.
 */
class BulkOpsResponseBatchingTest {

    private static ReusableString rs(String value) {
        var reusableString = new ReusableString();
        reusableString.copyFrom(value);
        return reusableString;
    }

    @Test
    @DisplayName("Should split a large bulk response into multiple frames with end-of-batch only on the last")
    void shouldBatchBulkResponseAcrossFrames() {
        // Arrange - a result with more operations than fit in a single batch (well over any batch size).
        int opCount = 250;
        var requestId = "bulk-req";
        var result = new BulkCacheOpsResult<>(SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.stringSupplier);
        result.setRequestId(requestId);
        result.setEndOfBatch(true);
        for (int i = 0; i < opCount; i++) {
            result.addOperationResult(CacheOperationStatus.SUCCESS, "op-" + i, rs("cache"), rs("k" + i), rs("v" + i));
        }

        var encoder = new ReusableStringCacheResponseEncoder();
        var decoder = new ReusableStringCacheResponseDecoder();
        MutableDirectBuffer egressBuffer = new ExpandableArrayBuffer();

        List<String> accumulatedKeys = new ArrayList<>();
        List<Boolean> frameEndOfBatchFlags = new ArrayList<>();

        // Act - decode each emitted frame synchronously as the encoder produces it, accumulating results.
        encoder.encodeBulkOpsResponse(result, egressBuffer, new HydratingPublicationConsumer() {
            @Override
            public void accept(MutableDirectBuffer buffer) {
                var decoded = new BulkCacheOpsResult<>(SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.stringSupplier);
                decoder.decodeBulkCacheOpsResult(buffer, 0, decoded);
                frameEndOfBatchFlags.add(decoded.isEndOfBatch());
                decoded.getOperations().forEach(op -> accumulatedKeys.add(op.getKey().value()));
            }
        });

        // Assert - the response spans multiple frames, end-of-batch only on the last, nothing dropped.
        int frameCount = frameEndOfBatchFlags.size();
        assertTrue(frameCount > 1, "expected the response to span multiple frames");
        for (int i = 0; i < frameCount - 1; i++) {
            assertFalse(frameEndOfBatchFlags.get(i), "only the final frame may be end-of-batch (frame " + i + ")");
        }
        assertTrue(frameEndOfBatchFlags.get(frameCount - 1), "final frame must be end-of-batch");

        assertEquals(opCount, accumulatedKeys.size(), "expected every operation result across all frames");
        for (int i = 0; i < opCount; i++) {
            assertEquals("k" + i, accumulatedKeys.get(i), "operation results must stay in order across frames");
        }
    }

    @Test
    @DisplayName("Should emit a single end-of-batch frame for an empty bulk response")
    void shouldEmitSingleFrameForEmptyBulkResponse() {
        // Arrange
        var result = new BulkCacheOpsResult<>(SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.stringSupplier);
        result.setRequestId("empty-req");
        result.setEndOfBatch(true);

        var encoder = new ReusableStringCacheResponseEncoder();
        var decoder = new ReusableStringCacheResponseDecoder();
        MutableDirectBuffer egressBuffer = new ExpandableArrayBuffer();

        List<Boolean> frameEndOfBatchFlags = new ArrayList<>();
        int[] totalOps = {0};

        // Act
        encoder.encodeBulkOpsResponse(result, egressBuffer, new HydratingPublicationConsumer() {
            @Override
            public void accept(MutableDirectBuffer buffer) {
                var decoded = new BulkCacheOpsResult<>(SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.stringSupplier);
                decoder.decodeBulkCacheOpsResult(buffer, 0, decoded);
                frameEndOfBatchFlags.add(decoded.isEndOfBatch());
                totalOps[0] += decoded.getOperations().size();
            }
        });

        // Assert
        assertEquals(1, frameEndOfBatchFlags.size(), "an empty response is still a single frame");
        assertTrue(frameEndOfBatchFlags.get(0), "the single empty frame must be end-of-batch");
        assertEquals(0, totalOps[0]);
    }
}
