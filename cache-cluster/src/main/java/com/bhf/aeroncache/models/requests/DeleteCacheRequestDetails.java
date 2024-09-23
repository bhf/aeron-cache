package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

/**
 * Decoded version of a request to delete a cache.
 *
 * @param <I> The type the caches are indexed on.
 */
@Getter
@Setter
public class DeleteCacheRequestDetails<I> implements Reusable<DeleteCacheRequestDetails<I>> {

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
    public void copyFrom(DeleteCacheRequestDetails<I> source) {
        this.cacheId = source.cacheId;
    }
}
