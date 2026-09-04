package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.response.CacheResponseDecoder;
import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.services.cacheclient.CacheClientSchemDetailsProvider;
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

import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheClearedResultTest {

    public static final int CACHE_CLEARED_TID = 1;
    public static final int TID_INDEX = 2;
    @Mock
    private Header header;
    private MutableDirectBuffer requestBuffer;

    private AeronCacheClusterListener sut;
    @Mock
    private CacheResponseHandler callbackHandler;
    @Mock
    private CacheResponseDecoder cacheResponseDecoder;
    @Mock
    private CacheClientSchemDetailsProvider schemaDetailsProvider;

    @BeforeEach
    void setup() {
        var supplier = mock(Supplier.class);
        sut = new AeronCacheClusterListener<>(cacheResponseDecoder, schemaDetailsProvider,
                supplier, supplier, supplier);
        sut.setCacheResultsCallbacks(callbackHandler);
        requestBuffer = new ExpandableArrayBuffer(512);
    }

    @Test
    @DisplayName("Should decode cache cleared result and pass to callback")
    @HappyPath
    void shouldDecodePassResultToCallbackHandler() {
        // Arrange
        var sessionId = 1L;
        var timeStamp = 1L;
        var length = 1024;
        when(schemaDetailsProvider.getCacheClearedId()).thenReturn(CACHE_CLEARED_TID);

        requestBuffer.putShort(TID_INDEX, (short) CACHE_CLEARED_TID);

        // Act
        sut.onMessage(sessionId, timeStamp, requestBuffer, 0, length, header);

        // Assert
        verify(
                cacheResponseDecoder, times(1)).decodeCacheCleared(
                any(DirectBuffer.class), anyInt(), any(ClearCacheResult.class)
        );

        verify(callbackHandler, times(1))
                .handleCacheCleared(any(ClearCacheResult.class));
    }

}
