package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Decoded version of a request to remove a cache entry.
 *
 * @param <I> The type the caches are indexed on.
 * @param <K> The type the entries of the caches are indexed on.
 */
@Getter
@Setter
@RequiredArgsConstructor
public class RemoveCacheEntryRequestDetails<I extends Reusable, K extends Reusable> implements Reusable<RemoveCacheEntryRequestDetails<I, K>> {

    final I cacheId;
    final K key;
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
        key.clear();
        this.requestId.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(RemoveCacheEntryRequestDetails<I, K> source) {
        this.cacheId.copyFrom(source.getCacheId());
        this.key.copyFrom(source.getKey());
        this.requestId.copyFrom(source.requestId);
    }
}
