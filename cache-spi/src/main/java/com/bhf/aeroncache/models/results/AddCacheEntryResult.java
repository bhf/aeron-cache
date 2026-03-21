package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.messages.OperationStatus;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * The result of a request to make an addition to a cache.
 *
 * @param <I> The type of the cache ID.
 * @param <K> The type of the key used in the cache.
 */
@Getter
@Setter
@RequiredArgsConstructor
@Flyweight
public class AddCacheEntryResult<I extends Reusable, K extends Reusable> implements Reusable<AddCacheEntryResult<I, K>> {

    I cacheId;
    boolean entryAdded;
    K entryKey;
    final RequestId requestId = new RequestId();
    OperationStatus status = OperationStatus.NONE;

    public AddCacheEntryResult(I cacheID, K entryKey) {
        this.cacheId = cacheID;
        this.entryKey = entryKey;
    }

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
        entryAdded = false;
        entryKey.clear();
        cacheId.clear();
        requestId.clear();
        status = OperationStatus.NONE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(AddCacheEntryResult<I, K> source) {
        this.entryAdded = source.entryAdded;
        this.entryKey.copyFrom(source.entryKey);
        this.cacheId.copyFrom(source.cacheId);
        this.requestId.copyFrom(source.requestId);
        this.status = source.status;
    }

    @Override
    public void copyFrom(Reusable<AddCacheEntryResult<I, K>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public AddCacheEntryResult<I, K> value() {
        return this;
    }
}
