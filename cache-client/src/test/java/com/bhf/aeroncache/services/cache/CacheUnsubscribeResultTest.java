package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.CacheResponseDecoder;
import com.bhf.aeroncache.messages.CacheUnsubscribeResponseDecoder;
import com.bhf.aeroncache.messages.CacheUnsubscribeResponseEncoder;
import com.bhf.aeroncache.messages.MessageHeaderDecoder;
import com.bhf.aeroncache.messages.MessageHeaderEncoder;
import com.bhf.aeroncache.models.results.CacheUnsubscribeResult;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheUnsubscribeResultTest {

    @Mock
    private Header header;
    private MutableDirectBuffer requestBuffer;
    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final CacheUnsubscribeResponseEncoder cacheUnsubscribeRequestEncoder =
            new CacheUnsubscribeResponseEncoder();
    private AeronCacheClusterListener sut;
    @Mock
    private CacheResponseHandler callbackHandler;
    @Mock
    private CacheResponseDecoder cacheResponseDecoder;

    @BeforeEach
    void setup() {
        sut = new AeronCacheClusterListener(cacheResponseDecoder);
        sut.setCacheResultsCallbacks(callbackHandler);
        requestBuffer = new ExpandableArrayBuffer(512);
    }

    @Test
    @DisplayName("Should decode cache unsubscribe result and pass to callback")
    @HappyPath
    void shouldDecodePassResultToCallbackHandler() {
        // Arrange
        var sessionId = 1L;
        var timeStamp = System.currentTimeMillis();
        var length = 1024;
        cacheUnsubscribeRequestEncoder.wrapAndApplyHeader(requestBuffer, 0, headerEncoder);

        // Act
        sut.onMessage(sessionId, timeStamp, requestBuffer, 0, length, header);

        // Assert
        verify(
                cacheResponseDecoder, times(1)).decodeCacheUnsubscribeResult(
                any(CacheUnsubscribeResult.class),
                any(DirectBuffer.class),
                anyInt());

        verify(callbackHandler, times(1))
                .handleCacheUnsubscribeResponse(any(CacheUnsubscribeResult.class));
    }

}