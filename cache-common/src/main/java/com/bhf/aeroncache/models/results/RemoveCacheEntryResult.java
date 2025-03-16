package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * The result of a request to remove a cache entry.
 *
 * @param <I> The type of the cache ID.
 * @param <K> The type of the key for the entry that has been removed.
 */
@Getter
@Setter
@RequiredArgsConstructor
@Flyweight
public class RemoveCacheEntryResult<I extends Reusable, K extends Reusable> implements Reusable<RemoveCacheEntryResult<I, K>> {

    final I cacheId;
    final K key;
    boolean removed = false;
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
        key.clear();
        cacheId.clear();
        requestId.clear();
        removed = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(RemoveCacheEntryResult<I, K> source) {
        this.key.copyFrom(source.key);
        this.cacheId.copyFrom(source.cacheId);
        this.requestId.copyFrom(source.requestId);
        this.removed = source.removed;
    }

    @Override
    public void copyFrom(Reusable<RemoveCacheEntryResult<I, K>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public RemoveCacheEntryResult<I, K> value() {
        return this;
    }
}
