package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@RequiredArgsConstructor
@Flyweight
public class CacheSubscriptionResult<I extends Reusable, K extends Reusable, V extends Reusable> implements Reusable<CacheSubscriptionResult<I,K,V>> {

    final RequestId requestId = new RequestId();
    final I cacheId;
    public Map<K, V> entries = new HashMap<>();
    CacheOperationStatus status = CacheOperationStatus.NONE;
    boolean isEob;

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
        status = CacheOperationStatus.NONE;
        isEob = false;
    }

    @Override
    public void copyFrom(CacheSubscriptionResult<I,K,V> source) {
        this.requestId.copyFrom(source.requestId);
        this.cacheId.copyFrom(source.cacheId);
        this.status = source.status;
        this.isEob = source.isEob;
    }

    @Override
    public void copyFrom(Reusable<CacheSubscriptionResult<I,K,V>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public CacheSubscriptionResult<I,K,V> value() {
        return this;
    }
}
