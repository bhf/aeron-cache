package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * The result of a request to cancel a previously scheduled removal (TTL expiry) of a cache entry.
 *
 * @param <I> The type of the cache ID.
 * @param <K> The type of the key for the entry whose removal was cancelled.
 */
@Getter
@Setter
@RequiredArgsConstructor
@Flyweight
public class CancelItemRemovalResult<I extends Reusable, K extends Reusable> implements Reusable<CancelItemRemovalResult<I, K>> {

    final I cacheId;
    final K key;
    boolean cancelled = false;
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
        key.clear();
        cacheId.clear();
        requestId.clear();
        cancelled = false;
        status = CacheOperationStatus.NONE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(CancelItemRemovalResult<I, K> source) {
        this.key.copyFrom(source.key);
        this.cacheId.copyFrom(source.cacheId);
        this.requestId.copyFrom(source.requestId);
        this.cancelled = source.cancelled;
        this.status = source.status;
    }

    @Override
    public void copyFrom(Reusable<CancelItemRemovalResult<I, K>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public CancelItemRemovalResult<I, K> value() {
        return this;
    }
}
