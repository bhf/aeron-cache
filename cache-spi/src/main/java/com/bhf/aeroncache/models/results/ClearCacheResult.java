package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.messages.CacheOperationStatus;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * The result of a request to clear a cache.
 *
 * @param <I> The type of the cache ID.
 */
@Getter
@Setter
@RequiredArgsConstructor
@Flyweight
public class ClearCacheResult<I extends Reusable> implements Reusable<ClearCacheResult<I>> {

    final I cacheId;
    final RequestId requestId = new RequestId();
    CacheOperationStatus status = CacheOperationStatus.NONE;

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
        this.cacheId.clear();
        this.requestId.clear();
        this.status = CacheOperationStatus.NONE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(ClearCacheResult<I> source) {
        this.cacheId.copyFrom(source.cacheId);
        this.requestId.copyFrom(source.requestId);
        this.status = source.status;
    }

    @Override
    public void copyFrom(Reusable<ClearCacheResult<I>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public ClearCacheResult<I> value() {
        return this;
    }
}
