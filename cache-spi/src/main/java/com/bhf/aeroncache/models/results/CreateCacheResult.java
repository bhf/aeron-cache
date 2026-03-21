package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.messages.CacheOperationStatus;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * The result of a request to create a cache.
 *
 * @param <I> The type the cache is indexed on.
 */
@Getter
@Setter
@RequiredArgsConstructor
@Flyweight
public class CreateCacheResult<I extends Reusable> implements Reusable<CreateCacheResult<I>> {

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
        cacheId.clear();
        requestId.clear();
        status = CacheOperationStatus.NONE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(CreateCacheResult<I> source) {
        this.cacheId.copyFrom(source.cacheId);
        this.requestId.copyFrom(source.requestId);
        this.status = source.status;
    }

    @Override
    public void copyFrom(Reusable<CreateCacheResult<I>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public CreateCacheResult<I> value() {
        return this;
    }
}
