package com.bhf.aeroncache.codecs;

import com.bhf.aeroncache.models.PendingRemove;
import com.bhf.aeroncache.models.Reusable;
import io.aeron.cluster.service.Cluster;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.Long2ObjectHashMap;

public interface CacheTimersCodec<I extends Reusable, K extends Reusable> {

    int encodeCacheTimer(MutableDirectBuffer timersBuffer, int offset, long timerCorrelationId, K key, I cacheId);

    void decodeCacheTimers(int timersSize, Long2ObjectHashMap<PendingRemove<I, K>> pendingRemoves, DirectBuffer buffer, int offset, Cluster cluster);
}
