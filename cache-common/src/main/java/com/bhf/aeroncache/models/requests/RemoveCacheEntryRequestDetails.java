package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

/**
 * Decoded version of a request to remove a cache entry.
 *
 * @param <I> The type the caches are indexed on.
 * @param <K> The type the entries of the caches are indexed on.
 */
@Getter
@Setter
public class RemoveCacheEntryRequestDetails<I, K> implements Reusable<RemoveCacheEntryRequestDetails<I, K>> {

    I cacheId;
    K key;

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        cacheId = null;
        key = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(RemoveCacheEntryRequestDetails<I, K> source) {
        this.cacheId = source.getCacheId();
        this.key = source.getKey();
    }
}
