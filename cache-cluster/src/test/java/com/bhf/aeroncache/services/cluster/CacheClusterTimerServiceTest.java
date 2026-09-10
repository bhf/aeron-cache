package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.CacheTimersCodec;
import com.bhf.aeroncache.services.cache.Cache;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.types.ReusableString;
import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.cluster.service.Cluster;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyInt;

@ExtendWith(MockitoExtension.class)
class CacheClusterTimerServiceTest {

    private CacheClusterTimerService<ReusableString, ReusableString, ReusableString> sut;

    @Mock
    private CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory;

    @Mock
    private Cluster cluster;

    @Mock
    private TimerDetailsFlyweight<ReusableString, ReusableString> timerDetailsFlyweight;

    @Mock
    private Consumer<TimerDetailsFlyweight<ReusableString, ReusableString>> removeConsumer;

    @Mock
    private CacheTimersCodec<ReusableString, ReusableString> cacheTimersCodec;

    @Mock
    private Cache<ReusableString, ReusableString, ReusableString> cache;

    @BeforeEach
    void setUp() {
        Supplier<ReusableString> stringSupplier = ReusableString::new;
        when(cacheManagerFactory.getIndexSupplier()).thenReturn(stringSupplier);
        when(cacheManagerFactory.getKeySupplier()).thenReturn(stringSupplier);
        when(cacheManagerFactory.getCacheTimersCodec()).thenReturn(cacheTimersCodec);

        sut = new CacheClusterTimerService<>(cacheManagerFactory.getIndexSupplier(), cacheManagerFactory.getKeySupplier(),
                cacheManagerFactory.getCacheTimersCodec(), new TimerCorrelationIdProvider(), cluster, timerDetailsFlyweight, removeConsumer);
    }

    @Test
    @HappyPath
    @DisplayName("Should schedule item removal")
    void shouldScheduleItemRemoval() {
        // Arrange
        ReusableString cacheId = new ReusableString();
        cacheId.copyFrom("cache1");
        ReusableString key = new ReusableString();
        key.copyFrom("key1");
        long deadline = 1000L;

        when(cluster.scheduleTimer(anyLong(), eq(deadline))).thenReturn(true);

        // Act
        sut.scheduleItemRemoval(cacheId, key, cache, deadline);

        // Assert
        verify(cluster).scheduleTimer(1L, deadline);
    }

    @Test
    @HappyPath
    @DisplayName("Should cancel existing timer on same key")
    void shouldCancelExistingTimerIfRescheduled() {
        // Arrange
        ReusableString cacheId = new ReusableString();
        cacheId.copyFrom("cache1");
        ReusableString key = new ReusableString();
        key.copyFrom("key1");
        long deadline1 = 1000L;
        long deadline2 = 2000L;

        when(cluster.scheduleTimer(anyLong(), anyLong())).thenReturn(true);

        // Act
        sut.scheduleItemRemoval(cacheId, key, cache, deadline1);
        sut.scheduleItemRemoval(cacheId, key, cache, deadline2);

        // Assert
        verify(cluster).scheduleTimer(1L, deadline1);
        verify(cluster).cancelTimer(1L);
        verify(cluster).scheduleTimer(2L, deadline2);
    }

    @Test
    @HappyPath
    @DisplayName("Should process timer event and call the remove consumer")
    void shouldProcessTimerEvent() {
        // Arrange
        ReusableString cacheId = new ReusableString();
        cacheId.copyFrom("cache1");
        ReusableString key = new ReusableString();
        key.copyFrom("key1");
        long deadline = 1000L;
        
        when(cluster.scheduleTimer(anyLong(), eq(deadline))).thenReturn(true);
        sut.scheduleItemRemoval(cacheId, key, cache, deadline);

        // Act
        sut.onTimerEvent(1L, deadline);

        // Assert
        verify(timerDetailsFlyweight).setCache(any(ReusableString.class));
        verify(timerDetailsFlyweight).setKey(any(ReusableString.class));
        verify(timerDetailsFlyweight).setCorrelationId(1L);
        verify(removeConsumer).accept(timerDetailsFlyweight);
    }
    
    @Test
    @HappyPath
    @DisplayName("Should snapshot scheduled timers")
    void shouldTakeSnapshot() {
        // Arrange
        ReusableString cacheId = new ReusableString();
        cacheId.copyFrom("cache1");
        ReusableString key = new ReusableString();
        key.copyFrom("key1");
        long deadline = 1000L;
        
        when(cluster.scheduleTimer(anyLong(), eq(deadline))).thenReturn(true);
        sut.scheduleItemRemoval(cacheId, key, cache, deadline);

        when(cacheTimersCodec.encodeCacheTimer(any(), anyInt(), anyLong(), any(), any())).thenReturn(10);
        
        ExclusivePublication snapshotPublication = mock(ExclusivePublication.class);
        MutableDirectBuffer buffer = new UnsafeBuffer(new byte[128]);
        
        // Act
        int length = sut.onTakeSnapshot(snapshotPublication, buffer);
        
        // Assert
        assertEquals(1, buffer.getInt(0));
        assertEquals(14, length);
        verify(cacheTimersCodec).encodeCacheTimer
                (eq(buffer), eq(4), eq(1L), any(ReusableString.class), any(ReusableString.class));
    }
    
    @Test
    @HappyPath
    @DisplayName("Should load snapshot of scheduled timer")
    void shouldLoadSnapshot() {
        // Arrange
        Image snapshotImage = mock(Image.class);
        
        doAnswer(invocation -> {
            FragmentHandler handler = invocation.getArgument(0);
            var timersSize = 1;
            MutableDirectBuffer buffer = new UnsafeBuffer(new byte[128]);
            buffer.putInt(0, timersSize);
            
            handler.onFragment(buffer, 0, 128, null);
            return 1;
        }).when(snapshotImage).poll(any(FragmentHandler.class), anyInt());
        
        // Act
        sut.loadSnapshot(cluster, snapshotImage);
        
        // Assert
        verify(cacheTimersCodec).decodeCacheTimers(eq(1), any(), any(), eq(4), cluster);
    }
}
