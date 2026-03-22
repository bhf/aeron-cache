package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * The result of a request to delete a cache.
 *
 * @param <I> The type of the cache ID.
 */
@Getter
@Setter
@RequiredArgsConstructor
@Flyweight
public class DeleteCacheResult<I extends Reusable> implements Reusable<DeleteCacheResult<I>> {

    final I cacheId;
    final RequestId requestId = new RequestId();
    CacheOperationStatus status = CacheOperationStatus.NONE;

    public String getRequestId() {
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    @Override
    public void clear() {
        cacheId.clear();
        this.requestId.clear();
        status = CacheOperationStatus.NONE;
    }

    @Override
    public void copyFrom(DeleteCacheResult<I> source) {
        this.cacheId.copyFrom(source.cacheId);
        this.requestId.copyFrom(source.requestId);
        this.status = source.status;
    }

    @Override
    public void copyFrom(Reusable<DeleteCacheResult<I>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public DeleteCacheResult<I> value() {
        return this;
    }
}
