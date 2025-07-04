package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.CacheResponseDecoder;
import com.bhf.aeroncache.messages.CacheEntryRemovedDecoder;
import com.bhf.aeroncache.messages.CacheEntryRemovedEncoder;
import com.bhf.aeroncache.messages.MessageHeaderDecoder;
import com.bhf.aeroncache.messages.MessageHeaderEncoder;
import com.bhf.aeroncache.models.results.RemoveCacheEntryResult;
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
class CacheEntryRemovedResultTest {

    @Mock
    private Header header;
    private MutableDirectBuffer requestBuffer;
    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final CacheEntryRemovedEncoder cacheEntryRemovedEncoder = new CacheEntryRemovedEncoder();
    private AeronCacheClusterListener sut;
    @Mock
    private CacheResponseHandler callbackHandler;

    @BeforeEach
    void setup() {
        sut = new AeronCacheClusterListener();
        sut.setCacheResultsCallbacks(callbackHandler);
        requestBuffer = new ExpandableArrayBuffer(512);
    }

    @Test
    @DisplayName("Should decode cache entry removed result and pass to callback")
    @HappyPath
    void shouldDecodePassResultToCallbackHandler() {
        // Arrange
        var sessionId = 1L;
        var timeStamp = System.currentTimeMillis();
        var length = 1024;
        cacheEntryRemovedEncoder.wrapAndApplyHeader(requestBuffer, 0, headerEncoder);

        // Act
        try (MockedStatic<CacheResponseDecoder> encoder = Mockito.mockStatic(CacheResponseDecoder.class)) {
            sut.onMessage(sessionId, timeStamp, requestBuffer, 0, length, header);

            // Assert
            encoder.verify(() ->
                            CacheResponseDecoder.decodeCacheEntryRemoved(
                                    any(CacheEntryRemovedDecoder.class),
                                    any(MessageHeaderDecoder.class),
                                    any(RemoveCacheEntryResult.class),
                                    any(DirectBuffer.class),
                                    anyInt()),
                    times(1));

            verify(callbackHandler, times(1))
                    .handleCacheEntryRemoved(any(RemoveCacheEntryResult.class));
        }
    }

}