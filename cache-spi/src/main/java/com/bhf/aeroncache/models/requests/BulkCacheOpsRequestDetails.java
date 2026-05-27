package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.bulk.requests.BulkOperationType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
@Flyweight
public class BulkCacheOpsRequestDetails <I extends Reusable, K extends Reusable, V extends Reusable> implements Reusable<BulkCacheOpsRequestDetails<I,K,V>>{

    final List<CacheOperationRequestDetails<I,K,V>> operations = new ArrayList<>();

    final RequestId requestId = new RequestId();

    public String getRequestId(){
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    @Override
    public void clear() {
        requestId.clear();
        operations.clear();
    }

    @Override
    public void copyFrom(BulkCacheOpsRequestDetails<I,K,V> source) {
        this.requestId.copyFrom(source.requestId);
        this.operations.addAll(source.getOperations());
    }

    @Override
    public void copyFrom(Reusable<BulkCacheOpsRequestDetails<I,K,V>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public BulkCacheOpsRequestDetails<I,K,V> value() {
        return this;
    }

    public void addOperation(BulkOperationType opType, long ttl, String requestId, String cacheId, String key, String value) {

    }
}
