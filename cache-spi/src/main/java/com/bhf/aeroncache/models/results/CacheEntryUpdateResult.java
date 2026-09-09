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
public class CacheEntryUpdateResult<I extends Reusable, K extends Reusable, V extends Reusable> implements Reusable<CacheEntryUpdateResult<I, K, V>> {

    final I cacheId;
    final K key;
    final V value;
    final RequestId requestId = new RequestId();
    boolean patch = false;

    public String getRequestId() {
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    @Override
    public void clear() {
        cacheId.clear();
        requestId.clear();
        key.clear();
        value.clear();
        patch = false;
    }

    @Override
    public void copyFrom(CacheEntryUpdateResult<I, K, V> source) {
        cacheId.copyFrom(source.cacheId);
        requestId.copyFrom(source.requestId);
        key.copyFrom(source.key);
        value.copyFrom(source.value);
        patch = source.patch;
    }

    @Override
    public void copyFrom(Reusable<CacheEntryUpdateResult<I, K, V>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public CacheEntryUpdateResult<I, K, V> value() {
        return this;
    }
}
