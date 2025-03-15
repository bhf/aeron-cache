package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Decoded version of a request to clear a particular cache.
 *
 * @param <I> The type the caches are indexed on.
 */
@Getter
@Setter
@RequiredArgsConstructor
public class ClearCacheRequestDetails<I extends Reusable> implements Reusable<ClearCacheRequestDetails<I>> {

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
        this.cacheId.clear();
        this.requestId.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(ClearCacheRequestDetails<I> source) {
        this.cacheId.copyFrom(source.getCacheId());
        this.requestId.copyFrom(source.requestId);
    }
}
