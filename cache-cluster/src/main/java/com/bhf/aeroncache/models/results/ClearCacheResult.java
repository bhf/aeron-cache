package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

/**
 * The result of a request to clear a cache.
 *
 * @param <I> The type the cache is indexed on.
 */
@Getter
@Setter
public class ClearCacheResult<I> implements Reusable<ClearCacheResult<I>> {

    I cacheId;

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        this.cacheId=null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(ClearCacheResult<I> source) {
        this.cacheId=source.cacheId;
    }
}
