package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseDecoder;
import com.bhf.aeroncache.messages.CacheDeletedEncoder;
import com.bhf.aeroncache.messages.MessageHeaderEncoder;
import com.bhf.aeroncache.models.results.DeleteCacheResult;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheDeletedResultTest {

    @Mock
    private Header header;
    private MutableDirectBuffer requestBuffer;
    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final CacheDeletedEncoder cacheDeletedEncoder = new CacheDeletedEncoder();
    private AeronCacheClusterListener sut;
    @Mock
    private CacheResponseHandler callbackHandler;
    @Mock
    private ReusableStringCacheResponseDecoder cacheResponseDecoder;

    @BeforeEach
    void setup() {
        sut = new AeronCacheClusterListener(cacheResponseDecoder);
        sut.setCacheResultsCallbacks(callbackHandler);
        requestBuffer = new ExpandableArrayBuffer(512);
    }

    @Test
    @DisplayName("Should decode cache deleted result and pass to callback")
    @HappyPath
    void shouldDecodePassResultToCallbackHandler() {
        // Arrange
        var sessionId = 1L;
        var timeStamp = System.currentTimeMillis();
        var length = 1024;
        cacheDeletedEncoder.wrapAndApplyHeader(requestBuffer, 0, headerEncoder);

        // Act
        sut.onMessage(sessionId, timeStamp, requestBuffer, 0, length, header);

        // Assert
        verify(
                cacheResponseDecoder, times(1)).decodeCacheDeleted(
                any(DirectBuffer.class), anyInt(), any(DeleteCacheResult.class)
        );

        verify(callbackHandler, times(1))
                .handleCacheDeleted(any(DeleteCacheResult.class));
    }

}