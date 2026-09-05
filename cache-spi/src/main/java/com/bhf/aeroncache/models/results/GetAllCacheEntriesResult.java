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
public class GetAllCacheEntriesResult<I extends Reusable, K extends Reusable, V extends Reusable> implements Reusable<GetAllCacheEntriesResult<I, K, V>> {

    final I cacheId;
    final Map<K,V> values=new HashMap<>();
    final RequestId requestId = new RequestId();
    CacheOperationStatus status = CacheOperationStatus.NONE;
    boolean endOfBatch = true;

    public String getRequestId() {
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    @Override
    public void clear() {
        cacheId.clear();
        values.clear();
        requestId.clear();
        status = CacheOperationStatus.NONE;
        endOfBatch = true;
    }

    @Override
    public void copyFrom(GetAllCacheEntriesResult<I, K, V> source) {
        this.cacheId.copyFrom(source.cacheId);
        this.values.putAll(source.values);
        this.requestId.copyFrom(source.requestId);
        this.status = source.status;
        this.endOfBatch = source.endOfBatch;
    }

    @Override
    public void copyFrom(Reusable<GetAllCacheEntriesResult<I, K, V>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public GetAllCacheEntriesResult<I, K, V> value() {
        return this;
    }
}
