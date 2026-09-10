package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.request.CacheRequestDecoder;
import com.bhf.aeroncache.codecs.response.CacheResponseEncoder;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.requests.*;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.CacheTimerService;
import com.bhf.aeroncache.services.cachemanager.CacheManager;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.cachemanager.CacheSchemaDetailsProvider;
import com.bhf.aeroncache.services.cachemanager.CountersCacheManager;
import com.bhf.aeroncache.services.subscription.CacheSubscriptionService;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import com.bhf.aeroncache.types.ReusableString;
import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractCacheClusterServiceTest {

    private TestCacheClusterService sut;

    @Mock
    private CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory;

    @Mock
    private CountersCacheManager<ReusableString, ReusableString, ReusableLong> countersCacheManager;

    @Mock
    private CacheManager<ReusableString, ReusableString, ReusableString> cacheManager;

    @Mock
    private CacheTracingService tracingService;

    @Mock
    private CacheTimerService<ReusableString, ReusableString> cacheTimerService;

    @Mock
    private CacheTimerService<ReusableString, ReusableString> cacheCountersTimerService;

    @Mock
    private ExclusivePublication snapshotPublication;

    @Mock
    private Image snapshotImage;

    @Mock
    private Cluster cluster;

    @Mock
    private CacheSchemaDetailsProvider schemaDetailsProvider;

    @BeforeEach
    void setUp() throws Exception {
        Supplier<ReusableString> stringSupplier = ReusableString::new;
        when(cacheManagerFactory.getIndexSupplier()).thenReturn(stringSupplier);
        when(cacheManagerFactory.getKeySupplier()).thenReturn(stringSupplier);
        when(cacheManagerFactory.getValueSupplier()).thenReturn(ReusableString::new);
        when(cacheManagerFactory.getCacheManager()).thenReturn(cacheManager);
        when(cacheManagerFactory.getSchemaDetailsProvider()).thenReturn(schemaDetailsProvider);
        when(cacheManagerFactory.getCountersCacheManager()).thenReturn(countersCacheManager);

        sut = new TestCacheClusterService("test-node", tracingService, cacheManagerFactory);

        setTimerServiceWithReflection();
        setCountersTimerServiceWithReflection();
        setClusterWithReflection();
    }

    private void setClusterWithReflection() throws NoSuchFieldException, IllegalAccessException {
        Field clusterField = AbstractCacheClusterService.class.getDeclaredField("cluster");
        clusterField.setAccessible(true);
        clusterField.set(sut, cluster);
    }

    /**
     * Use a reflection based approach to set the {@link CacheTimerService} rather than onStart()
     * based orchestration.
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private void setTimerServiceWithReflection() throws NoSuchFieldException, IllegalAccessException {
        Field timerField = AbstractCacheClusterService.class.getDeclaredField("cacheTimerService");
        timerField.setAccessible(true);
        timerField.set(sut, cacheTimerService);
    }

    private void setCountersTimerServiceWithReflection() throws NoSuchFieldException, IllegalAccessException {
        Field timerField = AbstractCacheClusterService.class.getDeclaredField("cacheCountersTimerService");
        timerField.setAccessible(true);
        timerField.set(sut, cacheCountersTimerService);
    }

    @Test
    @HappyPath
    @DisplayName("Should delegate to internal services when taking snapshot")
    void shouldTakeSnapshot() {
        // Arrange
        when(cacheTimerService.onTakeSnapshot(eq(snapshotPublication), any(MutableDirectBuffer.class))).thenReturn(100);

        // Act
        sut.onTakeSnapshot(snapshotPublication);

        // Assert
        verify(cacheTimerService).onTakeSnapshot(eq(snapshotPublication), any(MutableDirectBuffer.class));
        verify(snapshotPublication).offer(any(MutableDirectBuffer.class), eq(0), eq(100));
        verify(cacheManager).takeSnapshot(snapshotPublication, cluster);
    }

    @Test
    @HappyPath
    @DisplayName("Should delegate to internal services when loading snapshot")
    void shouldLoadSnapshot() {
        // Act
        sut.loadSnapshot(cluster, snapshotImage);

        // Assert
        verify(cacheTimerService).loadSnapshot(cluster, snapshotImage);
        verify(cacheManager).loadSnapshot(snapshotImage);
    }

    /**
     * Concrete implementation for this test.
     */
    static class TestCacheClusterService extends AbstractCacheClusterService<ReusableString, ReusableString, ReusableString> {
        protected TestCacheClusterService(String nodeId, CacheTracingService tracingService, CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory) {
            super(nodeId, tracingService, cacheManagerFactory);
        }

        @Override
        protected <VT extends Reusable> GetAllCacheEntriesRequestDetails<ReusableString> getAllCacheEntriesRequestDetails(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<ReusableString, ReusableString, VT> decoder, GetAllCacheEntriesRequestDetails<ReusableString> getAllCacheEntriesRequestDetails) {return null;}

        @Override
        protected <VT extends Reusable> ClearCacheRequestDetails<ReusableString> getClearCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<ReusableString, ReusableString, VT> decoder, ClearCacheRequestDetails<ReusableString> clearCacheRequestDetails) {
            return null;
        }

        @Override
        protected <VT extends Reusable> RemoveCacheEntryRequestDetails<ReusableString, ReusableString> getRemoveCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<ReusableString, ReusableString, VT> decoder, RemoveCacheEntryRequestDetails<ReusableString, ReusableString> removeCacheEntryRequestDetails) {
            return null;
        }

        @Override
        protected <VT extends Reusable> GetCacheEntryRequestDetails<ReusableString, ReusableString> getCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<ReusableString, ReusableString, VT> decoder, GetCacheEntryRequestDetails<ReusableString, ReusableString> getCacheEntryRequestDetails) {
            return null;
        }


        @Override
        protected <VT extends Reusable> DeleteCacheRequestDetails<ReusableString> getDeleteCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<ReusableString, ReusableString, VT> decoder, DeleteCacheRequestDetails<ReusableString> deleteCacheRequestDetails) {
            return null;
        }

        @Override
        protected <VT extends Reusable> GetCacheStatsRequestDetails getCacheStatsRequestDetails(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<ReusableString, ReusableString, VT> decoder, GetCacheStatsRequestDetails getCacheStatsRequestDetails) {
            return null;
        }

        @Override
        protected <VT extends Reusable> CacheSubscriptionRequestDetails<ReusableString, ReusableString> getCacheSubscriptionRequest(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<ReusableString, ReusableString, VT> decoder, CacheSubscriptionRequestDetails<ReusableString, ReusableString> cacheSubscribeRequestDetails) {
            return null;
        }

        @Override
        protected <VT extends Reusable> CacheUnsubscribeRequestDetails<ReusableString> getCacheUnsubscribeRequest(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<ReusableString, ReusableString, VT> decoder, CacheUnsubscribeRequestDetails<ReusableString> cacheUnsubscribeRequestDetails) {
            return null;
        }


        @Override protected <VT extends Reusable> AddCacheEntryRequestDetails<ReusableString, ReusableString, VT> getAddCacheEntryRequestDetails(ClientSession s, DirectBuffer b, int o, CacheRequestDecoder<ReusableString,ReusableString,VT> decoder, AddCacheEntryRequestDetails<ReusableString, ReusableString, VT> addCacheEntryRequestDetails) { return null; }
        @Override protected <VT extends Reusable> BulkCacheOpsRequestDetails<ReusableString, ReusableString, VT> getBulkOpsRequest(ClientSession s, DirectBuffer b, int o, BulkCacheOpsRequestDetails<ReusableString, ReusableString, VT> bulkCacheOpsRequestDetails_) { return null; }
        @Override protected <VT extends Reusable> void handlePostCreateCache(ReusableString cacheId, CreateCacheResult<ReusableString> cacheCreationResult, ClientSession session, CacheResponseEncoder<ReusableString, ReusableString, VT> encoder) {}
        @Override protected <VT extends Reusable> void handlePostAddCacheEntry(ReusableString cacheId, ReusableString key, VT value, AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult, ClientSession session, CacheResponseEncoder<ReusableString, ReusableString, VT> encoder, CacheSubscriptionService<ReusableString, ReusableString, VT> subscriptionService, CacheSubscriptionService<ReusableString, ReusableString, VT> patchSubscriptionService, PatchValueResult<ReusableString, ReusableString, VT> mergePatchResult) {}
        @Override protected <VT extends Reusable> void handlePostGetCacheEntry(ReusableString cacheId, ReusableString key, GetCacheEntryResult<ReusableString, ReusableString, VT> getCacheEntryResult, ClientSession session, CacheResponseEncoder<ReusableString, ReusableString, VT> encoder) {}
        @Override protected <VT extends Reusable> void handlePostGetAllCacheEntries(ReusableString cacheId, GetAllCacheEntriesResult<ReusableString, ReusableString, VT> getCacheEntryResult, ClientSession session, CacheResponseEncoder<ReusableString, ReusableString, VT> encoder) {}
        @Override protected <VT extends Reusable> void handlePostRemoveCacheEntry(ReusableString c, ReusableString k, RemoveCacheEntryResult<ReusableString, ReusableString> r, ClientSession s, CacheResponseEncoder<ReusableString, ReusableString, VT> encoder, CacheSubscriptionService<ReusableString, ReusableString, VT> subscriptionService) {}
        @Override protected <VT extends Reusable> void handlePostRemoveTimerCacheEntry(ReusableString c, ReusableString k, RemoveCacheEntryResult<ReusableString, ReusableString> r, CacheResponseEncoder<ReusableString, ReusableString, VT> encoder, CacheSubscriptionService<ReusableString, ReusableString, VT> subscriptionService) {}
        @Override protected <VT extends Reusable> void handlePostClearCache(ReusableString c, ClearCacheResult<ReusableString> r, ClientSession s, CacheResponseEncoder<ReusableString, ReusableString, VT> encoder, CacheSubscriptionService<ReusableString, ReusableString, VT> subscriptionService) {}
        @Override protected <VT extends Reusable> void handlePostDeleteCache(ReusableString c, DeleteCacheResult<ReusableString> r, ClientSession s, CacheResponseEncoder<ReusableString, ReusableString, VT> encoder, CacheSubscriptionService<ReusableString, ReusableString, VT> subscriptionService) {}
        @Override protected <VT extends Reusable> void handlePostGetCacheStats(CacheStatsResult<ReusableString> r, ClientSession s, CacheResponseEncoder<ReusableString, ReusableString, VT> encoder) {}
        @Override protected <VT extends Reusable> void handlePostCacheSubscriptionRequest(CacheSubscriptionResult<ReusableString, ReusableString, VT> subscriptionRequestResult, ClientSession session, CacheResponseEncoder<ReusableString, ReusableString, VT> encoder) {}
        @Override protected <VT extends Reusable> void handlePostCacheUnsubscribeRequest(CacheUnsubscribeResult<ReusableString> r, ClientSession s, CacheResponseEncoder<ReusableString, ReusableString, VT> encoder) {}
        @Override protected void handlePostBulkOpsRequest(BulkCacheOpsResult<ReusableString, ReusableString, ReusableString> r, ClientSession s, CacheResponseEncoder<ReusableString, ReusableString, ReusableString> encoder) {}
    }
}