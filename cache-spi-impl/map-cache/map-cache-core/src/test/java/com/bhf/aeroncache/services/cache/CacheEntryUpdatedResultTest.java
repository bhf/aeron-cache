package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseDecoder;
import com.bhf.aeroncache.messages.CacheEntryUpdateEncoder;
import com.bhf.aeroncache.messages.MessageHeaderEncoder;
import com.bhf.aeroncache.models.results.CacheEntryUpdateResult;
import com.bhf.aeroncache.services.cacheclient.MapCacheClientSchemDetailsProvider;
import com.bhf.aeroncache.utils.SupplierUtils;
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
class CacheEntryUpdatedResultTest {

    @Mock
    private Header header;
    private MutableDirectBuffer requestBuffer;
    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final CacheEntryUpdateEncoder cacheEntryUpdateEncoder = new CacheEntryUpdateEncoder();
    private AeronCacheClusterListener sut;
    @Mock
    private CacheResponseHandler callbackHandler;
    @Mock
    private ReusableStringCacheResponseDecoder cacheResponseDecoder;

    @BeforeEach
    void setup() {
        sut = new AeronCacheClusterListener(cacheResponseDecoder, new MapCacheClientSchemDetailsProvider(), SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.stringSupplier);
        sut.setCacheResultsCallbacks(callbackHandler);
        requestBuffer = new ExpandableArrayBuffer(512);
    }

    @Test
    @DisplayName("Should decode cache entry update result and pass to callback")
    @HappyPath
    void shouldDecodePassResultToCallbackHandler() {
        // Arrange
        var sessionId = 1L;
        var timeStamp = System.currentTimeMillis();
        var length = 1024;
        cacheEntryUpdateEncoder.wrapAndApplyHeader(requestBuffer, 0, headerEncoder);

        // Act
        sut.onMessage(sessionId, timeStamp, requestBuffer, 0, length, header);

        // Assert
        verify(
                cacheResponseDecoder, times(1)).decodeCacheEntryUpdated(
                any(DirectBuffer.class), anyInt(), any(CacheEntryUpdateResult.class)
        );

        verify(callbackHandler, times(1))
                .handleCacheEntryUpdated(any(CacheEntryUpdateResult.class));
    }

}