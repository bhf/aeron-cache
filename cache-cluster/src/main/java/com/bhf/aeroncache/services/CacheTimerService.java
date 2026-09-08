package com.bhf.aeroncache.services;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.services.cache.Cache;
import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.cluster.service.Cluster;
import org.agrona.MutableDirectBuffer;

public interface CacheTimerService<I extends Reusable,K extends Reusable> {

    /**
     * Schedule an item for removal.
     * @param cacheId
     * @param key
     * @param cache
     * @param deadline
     */
    <CT extends Reusable> void scheduleItemRemoval(I cacheId, K key, Cache<I, K, CT> cache, long deadline);

    /**
     * Cancel a scheduled item removal.
     * @param cacheId
     * @param key
     * @return true if the item removal was cancelled, false if it was not found or already removed.
     */
    boolean cancelItemRemoval(I cacheId, K key);

    /**
     * Take a snapshot of the timers and related metadata.
     * @param snapshotPublication
     */
    int onTakeSnapshot(final ExclusivePublication snapshotPublication, MutableDirectBuffer timersBuffer);

    /**
     * Load the cache timers from the snapshot.
     * @param cluster
     * @param snapshotImage
     */
    void loadSnapshot(final Cluster cluster, final Image snapshotImage);

    /**
     * Handle a timer event.
     *
     * @param correlationId
     * @param timestamp
     */
    void onTimerEvent(final long correlationId, final long timestamp);
}
