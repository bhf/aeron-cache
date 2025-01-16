package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

/**
 * Decoded version of a request to create a new cache.
 *
 * @param <I> The type the caches are indexed on.
 */
@Getter
@Setter
public class CreateCacheRequestDetails<I> implements Reusable<CreateCacheRequestDetails<I>> {

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
    public void copyFrom(CreateCacheRequestDetails<I> source) {
        this.cacheId = source.getCacheId();
    }
}
