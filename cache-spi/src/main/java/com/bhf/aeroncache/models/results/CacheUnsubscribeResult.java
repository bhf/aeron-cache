package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.messages.OperationStatus;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@Flyweight
public class CacheUnsubscribeResult<I extends Reusable> implements Reusable<CacheUnsubscribeResult<I>> {

    final RequestId requestId = new RequestId();
    final I cacheId;
    OperationStatus status = OperationStatus.NONE;

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
        status = OperationStatus.NONE;
    }

    @Override
    public void copyFrom(CacheUnsubscribeResult<I> source) {
        this.requestId.copyFrom(source.requestId);
        this.cacheId.copyFrom(source.cacheId);
        this.status = source.status;
    }

    @Override
    public void copyFrom(Reusable<CacheUnsubscribeResult<I>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public CacheUnsubscribeResult<I> value() {
        return this;
    }
}
