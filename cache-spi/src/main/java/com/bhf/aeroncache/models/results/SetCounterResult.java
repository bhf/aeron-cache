package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@Flyweight
public class SetCounterResult<I extends Reusable, K extends Reusable> implements Reusable<SetCounterResult<I, K>> {

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

    @Override
    public void clear() {
        key.clear();
        cacheId.clear();
        requestId.clear();
        status = CacheOperationStatus.NONE;
        counterValue = 0;
    }

    @Override
    public void copyFrom(SetCounterResult<I, K> source) {
        this.key.copyFrom(source.key);
        this.cacheId.copyFrom(source.cacheId);
        this.requestId.copyFrom(source.requestId);
        this.status = source.status;
        this.counterValue = source.getCounterValue();
    }

    @Override
    public void copyFrom(Reusable<SetCounterResult<I, K>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public SetCounterResult<I, K> value() {
        return this;
    }
}
