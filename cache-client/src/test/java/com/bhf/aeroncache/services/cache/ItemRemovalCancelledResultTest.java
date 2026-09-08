package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.response.CacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.CountersCacheResponseDecoder;
import com.bhf.aeroncache.models.results.CancelItemRemovalResult;
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
class ItemRemovalCancelledResultTest {

    public static final int CACHE_TID = 1;
    public static final int COUNTER_TID = 2;
    public static final int TID_INDEX = 2;
    @Mock
    private Header header;
    private MutableDirectBuffer requestBuffer;

    private AeronCacheClusterListener sut;
    @Mock
    private CacheResponseHandler callbackHandler;
    @Mock
    private CacheResponseHandler counterCallbackHandler;
    @Mock
    private CacheResponseDecoder cacheResponseDecoder;
    @Mock
    private CountersCacheResponseDecoder countersCacheResponseDecoder;
    @Mock
    private CacheClientSchemDetailsProvider schemaDetailsProvider;

    @BeforeEach
    void setup() {
        var supplier = mock(Supplier.class);
        sut = new AeronCacheClusterListener<>(cacheResponseDecoder, schemaDetailsProvider,
                supplier, supplier, supplier);
        sut.setCacheResultsCallbacks(callbackHandler);
        sut.setCountersResultsCallbacks(counterCallbackHandler);
        sut.setCountersCacheResponseDecoder(countersCacheResponseDecoder);
        requestBuffer = new ExpandableArrayBuffer(512);
    }

    @Test
    @DisplayName("Should decode cache item removal cancelled result and pass to callback")
    @HappyPath
    void shouldDecodeCacheResultToCallbackHandler() {
        // Arrange
        when(schemaDetailsProvider.getCacheItemRemovalCancelledId()).thenReturn(CACHE_TID);
        requestBuffer.putShort(TID_INDEX, (short) CACHE_TID);

        // Act
        sut.onMessage(1L, 1L, requestBuffer, 0, 1024, header);

        // Assert
        verify(cacheResponseDecoder, times(1)).decodeItemRemovalCancelled(
                any(DirectBuffer.class), anyInt(), any(CancelItemRemovalResult.class)
        );
        verify(callbackHandler, times(1))
                .handleItemRemovalCancelled(any(CancelItemRemovalResult.class));
    }

    @Test
    @DisplayName("Should decode counter item removal cancelled result and pass to callback")
    @HappyPath
    void shouldDecodeCounterResultToCallbackHandler() {
        // Arrange
        when(schemaDetailsProvider.getCounterItemRemovalCancelledId()).thenReturn(COUNTER_TID);
        requestBuffer.putShort(TID_INDEX, (short) COUNTER_TID);

        // Act
        sut.onMessage(1L, 1L, requestBuffer, 0, 1024, header);

        // Assert
        verify(countersCacheResponseDecoder, times(1)).decodeItemRemovalCancelled(
                any(DirectBuffer.class), anyInt(), any(CancelItemRemovalResult.class)
        );
        verify(counterCallbackHandler, times(1))
                .handleItemRemovalCancelled(any(CancelItemRemovalResult.class));
    }

}
