package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

/**
 * The result of a request to create a cache.
 *
 * @param <I> The type the cache is indexed on.
 */
@Getter
@Setter
public class CreateCacheResult<I> implements Reusable<CreateCacheResult<I>> {

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
    public void copyFrom(CreateCacheResult<I> source) {
        this.cacheId = source.cacheId;
    }
}
