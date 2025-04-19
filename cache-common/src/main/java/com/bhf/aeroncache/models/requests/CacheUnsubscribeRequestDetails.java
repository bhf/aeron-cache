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
public class CacheUnsubscribeRequestDetails<I> implements Reusable<CacheUnsubscribeRequestDetails<I>> {

    final RequestId requestId = new RequestId();
    final I cacheId;

    public String getRequestId(){
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    @Override
    public void clear() {
        requestId.clear();
    }

    @Override
    public void copyFrom(CacheUnsubscribeRequestDetails<I> source) {
        this.requestId.copyFrom(source.requestId);
    }

    @Override
    public void copyFrom(Reusable<CacheUnsubscribeRequestDetails<I>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public CacheUnsubscribeRequestDetails<I> value() {
        return this;
    }
}
