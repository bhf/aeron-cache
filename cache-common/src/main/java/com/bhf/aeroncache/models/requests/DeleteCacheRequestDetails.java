package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.models.RequestId;
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
        cacheId = null;
        requestId.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(DeleteCacheRequestDetails<I> source) {
        this.cacheId = source.cacheId;
        this.requestId.copyFrom(source.requestId);
    }
}
