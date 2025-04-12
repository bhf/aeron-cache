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
public class GetAllCacheEntriesRequestDetails<I extends Reusable> implements Reusable<GetAllCacheEntriesRequestDetails<I>> {

    final I cacheId;

    final RequestId requestId = new RequestId();

    public String getRequestId(){
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    @Override
    public void clear() {
        this.cacheId.clear();
        this.requestId.clear();
    }

    @Override
    public void copyFrom(GetAllCacheEntriesRequestDetails<I> source) {
        this.cacheId.copyFrom(source.cacheId);
        this.requestId.copyFrom(source.requestId);
    }

    @Override
    public void copyFrom(Reusable<GetAllCacheEntriesRequestDetails<I>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public GetAllCacheEntriesRequestDetails<I> value() {
        return this;
    }
}
