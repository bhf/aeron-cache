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
public class CacheSubscriptionResult<I extends Reusable> implements Reusable<CacheSubscriptionResult<I>> {

    final RequestId requestId = new RequestId();
    final I cacheId;
    CacheOperationStatus status = CacheOperationStatus.NONE;

    public String getRequestId() {
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    @Override
    public void clear() {
        requestId.clear();
        cacheId.clear();
        status = CacheOperationStatus.NONE;
    }

    @Override
    public void copyFrom(CacheSubscriptionResult<I> source) {
        this.requestId.copyFrom(source.requestId);
        this.cacheId.copyFrom(source.cacheId);
        this.status = source.status;
    }

    @Override
    public void copyFrom(Reusable<CacheSubscriptionResult<I>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public CacheSubscriptionResult<I> value() {
        return this;
    }
}
