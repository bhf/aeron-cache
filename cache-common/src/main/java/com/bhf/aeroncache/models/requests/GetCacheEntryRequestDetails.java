package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetCacheEntryRequestDetails<I, K> implements Reusable<GetCacheEntryRequestDetails<I,K>> {

    I cacheId;
    K key;
    final RequestId requestId = new RequestId();

    public String getRequestId(){
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    @Override
    public void clear() {
        cacheId=null;
        key=null;
        this.requestId.clear();
    }

    @Override
    public void copyFrom(GetCacheEntryRequestDetails<I, K> source) {
        this.cacheId=source.cacheId;
        this.key=source.key;
        this.requestId.copyFrom(source.requestId);
    }
}
