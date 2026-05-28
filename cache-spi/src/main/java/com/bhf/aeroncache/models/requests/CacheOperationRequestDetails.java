package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.bulk.requests.BulkOperationType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.function.Supplier;

@Getter
@Setter
@RequiredArgsConstructor
@Flyweight
public class CacheOperationRequestDetails <I extends Reusable, K extends Reusable, V extends Reusable> implements Reusable<CacheOperationRequestDetails<I,K,V>>{

    final RequestId requestId = new RequestId();
    BulkOperationType operationType = BulkOperationType.NONE;
    long ttl = 0;
    final I cacheId;
    final K key;
    final V value;

    public CacheOperationRequestDetails(Supplier<I> indexSupplier, Supplier<K> keySupplier, Supplier<V> valueSupplier) {
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
        operationType = BulkOperationType.NONE;
        ttl = 0;
        cacheId.clear();
        key.clear();
        value.clear();
    }

    @Override
    public void copyFrom(CacheOperationRequestDetails<I, K, V> source) {
        this.requestId.copyFrom(source.requestId);
        this.operationType = source.operationType;
        this.ttl = source.getTtl();
        this.cacheId.copyFrom(source.cacheId);
        this.key.copyFrom(source.key);
        this.value.copyFrom(source.value);
    }

    @Override
    public void copyFrom(Reusable<CacheOperationRequestDetails<I, K, V>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public CacheOperationRequestDetails<I, K, V> value() {
        return this;
    }
}
