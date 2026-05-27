package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.function.Supplier;

@Getter
@Setter
@RequiredArgsConstructor
@Flyweight
public class CacheOperationResultDetails<I extends Reusable, K extends Reusable, V extends Reusable> implements Reusable<CacheOperationResultDetails<I,K,V>>{

    final RequestId requestId = new RequestId();
    CacheOperationStatus operationStatus = CacheOperationStatus.NONE;
    final I cacheId;
    final K key;
    final V value;

    public CacheOperationResultDetails(Supplier<I> indexSupplier, Supplier<K> keySupplier, Supplier<V> valueSupplier) {
        cacheId = indexSupplier.get();
        key = keySupplier.get();
        value = valueSupplier.get();
    }

    public String getRequestId(){
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    @Override
    public void clear() {
        requestId.clear();
        operationStatus = CacheOperationStatus.NONE;
        cacheId.clear();
        key.clear();
        value.clear();
    }

    @Override
    public void copyFrom(CacheOperationResultDetails<I, K, V> source) {
        this.requestId.copyFrom(source.requestId);
        this.operationStatus = source.operationStatus;
        this.cacheId.copyFrom(source.cacheId);
        this.key.copyFrom(source.key);
        this.value.copyFrom(source.value);
    }

    @Override
    public void copyFrom(Reusable<CacheOperationResultDetails<I, K, V>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public CacheOperationResultDetails<I, K, V> value() {
        return this;
    }
}
