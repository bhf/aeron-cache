package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.application.TestUtils;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.GetCacheEntryResult;
import com.bhf.aeroncache.models.results.PatchValueResult;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.subscription.CacheSubscriptionService;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import io.aeron.cluster.service.ClientSession;
import io.aeron.logbuffer.Header;
import lombok.RequiredArgsConstructor;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test decoding a patch value request. Uses a Detroit style for simplicity in
 * decoding the response buffer.
 *
 * @param <I>  The cache ID type.
 * @param <K>  The key type.
 * @param <V>  The value type used by the encoder/decoder under test.
 * @param <FV> The value type of the CacheManagerFactory.
 */
@RequiredArgsConstructor
public abstract class AbstractPatchValueTest<I extends Reusable, K extends Reusable, V extends Reusable, FV extends Reusable> {

    private final Header header = new Header(0, 0);
    private final CacheManagerFactory<I, K, FV> cacheManagerFactory;
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private PatchValueResult<I, K, V> result;
    protected SBEDecodingCacheClusterService<I, K, FV> sut;
    private CacheTracingService tracingService;

    @BeforeEach
    void setup() {
        tracingService = Mockito.mock(CacheTracingService.class);
        sut = new SBEDecodingCacheClusterService<>("node0", tracingService, cacheManagerFactory);
        sut.subscriptionService = Mockito.mock(CacheSubscriptionService.class);
        sut.countersSubscriptionService = Mockito.mock(CacheSubscriptionService.class);
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer = new ExpandableArrayBuffer();
        result = createResult();
    }

    @Test
    @DisplayName("Should merge the patch into an existing cache entry")
    void shouldPatchKnownEntry() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        I cacheId = getCacheId();
        K key = getKey();
        V value = getInitialValue();

        createCache(cacheId, session, requestBuffer, sut);

        // add an entry to the cache
        var requestId = UUID.randomUUID().toString();
        var length = encodeAddCacheEntry(requestId, cacheId, key, value, 0, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);

        // Act
        length = encodePatchValue(requestId, cacheId, key, getPatch(), requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        decodePatchValueResult(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId.value(), result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());
        assertEquals(key.value(), result.getEntryKey().value());

        // The patch response does not carry the value, so verify the merge by getting the entry
        var getResult = createGetResult();
        length = encodeGetCacheEntry(requestId, cacheId, key, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        decodeGetCacheEntryResult(responseBuffer, 0, getResult);

        assertEquals(CacheOperationStatus.SUCCESS, getResult.getStatus());
        assertEquals(key.value(), getResult.getEntryKey().value());
        assertEquals(getExpectedPatchedValue().value(), getResult.getEntryValue().value());

        // A successful patch notifies subscribers of the updated entry (once for the
        // initial add and once for the patch)
        verify(sut.subscriptionService, times(2)).handleEntryAdded(
                any(AddCacheEntryResult.class),
                any(MutableDirectBuffer.class),
                eq(key),
                any(),
                anyInt());
    }

    @Test
    @DisplayName("Should notify the patch subscriber only on patch and not on add")
    void shouldNotifyPatchSubscriberOnlyOnPatch() {
        // Arrange
        sut.patchSubscriptionService = Mockito.mock(CacheSubscriptionService.class);

        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        I cacheId = getCacheId();
        K key = getKey();
        V value = getInitialValue();

        createCache(cacheId, session, requestBuffer, sut);

        var requestId = UUID.randomUUID().toString();
        var length = encodeAddCacheEntry(requestId, cacheId, key, value, 0, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);

        // Adding an entry must NOT notify the patch subscription service
        verify(sut.patchSubscriptionService, Mockito.never()).handleEntryAdded(
                any(AddCacheEntryResult.class), any(MutableDirectBuffer.class), any(), any(), anyInt());

        // Act - patch the entry
        length = encodePatchValue(requestId, cacheId, key, getPatch(), requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);

        // Assert - a patch notifies the patch subscription service exactly once
        verify(sut.patchSubscriptionService, times(1)).handleEntryAdded(
                any(AddCacheEntryResult.class),
                any(MutableDirectBuffer.class),
                eq(key),
                any(),
                anyInt());
    }

    @Test
    @DisplayName("Should notify when entry key is unknown")
    void shouldNotifyWhenEntryKeyIsUnknown() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        I cacheId = getUnknownKeyCacheId();
        K key = getKey();

        createCache(cacheId, session, requestBuffer, sut);

        // Act
        var requestId = UUID.randomUUID().toString();
        var length = encodePatchValue(requestId, cacheId, key, getPatch(), requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        decodePatchValueResult(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId.value(), result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.UNKNOWN_KEY, result.getStatus());
    }

    @Test
    @DisplayName("Should report an error when the patch is not valid JSON")
    void shouldReturnErrorForInvalidPatch() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        I cacheId = getCacheId();
        K key = getKey();
        V value = getInitialValue();

        createCache(cacheId, session, requestBuffer, sut);

        var requestId = UUID.randomUUID().toString();
        var length = encodeAddCacheEntry(requestId, cacheId, key, value, 0, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);

        // Act
        length = encodePatchValue(requestId, cacheId, key, getInvalidPatch(), requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        decodePatchValueResult(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId.value(), result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.ERROR, result.getStatus());
    }

    @Test
    @DisplayName("Should notify the patch subscriber with the merge patch when an add overwrites an existing entry")
    void shouldNotifyPatchSubscriberOnAddOverExistingKey() {
        // Arrange
        sut.patchSubscriptionService = Mockito.mock(CacheSubscriptionService.class);
        when(sut.patchSubscriptionService.hasSubscriber(any(), any())).thenReturn(true);

        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        I cacheId = getCacheId();
        K key = getKey();

        createCache(cacheId, session, requestBuffer, sut);

        var requestId = UUID.randomUUID().toString();
        // initial add - no prior value, so no patch is produced
        var length = encodeAddCacheEntry(requestId, cacheId, key, getInitialValue(), 0, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);

        verify(sut.patchSubscriptionService, Mockito.never()).handleEntryAdded(
                any(AddCacheEntryResult.class), any(MutableDirectBuffer.class), any(), any(), anyInt());

        // Act - overwrite the key with a new value that differs from the previous one
        length = encodeAddCacheEntry(requestId, cacheId, key, getExpectedPatchedValue(), 0, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);

        // Assert - the patch subscriber is notified once with the merge patch (the delta)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<V> valueCaptor = ArgumentCaptor.forClass((Class<V>) getPatch().getClass());
        verify(sut.patchSubscriptionService, times(1)).handleEntryAdded(
                any(AddCacheEntryResult.class),
                any(MutableDirectBuffer.class),
                eq(key),
                valueCaptor.capture(),
                anyInt());
        assertEquals(getPatch().value(), valueCaptor.getValue().value());
    }

    @Test
    @DisplayName("Should not notify the patch subscriber when an add creates a new key")
    void shouldNotNotifyPatchSubscriberOnFirstAdd() {
        // Arrange
        sut.patchSubscriptionService = Mockito.mock(CacheSubscriptionService.class);
        when(sut.patchSubscriptionService.hasSubscriber(any(), any())).thenReturn(true);

        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        I cacheId = getCacheId();
        K key = getKey();

        createCache(cacheId, session, requestBuffer, sut);

        // Act - a single add on a fresh key
        var requestId = UUID.randomUUID().toString();
        var length = encodeAddCacheEntry(requestId, cacheId, key, getInitialValue(), 0, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);

        // Assert - no prior value means no merge patch, so the patch subscriber is not notified
        verify(sut.patchSubscriptionService, Mockito.never()).handleEntryAdded(
                any(AddCacheEntryResult.class), any(MutableDirectBuffer.class), any(), any(), anyInt());
    }

    @Test
    @DisplayName("Should not notify the patch subscriber on an overwriting add when there is no patch subscriber for the key")
    void shouldNotNotifyPatchSubscriberOnAddWhenNoSubscriberForKey() {
        // Arrange - patchSubscriptionService is present but reports no subscriber for the key
        sut.patchSubscriptionService = Mockito.mock(CacheSubscriptionService.class);
        when(sut.patchSubscriptionService.hasSubscriber(any(), any())).thenReturn(false);

        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        I cacheId = getCacheId();
        K key = getKey();

        createCache(cacheId, session, requestBuffer, sut);

        var requestId = UUID.randomUUID().toString();
        var length = encodeAddCacheEntry(requestId, cacheId, key, getInitialValue(), 0, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);

        // Act - overwrite the key with a differing value
        length = encodeAddCacheEntry(requestId, cacheId, key, getExpectedPatchedValue(), 0, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);

        // Assert - no subscriber for the key means no patch is produced or sent
        verify(sut.patchSubscriptionService, Mockito.never()).handleEntryAdded(
                any(AddCacheEntryResult.class), any(MutableDirectBuffer.class), any(), any(), anyInt());
    }

    protected abstract PatchValueResult<I, K, V> createResult();

    protected abstract GetCacheEntryResult<I, K, V> createGetResult();

    public abstract int encodeAddCacheEntry(String requestId, I cacheId, K key, V value, long ttl, MutableDirectBuffer buffer);

    public abstract int encodePatchValue(String requestId, I cacheId, K key, V patch, MutableDirectBuffer buffer);

    public abstract int encodeGetCacheEntry(String requestId, I cacheId, K key, MutableDirectBuffer buffer);

    public abstract void decodePatchValueResult(DirectBuffer buffer, int offset, PatchValueResult<I, K, V> result);

    public abstract void decodeGetCacheEntryResult(DirectBuffer buffer, int offset, GetCacheEntryResult<I, K, V> result);

    protected abstract void createCache(I cacheId, ClientSession session, MutableDirectBuffer requestBuffer, SBEDecodingCacheClusterService<I, K, FV> sut);

    protected abstract I getCacheId();

    protected abstract I getUnknownKeyCacheId();

    protected abstract K getKey();

    protected abstract V getInitialValue();

    protected abstract V getPatch();

    protected abstract V getExpectedPatchedValue();

    protected abstract V getInvalidPatch();
}
