package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.messages.CacheCreatedDecoder;
import com.bhf.aeroncache.messages.CreateCacheEncoder;
import com.bhf.aeroncache.messages.MessageHeaderDecoder;
import com.bhf.aeroncache.messages.MessageHeaderEncoder;
import io.aeron.DirectBufferVector;
import io.aeron.cluster.service.ClientSession;
import io.aeron.logbuffer.BufferClaim;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test cases for the {@link SBEDecodingCacheClusterService}.
 */
class SBEDecodingCacheClusterServiceTest {

    private static final long MAX_SBE_LONG = Long.MAX_VALUE;
    private static final long MIN_SBE_LONG = -Long.MAX_VALUE;
    private SBEDecodingCacheClusterService sut;
    private MutableDirectBuffer responseBuffer;
    private MutableDirectBuffer requestBuffer;
    private final Header header= new Header(0,0);
    private final CreateCacheEncoder createCacheEncoder = new CreateCacheEncoder();
    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final CacheCreatedDecoder cacheCreatedDecoder=new CacheCreatedDecoder();
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();


    @BeforeEach
    void setup(){
        sut=new SBEDecodingCacheClusterService();
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer =new ExpandableArrayBuffer();
    }

    /**
     * Test creating a cache.
     */
    @ParameterizedTest
    @ValueSource(longs = {0, MAX_SBE_LONG, MIN_SBE_LONG})
    void testCreateCacheMessage(long cacheId){
        ClientSession session=getMockedSession();
        long ts=System.currentTimeMillis();
        createCacheEncoder.wrapAndApplyHeader(requestBuffer, 0, headerEncoder)
                .cacheId(cacheId);
        int length=createCacheEncoder.encodedLength() + headerEncoder.encodedLength();
        sut.onSessionMessage(session, ts, requestBuffer, 0, length, header);
        cacheCreatedDecoder.wrapAndApplyHeader(responseBuffer, 0, headerDecoder);
        var cacheIdCreated = cacheCreatedDecoder.cacheId();
        assertEquals(cacheId, cacheIdCreated);
    }

    /**
     * The mocked session copies response data over to the member
     * response buffer {@link SBEDecodingCacheClusterServiceTest#responseBuffer}.
     * @return The mocked client session.
     */
    private ClientSession getMockedSession() {
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

}
