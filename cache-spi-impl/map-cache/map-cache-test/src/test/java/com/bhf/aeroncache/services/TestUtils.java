package com.bhf.aeroncache.services;

import com.bhf.aeroncache.codecs.ReusableStringTimersCodec;
import com.bhf.aeroncache.codecs.request.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.RegularStringCacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCacheRequestDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseEncoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCountersCacheRequestDecoder;
import com.bhf.aeroncache.codecs.response.CacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCountersCacheResponseEncoder;
import com.bhf.aeroncache.models.bulk.requests.BulkOperationType;
import com.bhf.aeroncache.models.bulk.requests.CacheOperationRequest;
import com.bhf.aeroncache.models.results.BulkCacheOpsResult;
import com.bhf.aeroncache.models.results.CacheOperationResultDetails;
import com.bhf.aeroncache.services.cache.snapshot.CountersCacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.ReusableStringCacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.ReusableStringCacheIdSnapshotCodec;
import com.bhf.aeroncache.services.cachemanager.MapCacheManagerFactory;
import com.bhf.aeroncache.services.cluster.SBEDecodingCacheClusterService;
import com.bhf.aeroncache.services.integrity.NoOpMultiTypeStreamingHasher;
import com.bhf.aeroncache.services.integrity.NoOpStreamingHasher;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.DirectBufferVector;
import io.aeron.cluster.service.ClientSession;
import io.aeron.logbuffer.BufferClaim;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUtils {

    private static final CacheRequestEncoder cacheRequestEncoder = new RegularStringCacheRequestEncoder();
    private static final Header header = new Header(0, 0);
    /**
     * The mocked session copies response data over to the
     * response buffer.
     *
     * @return The mocked client session.
     */
    public static ClientSession getMockedSession(MutableDirectBuffer responseBuffer) {
        return new ClientSession() {
            @Override
            public long id() {
                return 0;
            }

            @Override
            public int responseStreamId() {
                return 0;
            }

            @Override
            public String responseChannel() {
                return null;
            }

            @Override
            public byte[] encodedPrincipal() {
                return new byte[0];
            }

            @Override
            public void close() {

            }

            @Override
            public boolean isClosing() {
                return false;
            }

            @Override
            public long offer(DirectBuffer buffer, int offset, int length) {
                responseBuffer.putBytes(0, buffer, offset, length);
                return length;
            }

            @Override
            public long offer(DirectBufferVector[] vectors) {
                return 0;
            }

            @Override
            public long tryClaim(int length, BufferClaim bufferClaim) {
                return 0;
            }
        };
    }

    /**
     * A mocked session that captures every frame offered to it as an independent copy, in offer order.
     * Unlike {@link #getMockedSession(MutableDirectBuffer)} (which overwrites a single buffer and so only
     * retains the last frame), this preserves every frame. A bulk ops response is published in one or more
     * batches (only the final frame flagged end-of-batch), so a response spanning several frames needs every
     * frame to be reconstructed - see {@link #decodeBulkOpsFrames}.
     *
     * @param frames the list each offered frame is appended to.
     * @return the mocked client session.
     */
    public static ClientSession getFrameCapturingSession(List<DirectBuffer> frames) {
        return new ClientSession() {
            @Override
            public long id() {
                return 0;
            }

            @Override
            public int responseStreamId() {
                return 0;
            }

            @Override
            public String responseChannel() {
                return null;
            }

            @Override
            public byte[] encodedPrincipal() {
                return new byte[0];
            }

            @Override
            public void close() {

            }

            @Override
            public boolean isClosing() {
                return false;
            }

            @Override
            public long offer(DirectBuffer buffer, int offset, int length) {
                var copy = new ExpandableArrayBuffer(length);
                copy.putBytes(0, buffer, offset, length);
                frames.add(copy);
                return length;
            }

            @Override
            public long offer(DirectBufferVector[] vectors) {
                return 0;
            }

            @Override
            public long tryClaim(int length, BufferClaim bufferClaim) {
                return 0;
            }
        };
    }

    /**
     * Decode a bulk ops response that may span multiple frames and return every operation result across all
     * frames, in order. The cluster service publishes a bulk response in batches (end-of-batch only on the
     * final frame); callers accumulate the per-operation results across frames to reconstruct the full
     * response, independently of how it was batched. The request id and end-of-batch flag of the response
     * are copied into {@code into} so callers can assert on them.
     *
     * @param frames  the captured response frames, in offer order (see {@link #getFrameCapturingSession}).
     * @param decoder the response decoder to use.
     * @param into    populated with the response's request id and (final) end-of-batch flag.
     * @return the operation results across every frame, in order.
     */
    public static List<CacheOperationResultDetails<ReusableString, ReusableString, ReusableString>> decodeBulkOpsFrames(
            List<DirectBuffer> frames, CacheResponseDecoder decoder,
            BulkCacheOpsResult<ReusableString, ReusableString, ReusableString> into) {
        var combined = new ArrayList<CacheOperationResultDetails<ReusableString, ReusableString, ReusableString>>();
        boolean endOfBatch = false;
        for (DirectBuffer frame : frames) {
            // Decode each frame into its own result so its pooled operation results stay stable once gathered.
            var frameResult = new BulkCacheOpsResult<>(SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.stringSupplier);
            decoder.decodeBulkCacheOpsResult(frame, 0, frameResult);
            combined.addAll(frameResult.getOperations());
            into.setRequestId(frameResult.getRequestId());
            endOfBatch = frameResult.isEndOfBatch();
        }
        into.setEndOfBatch(endOfBatch);
        return combined;
    }

    /**
     * Create a cache.
     * @param cacheId
     * @param session
     * @param requestBuffer
     * @param sut
     */
    public static void createCache(String cacheId, ClientSession session, MutableDirectBuffer requestBuffer,
                                   SBEDecodingCacheClusterService sut) {
        var requestId = UUID.randomUUID().toString();
        var length = cacheRequestEncoder.encodeCreateCacheRequest(requestId, cacheId, requestBuffer);
        long ts = System.currentTimeMillis();
        sut.onSessionMessage(session, ts, requestBuffer, 0, length, header);
    }

    public static MapCacheManagerFactory<ReusableString, ReusableString, ReusableString> getCacheManagerFactory(){
        var encoder = new ReusableStringCacheResponseEncoder();
        var decoder = new ReusableStringCacheRequestDecoder();
        var timersCodec = new ReusableStringTimersCodec(new NoOpMultiTypeStreamingHasher<>());
        var responseEncoder = new ReusableStringCountersCacheResponseEncoder();
        var requestDecoder = new ReusableStringCountersCacheRequestDecoder();
        return new MapCacheManagerFactory<>(SupplierUtils.stringSupplier,
                SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.mapSupplier,
                new ReusableStringCacheIdSnapshotCodec(new NoOpStreamingHasher<>()),
                new ReusableStringCacheEntrySnapshotCodec(new NoOpStreamingHasher<>()),
                new CountersCacheEntrySnapshotCodec(new NoOpStreamingHasher<>()),
                encoder,
                decoder,
                timersCodec, responseEncoder, requestDecoder);
    }

    public static void assertRequestIdCacheIdMatch(List<CacheOperationRequest> ops, List<CacheOperationResultDetails<ReusableString, ReusableString, ReusableString>> opResults, int i) {
        assertEquals(ops.get(i).requestId(), opResults.get(i).getRequestId());
        assertEquals(ops.get(i).cacheId(), opResults.get(i).getCacheId().value());
    }

    public static CacheOperationRequest getCacheOperation(BulkOperationType opType, String cacheId, String key, String value, long ttl) {
        String opRequestId = UUID.randomUUID().toString();
        return new CacheOperationRequest(opType, ttl, 0, opRequestId, cacheId, key, value);
    }
}
