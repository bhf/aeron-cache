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
public class CacheSubscriptionRequestDetails<I extends Reusable> implements Reusable<CacheSubscriptionRequestDetails<I>> {

    final RequestId requestId = new RequestId();
    final I cacheId;
    boolean sendSnapshot;

    public String getRequestId(){
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    @Override
    public void clear() {
        requestId.clear();
        cacheId.clear();
    }

    @Override
    public void copyFrom(CacheSubscriptionRequestDetails<I> source) {
        this.requestId.copyFrom(source.requestId);
        this.cacheId.copyFrom(source.cacheId);
    }

    @Override
    public void copyFrom(Reusable<CacheSubscriptionRequestDetails<I>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public CacheSubscriptionRequestDetails<I> value() {
        return this;
    }
}
