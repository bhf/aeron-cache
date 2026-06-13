package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.models.requests.*;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.CacheTimerService;
import com.bhf.aeroncache.services.cachemanager.CacheManager;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.cachemanager.CacheSchemaDetailsProvider;
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
    private CacheManager<ReusableString, ReusableString, ReusableString> cacheManager;

    @Mock
    private CacheTracingService tracingService;

    @Mock
    private CacheTimerService<ReusableString, ReusableString, ReusableString> cacheTimerService;

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
        when(cacheManagerFactory.getValueSupplier()).thenReturn(stringSupplier);
        when(cacheManagerFactory.getCacheManager()).thenReturn(cacheManager);
        when(cacheManagerFactory.getSchemaDetailsProvider()).thenReturn(schemaDetailsProvider);

        sut = new TestCacheClusterService("test-node", tracingService, cacheManagerFactory);

        setTimerServiceWithReflection();
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
        verify(cacheManager).takeSnapshot(snapshotPublication);
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

        @Override protected CreateCacheRequestDetails<ReusableString> getCreateCacheRequestDetails(ClientSession s, DirectBuffer b, int o) { return null; }
        @Override protected ClearCacheRequestDetails<ReusableString> getClearCacheRequestDetails(ClientSession s, DirectBuffer b, int o) { return null; }
        @Override protected RemoveCacheEntryRequestDetails<ReusableString, ReusableString> getRemoveCacheEntryRequestDetails(ClientSession s, DirectBuffer b, int o) { return null; }
        @Override protected AddCacheEntryRequestDetails<ReusableString, ReusableString, ReusableString> getAddCacheEntryRequestDetails(ClientSession s, DirectBuffer b, int o) { return null; }
        @Override protected GetCacheEntryRequestDetails<ReusableString, ReusableString> getCacheEntryRequestDetails(ClientSession s, DirectBuffer b, int o) { return null; }
        @Override protected GetAllCacheEntriesRequestDetails<ReusableString> getAllCacheEntriesRequestDetails(ClientSession s, DirectBuffer b, int o) { return null; }
        @Override protected DeleteCacheRequestDetails<ReusableString> getDeleteCacheRequestDetails(ClientSession s, DirectBuffer b, int o) { return null; }
        @Override protected GetCacheStatsRequestDetails getCacheStatsRequestDetails(ClientSession s, DirectBuffer b, int o) { return null; }
        @Override protected CacheSubscriptionRequestDetails<ReusableString> getCacheSubscriptionRequest(ClientSession s, DirectBuffer b, int o) { return null; }
        @Override protected CacheUnsubscribeRequestDetails<ReusableString> getCacheUnsubscribeRequest(ClientSession s, DirectBuffer b, int o) { return null; }
        @Override protected BulkCacheOpsRequestDetails<ReusableString, ReusableString, ReusableString> getBulkOpsRequest(ClientSession s, DirectBuffer b, int o) { return null; }
        @Override protected void handlePostCreateCache(ReusableString c, CreateCacheResult<ReusableString> r, ClientSession s) {}
        @Override protected void handlePostAddCacheEntry(ReusableString c, ReusableString k, ReusableString v, AddCacheEntryResult<ReusableString, ReusableString> r, ClientSession s) {}
        @Override protected void handlePostGetCacheEntry(ReusableString c, ReusableString k, GetCacheEntryResult<ReusableString, ReusableString, ReusableString> r, ClientSession s) {}
        @Override protected void handlePostGetAllCacheEntries(ReusableString c, GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString> r, ClientSession s) {}
        @Override protected void handlePostRemoveCacheEntry(ReusableString c, ReusableString k, RemoveCacheEntryResult<ReusableString, ReusableString> r, ClientSession s) {}
        @Override protected void handlePostRemoveTimerCacheEntry(ReusableString c, ReusableString k, RemoveCacheEntryResult<ReusableString, ReusableString> r) {}
        @Override protected void handlePostClearCache(ReusableString c, ClearCacheResult<ReusableString> r, ClientSession s) {}
        @Override protected void handlePostDeleteCache(ReusableString c, DeleteCacheResult<ReusableString> r, ClientSession s) {}
        @Override protected void handlePostGetCacheStats(CacheStatsResult<ReusableString> r, ClientSession s) {}
        @Override protected void handlePostCacheSubscriptionRequest(CacheSubscriptionResult<ReusableString, ReusableString, ReusableString> r, ClientSession s) {}
        @Override protected void handlePostCacheUnsubscribeRequest(CacheUnsubscribeResult<ReusableString> r, ClientSession s) {}
        @Override protected void handlePostBulkOpsRequest(BulkCacheOpsResult<ReusableString, ReusableString, ReusableString> r, ClientSession s) {}
    }
}