package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

/**
 * The result of a request to remove a cache entry.
 *
 * @param <I> The type of the cache ID.
 * @param <K> The type of the key for the entry that has been removed.
 */
@Getter
@Setter
public class RemoveCacheEntryResult<I, K> implements Reusable<RemoveCacheEntryResult<I, K>> {

    I cacheId;
    K key;

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        key = null;
        cacheId = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(RemoveCacheEntryResult<I, K> source) {
        this.key = source.key;
        this.cacheId = source.cacheId;
    }
}
