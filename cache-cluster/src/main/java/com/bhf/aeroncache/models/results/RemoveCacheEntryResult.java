package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.models.Reusable;

/**
 * The result of a request to remove a cache entry.
 *
 * @param <K> The type of the key for the entry that has been removed.
 */
public class RemoveCacheEntryResult<K> implements Reusable<RemoveCacheEntryResult<K>> {

    K key;

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        key=null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(RemoveCacheEntryResult<K> source) {
        this.key=source.key;
    }
}
