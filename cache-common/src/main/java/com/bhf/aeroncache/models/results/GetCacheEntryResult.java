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
public class GetCacheEntryResult<I extends Reusable, K extends Reusable, V extends Reusable> implements Reusable<GetCacheEntryResult<I, K, V>> {

    final I cacheId;
    final K entryKey;
    final V entryValue;
    final RequestId requestId = new RequestId();

    public String getRequestId() {
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    @Override
    public void clear() {
        cacheId.clear();
        entryKey.clear();
        entryValue.clear();
        requestId.clear();
    }

    @Override
    public void copyFrom(GetCacheEntryResult<I, K, V> source) {
        this.cacheId.copyFrom(source.cacheId);
        this.entryKey.copyFrom(source.entryKey);
        this.entryValue.copyFrom(source.entryValue);
        this.requestId.copyFrom(source.requestId);
    }

    @Override
    public void copyFrom(Reusable<GetCacheEntryResult<I, K, V>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public GetCacheEntryResult<I, K, V> value() {
        return this;
    }
}
