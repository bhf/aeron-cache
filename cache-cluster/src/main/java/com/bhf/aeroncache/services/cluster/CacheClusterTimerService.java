package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.codecs.CacheTimersCodec;
import com.bhf.aeroncache.models.PendingRemove;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.TimerLookupCompoundKey;
import com.bhf.aeroncache.services.CacheTimerService;
import com.bhf.aeroncache.services.cache.Cache;
import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.cluster.service.Cluster;
import io.aeron.logbuffer.FragmentHandler;
import lombok.extern.log4j.Log4j2;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.collections.MutableBoolean;
import org.agrona.collections.Object2ObjectHashMap;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Log4j2
public class CacheClusterTimerService<I extends Reusable,K extends Reusable,V extends Reusable> implements CacheTimerService<I,K,V> {

    private final Cluster cluster;
    private final Supplier<I> indexSupplier;
    private final Supplier<K> keySupplier;
    private final CacheTimersCodec<I, K> timersCodec;
    private final TimerCorrelationIdProvider timerCorrelationIdProvider;
    private final Long2ObjectHashMap<PendingRemove<I, K>> pendingRemoves = new Long2ObjectHashMap();
    private final Map<TimerLookupCompoundKey<I, K>, Long> cacheKeyToTimerId = new Object2ObjectHashMap<>();
    private final TimerLookupCompoundKey<I, K> lookupKey;
    private final Consumer<TimerDetailsFlyweight<I,K>> removeConsumer;
    private final TimerDetailsFlyweight<I, K> timerDetailsFlyweight;

    public CacheClusterTimerService(Supplier<I> indexSupplier, Supplier<K> keySupplier, CacheTimersCodec<I,K> timersCodec, TimerCorrelationIdProvider timerCorrelationIdProvider,
                                    Cluster cluster, TimerDetailsFlyweight<I,K> timerDetailsFlyweight, Consumer<TimerDetailsFlyweight<I,K>> remove) {
        this.lookupKey = new TimerLookupCompoundKey<>(indexSupplier.get(), keySupplier.get());
        this.indexSupplier = indexSupplier;
        this.keySupplier = keySupplier;
        this.cluster = cluster;
        this.removeConsumer = remove;
        this.timerDetailsFlyweight = timerDetailsFlyweight;
        this.timersCodec = timersCodec;
        this.timerCorrelationIdProvider = timerCorrelationIdProvider;
    }

    /**
     * Schedule the removal of an item from a cache.
     *
     * @param cacheId  The ID of the cache to remove from.
     * @param key      The key to remove.
     * @param cache    The cache to remove from.
     * @param deadline The epoch time at which to remove the item.
     */
    @Override
    public <CT extends Reusable> void scheduleItemRemoval(I cacheId, K key, Cache<I, K, CT> cache, long deadline) {
        lookupKey.clear();
        lookupKey.getCacheId().copyFrom(cacheId);
        lookupKey.getKey().copyFrom(key);

        var existingTimerId = cacheKeyToTimerId.remove(lookupKey);
        if (existingTimerId != null) {
            boolean timerCancelled = cluster.cancelTimer(existingTimerId);
            var removedItem = pendingRemoves.remove(existingTimerId);
            log.info("Cancelled existing timer {} for key {} in cache {}, cancelled: {}, removed: {}", existingTimerId, key, cacheId, timerCancelled, removedItem);
        }

        var timerCorrelationId = this.timerCorrelationIdProvider.getNextId();
        boolean success = cluster.scheduleTimer(timerCorrelationId, deadline);
        log.info("Scheduled timer for {} to remove key {} from cache {} correlationId {}", deadline, key, cacheId, timerCorrelationId);

        final var keyToRemove = keySupplier.get();
        keyToRemove.copyFrom(key);

        final var cacheToRemoveOn = indexSupplier.get();
        cacheToRemoveOn.copyFrom(cacheId);

        var pendingRemove = new PendingRemove(timerCorrelationId, cacheToRemoveOn, keyToRemove);
        pendingRemoves.put(timerCorrelationId, pendingRemove);
        cacheKeyToTimerId.put(new TimerLookupCompoundKey<>(cacheToRemoveOn, keyToRemove), timerCorrelationId);
    }

    @Override
    public int onTakeSnapshot(final ExclusivePublication snapshotPublication, MutableDirectBuffer timersBuffer) {
        log.info("Got request to take snapshot");

        var timersSize = pendingRemoves.keySet().size();

        timersBuffer.putInt(0, timersSize);

        log.info("Total timers to snapshot: {}", timersSize);
        int cumulativeLength = 4;
        if (timersSize > 0) {
            for (var t : pendingRemoves.keySet().stream().sorted().toList()) {
                var pendingTimer = pendingRemoves.get(t);
                var correlationId = pendingTimer.getTimerCorrelationId();
                var key = pendingTimer.getKeyToRemove();
                var cacheId = pendingTimer.getCacheToRemoveOn();
                var codec = timersCodec;
                int length = codec.encodeCacheTimer(timersBuffer, cumulativeLength, correlationId, key, cacheId);
                cumulativeLength += length;
            }
        }
        return cumulativeLength;
    }

    @Override
    public void loadSnapshot(final Cluster cluster, final Image snapshotImage) {
        log.info("Got request to load snapshot");
        MutableBoolean timersSnapshotFinished = new MutableBoolean(false);

        FragmentHandler handler = (buffer, offset, length, header) -> {
            int timersSize = buffer.getInt(offset);

            log.info("Total timers to load: {}", timersSize);

            if (timersSize > 0) {
                var codec = timersCodec;
                codec.decodeCacheTimers(timersSize, pendingRemoves, buffer, offset + 4);
                log.info("Loaded {} timers", pendingRemoves.size());

                for (var x : pendingRemoves.values()) {
                    log.info("Timer on cache {} key {}, correlationId {}", x.getCacheToRemoveOn(), x.getKeyToRemove(), x.getTimerCorrelationId());
                    cacheKeyToTimerId.put(new TimerLookupCompoundKey<>(x.getCacheToRemoveOn(), x.getKeyToRemove()), x.getTimerCorrelationId());
                }
            }

            timersSnapshotFinished.set(true);
        };

        while (!timersSnapshotFinished.get()) {
            snapshotImage.poll(handler, 1);
            if (timersSnapshotFinished.value) break;
        }
    }

    @Override
    public void onTimerEvent(final long correlationId, final long timestamp) {
        var pendingRemove = pendingRemoves.remove(correlationId);

        if (pendingRemove != null) {
            I cache = pendingRemove.getCacheToRemoveOn();
            K key = pendingRemove.getKeyToRemove();

            lookupKey.clear();
            lookupKey.getCacheId().copyFrom(cache);
            lookupKey.getKey().copyFrom(key);
            var timerId = cacheKeyToTimerId.remove(lookupKey);

            if(timerId!=null) {
                timerDetailsFlyweight.setCache(cache);
                timerDetailsFlyweight.setKey(key);
                timerDetailsFlyweight.setCorrelationId(correlationId);
                removeConsumer.accept(timerDetailsFlyweight);
            }
        }
    }

}
