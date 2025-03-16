package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Decoded version of a request to delete a cache.
 *
 * @param <I> The type the caches are indexed on.
 */
@Getter
@Setter
@RequiredArgsConstructor
@Flyweight
public class DeleteCacheRequestDetails<I extends Reusable> implements Reusable<DeleteCacheRequestDetails<I>> {

    final I cacheId;
    final RequestId requestId = new RequestId();

    public String getRequestId(){
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(DeleteCacheRequestDetails<I> source) {
        this.cacheId.copyFrom(source.cacheId);
        this.requestId.copyFrom(source.requestId);
    }

    @Override
    public void copyFrom(Reusable<DeleteCacheRequestDetails<I>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public DeleteCacheRequestDetails<I> value() {
        return this;
    }
}
