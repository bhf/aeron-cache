package com.bhf.aeroncache.services;

import com.bhf.aeroncache.codecs.ReusableStringTimersCodec;
import com.bhf.aeroncache.codecs.request.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.RegularStringCacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCacheRequestDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseEncoder;
import com.bhf.aeroncache.models.bulk.requests.BulkOperationType;
import com.bhf.aeroncache.models.bulk.requests.CacheOperationRequest;
import com.bhf.aeroncache.models.results.CacheOperationResultDetails;
import com.bhf.aeroncache.services.cache.snapshot.ReusableStringCacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.ReusableStringCacheIdSnapshotCodec;
import com.bhf.aeroncache.services.cachemanager.MapCacheManagerFactory;
import com.bhf.aeroncache.services.cluster.SBEDecodingCacheClusterService;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.DirectBufferVector;
import io.aeron.cluster.service.ClientSession;
import io.aeron.logbuffer.BufferClaim;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

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
        var timersCodec = new ReusableStringTimersCodec();
        return new MapCacheManagerFactory<>(SupplierUtils.stringSupplier,
                SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.mapSupplier,
                new ReusableStringCacheIdSnapshotCodec(),
                new ReusableStringCacheEntrySnapshotCodec(),
                encoder,
                decoder,
                timersCodec);
    }

    public static void assertRequestIdCacheIdMatch(List<CacheOperationRequest> ops, List<CacheOperationResultDetails<ReusableString, ReusableString, ReusableString>> opResults, int i) {
        assertEquals(ops.get(i).requestId(), opResults.get(i).getRequestId());
        assertEquals(ops.get(i).cacheId(), opResults.get(i).getCacheId().value());
    }

    public static CacheOperationRequest getCacheOperation(BulkOperationType opType, String cacheId, String key, String value, long ttl) {
        String opRequestId = UUID.randomUUID().toString();
        return new CacheOperationRequest(opType, ttl, opRequestId, cacheId, key, value);
    }
}
