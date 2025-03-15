package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class GetCacheEntryRequestDetails<I extends Reusable, K extends Reusable> implements Reusable<GetCacheEntryRequestDetails<I,K>> {

    final I cacheId;
    final K key;
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
        this.key.clear();
        this.requestId.clear();
    }

    @Override
    public void copyFrom(GetCacheEntryRequestDetails<I, K> source) {
        this.cacheId.copyFrom(source.cacheId);
        this.key.copyFrom(source.key);
        this.requestId.copyFrom(source.requestId);
    }
}
