package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetCacheEntryResult<I, K, V> implements Reusable<GetCacheEntryResult<I, K, V>> {

    I cacheId;
    K entryKey;
    V entryValue;
    final RequestId requestId = new RequestId();

    public String getRequestId() {
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    @Override
    public void clear() {
        cacheId = null;
        entryKey = null;
        entryValue = null;
        requestId.clear();
    }

    @Override
    public void copyFrom(GetCacheEntryResult<I, K, V> source) {
        this.cacheId = source.cacheId;
        this.entryKey = source.entryKey;
        this.entryValue = source.entryValue;
        this.requestId.copyFrom(source.requestId);
    }
}
