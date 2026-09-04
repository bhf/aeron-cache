package com.bhf.aeroncache.cache.utils;

import com.bhf.aeroncache.codecs.ReusableStringTimersCodec;
import com.bhf.aeroncache.codecs.request.ReusableStringCacheRequestDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseEncoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCountersCacheRequestDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCountersCacheResponseEncoder;
import com.bhf.aeroncache.services.cache.snapshot.CountersCacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.ReusableStringCacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.ReusableStringCacheIdSnapshotCodec;
import com.bhf.aeroncache.services.cachemanager.MapCacheManagerFactory;
import com.bhf.aeroncache.services.integrity.NoOpMultiTypeStreamingHasher;
import com.bhf.aeroncache.services.integrity.NoOpStreamingHasher;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.Aeron;
import io.aeron.DirectBufferVector;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.cluster.service.ClusteredServiceContainer;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class BenchmarkUtils {

    public static MapCacheManagerFactory<ReusableString, ReusableString, ReusableString> getCacheManagerFactory() {
        var encoder = new ReusableStringCacheResponseEncoder();
        var decoder = new ReusableStringCacheRequestDecoder();
        var timersCodec = new ReusableStringTimersCodec(new NoOpMultiTypeStreamingHasher<>());
        var responseEncoder = new ReusableStringCountersCacheResponseEncoder();
        var requestDecoder = new ReusableStringCountersCacheRequestDecoder();
        return new MapCacheManagerFactory<>(SupplierUtils.stringSupplier,
                SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.mapSupplier,
                new ReusableStringCacheIdSnapshotCodec(new NoOpStreamingHasher<>()),
                new ReusableStringCacheEntrySnapshotCodec(new NoOpStreamingHasher<>()),
                new CountersCacheEntrySnapshotCodec(new NoOpStreamingHasher<>()),
                encoder,
                decoder,
                timersCodec, responseEncoder, requestDecoder);
    }

    public static ClientSession getMockedSession(MutableDirectBuffer responseBuffer) {
        return new ClientSession() {
            @Override public long id() { return 0; }
            @Override public int responseStreamId() { return 0; }
            @Override public String responseChannel() { return null; }
            @Override public byte[] encodedPrincipal() { return new byte[0]; }
            @Override public void close() {}
            @Override public boolean isClosing() { return false; }
            @Override public long offer(DirectBuffer buffer, int offset, int length) {
                responseBuffer.putBytes(0, buffer, offset, length);
                return length;
            }
            @Override public long offer(DirectBufferVector[] vectors) { return 0; }
            @Override public long tryClaim(int length, BufferClaim bufferClaim) { return 0; }
        };
    }

    public static Cluster getClusterStub() {
        final IdleStrategy idleStrat = new BusySpinIdleStrategy();
        return new Cluster() {
            @Override public int memberId() { return 0; }
            @Override public Role role() { return null; }
            @Override public long logPosition() { return 0; }
            @Override public Aeron aeron() { return null; }
            @Override public ClusteredServiceContainer.Context context() { return null; }
            @Override public ClientSession getClientSession(long clusterSessionId) { return null; }
            @Override public Collection<ClientSession> clientSessions() { return List.of(); }
            @Override public void forEachClientSession(Consumer<? super ClientSession> action) {}
            @Override public boolean closeClientSession(long clusterSessionId) { return false; }
            @Override public long time() { return 0; }
            @Override public TimeUnit timeUnit() { return null; }
            @Override public boolean scheduleTimer(long correlationId, long deadline) { return false; }
            @Override public boolean cancelTimer(long correlationId) { return false; }
            @Override public long offer(DirectBuffer buffer, int offset, int length) { return 0; }
            @Override public long offer(DirectBufferVector[] vectors) { return 0; }
            @Override public long tryClaim(int length, BufferClaim bufferClaim) { return 0; }
            @Override public IdleStrategy idleStrategy() { return idleStrat; }
        };
    }
}