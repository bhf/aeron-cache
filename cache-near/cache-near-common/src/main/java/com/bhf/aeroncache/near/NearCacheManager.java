package com.bhf.aeroncache.near;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;

import java.util.Set;

/**
 * Manages a set of named {@link NearCache}s holding values of type {@code V}, keeping them in sync
 * with cache update events.
 */
public interface NearCacheManager<V> {

    void put(String cacheId, String key, V value);

    boolean contains(String cacheId);

    NearCache<V> get(String cacheId);

    void handleCacheUpdate(CacheUpdateEvent<V> cacheUpdateEvent);

    /**
     * Apply a cache update from its decomposed fields rather than a {@link CacheUpdateEvent} record.
     *
     * <p>Equivalent to {@link #handleCacheUpdate(CacheUpdateEvent)} but allocation-free: the Aeron gateway
     * dispatches streaming updates through a reused flyweight ({@code GatewayStreamUpdate}) on its single
     * egress thread rather than allocating a record per event, so it feeds the near cache through this
     * overload to keep its hot path free of per-update garbage.
     *
     * @param cacheId   the cache the update applies to.
     * @param eventType the kind of update.
     * @param itemKey   the item key (may be {@code null} for cache-wide events).
     * @param itemValue the item value (may be {@code null}).
     */
    void handleCacheUpdate(String cacheId, CacheUpdateEvent.EventType eventType, String itemKey, V itemValue);

    /**
     * The IDs of every cache currently held in the near cache.
     *
     * @return a snapshot of the warm cache IDs.
     */
    Set<String> cacheIds();

    /**
     * Drop the local copy of a cache, if present.
     *
     * @param cacheId the cache to remove.
     * @return true if a local copy existed and was removed.
     */
    boolean remove(String cacheId);
}
