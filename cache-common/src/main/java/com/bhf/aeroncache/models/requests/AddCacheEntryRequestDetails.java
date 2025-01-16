package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

/**
 * Decoded version of a request to add a cache entry.
 *
 * @param <I> The type the caches are indexed on.
 * @param <K> The type the entries of the caches are indexed on.
 * @param <V> The type of the values held in individual caches.
 */
@Getter
@Setter
public class AddCacheEntryRequestDetails<I, K, V> implements Reusable<AddCacheEntryRequestDetails<I, K, V>> {

    I cacheId;
    K key;
    V value;

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        cacheId = null;
        key = null;
        value = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(AddCacheEntryRequestDetails<I, K, V> source) {
        this.cacheId = source.getCacheId();
        this.key = source.getKey();
        this.value = source.value;
    }
}
