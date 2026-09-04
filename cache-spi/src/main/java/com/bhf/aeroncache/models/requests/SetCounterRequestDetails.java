package com.bhf.aeroncache.models.requests;

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
public class SetCounterRequestDetails<I extends Reusable, K extends Reusable> implements Reusable<SetCounterRequestDetails<I, K>> {

    final I cacheId;
    final K counterId;
    long counterValue;
    long ttl;
    final RequestId requestId = new RequestId();

    public String getRequestId(){
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    @Override
    public void clear() {
        cacheId.clear();
        counterId.clear();
        this.counterValue = 0;
        this.ttl = 0;
        this.requestId.clear();
    }

    @Override
    public void copyFrom(SetCounterRequestDetails<I, K> source) {
        this.cacheId.copyFrom(source.getCacheId());
        this.counterId.copyFrom(source.getCounterId());
        this.counterValue = source.getCounterValue();
        this.ttl = source.getTtl();
        this.requestId.copyFrom(source.requestId);
    }

    @Override
    public void copyFrom(Reusable<SetCounterRequestDetails<I, K>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public SetCounterRequestDetails<I, K> value() {
        return this;
    }
}