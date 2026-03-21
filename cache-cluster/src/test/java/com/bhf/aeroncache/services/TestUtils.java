package com.bhf.aeroncache.services;

import com.bhf.aeroncache.application.CacheSnapshotCodecUtils;
import com.bhf.aeroncache.codecs.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.RegularStringCacheRequestEncoder;
import com.bhf.aeroncache.services.cachemanager.BasicCacheManagerFactory;
import com.bhf.aeroncache.services.cluster.ReusableStringCacheRequestDecoder;
import com.bhf.aeroncache.services.cluster.ReusableStringCacheResponseEncoder;
import com.bhf.aeroncache.services.cluster.SBEDecodingCacheClusterService;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.DirectBufferVector;
import io.aeron.cluster.service.ClientSession;
import io.aeron.logbuffer.BufferClaim;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import java.util.UUID;

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

    public static BasicCacheManagerFactory<ReusableString, ReusableString, ReusableString> getCacheManagerFactory(){
        var encoder = new ReusableStringCacheResponseEncoder();
        var decoder = new ReusableStringCacheRequestDecoder();
        return new BasicCacheManagerFactory<>(SupplierUtils.stringSupplier,
                SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.mapSupplier,
                CacheSnapshotCodecUtils.getCacheIdSnapshotCodec(), CacheSnapshotCodecUtils.getCacheEntrySnapshotCodec(), encoder, decoder);
    }
}
