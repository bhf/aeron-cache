package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.ReusableLong;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.function.Supplier;

@Getter
@Setter
@RequiredArgsConstructor
@Flyweight
public class CountersCacheOperationRequestDetails<I extends Reusable, K extends Reusable> implements Reusable<CountersCacheOperationRequestDetails<I,K>>{

    final RequestId requestId = new RequestId();
    CountersBulkOperationType operationType = CountersBulkOperationType.NONE;
    long ttl = 0;
    final I cacheId;
    final K key;
    final ReusableLong value;

    public CountersCacheOperationRequestDetails(Supplier<I> indexSupplier, Supplier<K> keySupplier) {
        cacheId = indexSupplier.get();
        key = keySupplier.get();
        value = new ReusableLong();
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
        operationType = CountersBulkOperationType.NONE;
        ttl = 0;
        cacheId.clear();
        key.clear();
        value.clear();
    }

    @Override
    public void copyFrom(CountersCacheOperationRequestDetails<I, K> source) {
        this.requestId.copyFrom(source.requestId);
        this.operationType = source.operationType;
        this.ttl = source.getTtl();
        this.cacheId.copyFrom(source.cacheId);
        this.key.copyFrom(source.key);
        this.value.copyFrom(source.value);
    }

    @Override
    public void copyFrom(Reusable<CountersCacheOperationRequestDetails<I, K>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public CountersCacheOperationRequestDetails<I, K> value() {
        return this;
    }
}
