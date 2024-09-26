package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

/**
 * The result of a request to delete a cache.
 *
 * @param <I> The type of the cache ID.
 */
@Getter
@Setter
public class DeleteCacheResult<I> implements Reusable<DeleteCacheResult<I>> {

    I cacheId;

    @Override
    public void clear() {
        cacheId = null;
    }

    @Override
    public void copyFrom(DeleteCacheResult<I> source) {
        this.cacheId = source.cacheId;
    }
}
