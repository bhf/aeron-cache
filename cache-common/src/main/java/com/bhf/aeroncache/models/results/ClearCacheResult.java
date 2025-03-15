package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

/**
 * The result of a request to clear a cache.
 *
 * @param <I> The type of the cache ID.
 */
@Getter
@Setter
@Flyweight
public class ClearCacheResult<I> implements Reusable<ClearCacheResult<I>> {

    I cacheId;
    final RequestId requestId = new RequestId();

    public String getRequestId() {
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        this.cacheId = null;
        this.requestId.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(ClearCacheResult<I> source) {
        this.cacheId = source.cacheId;
        this.requestId.copyFrom(source.requestId);
    }
}
