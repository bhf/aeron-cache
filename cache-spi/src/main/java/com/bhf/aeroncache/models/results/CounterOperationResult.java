package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * The result of a request to do an operation on a counter cache.
 *
 * @param <I> The type of the cache ID.
 * @param <K> The type of the key for the entry that has been removed.
 */
@Getter
@Setter
@RequiredArgsConstructor
@Flyweight
public class CounterOperationResult<I extends Reusable, K extends Reusable> implements Reusable<CounterOperationResult<I, K>> {

    final I cacheId;
    final K key;
    final RequestId requestId = new RequestId();
    CacheOperationStatus status = CacheOperationStatus.NONE;
    long counterValue;

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
        status = CacheOperationStatus.NONE;
        counterValue = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(CounterOperationResult<I, K> source) {
        this.key.copyFrom(source.key);
        this.cacheId.copyFrom(source.cacheId);
        this.requestId.copyFrom(source.requestId);
        this.status = source.status;
        this.counterValue = source.getCounterValue();
    }

    @Override
    public void copyFrom(Reusable<CounterOperationResult<I, K>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public CounterOperationResult<I, K> value() {
        return this;
    }
}
