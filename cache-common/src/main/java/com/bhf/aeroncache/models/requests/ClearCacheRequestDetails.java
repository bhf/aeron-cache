package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

/**
 * Decoded version of a request to clear a particular cache.
 *
 * @param <I> The type the caches are indexed on.
 */
@Getter
@Setter
public class ClearCacheRequestDetails<I> implements Reusable<ClearCacheRequestDetails<I>> {

    I cacheId;

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        cacheId = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(ClearCacheRequestDetails<I> source) {
        this.cacheId = source.getCacheId();
    }
}
