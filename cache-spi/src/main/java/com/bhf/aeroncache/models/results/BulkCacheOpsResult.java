package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.requests.CacheOperationRequestDetails;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class BulkCacheOpsResult <I extends Reusable, K extends Reusable, V extends Reusable> implements Reusable<BulkCacheOpsResult<I,K,V>>{

    @Getter
    final List<CacheOperationResultDetails<I,K,V>> operations = new ArrayList<>();
    final RequestId requestId = new RequestId();
    final Supplier<I> indexSupplier;
    final Supplier<K> keySupplier;
    final Supplier<V> valueSupplier;


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
    public void copyFrom(BulkCacheOpsResult<I, K, V> source) {
        this.requestId.copyFrom(source.requestId);
        this.operations.addAll(source.operations);
    }

    @Override
    public void copyFrom(Reusable<BulkCacheOpsResult<I, K, V>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public BulkCacheOpsResult<I, K, V> value() {
        return this;
    }


    public void addResult(DeleteCacheResult<I> result) {
        var cacheOpResult = new CacheOperationResultDetails<>(indexSupplier, keySupplier, valueSupplier);
        cacheOpResult.requestId.copyFrom(result.requestId);
        cacheOpResult.getCacheId().copyFrom(result.getCacheId());
        cacheOpResult.operationStatus = result.status;
        operations.add(cacheOpResult);
    }

    public void addResult(ClearCacheResult<I> result) {
        var cacheOpResult = new CacheOperationResultDetails<>(indexSupplier, keySupplier, valueSupplier);
        cacheOpResult.requestId.copyFrom(result.requestId);
        cacheOpResult.getCacheId().copyFrom(result.getCacheId());
        cacheOpResult.operationStatus = result.status;
        operations.add(cacheOpResult);
    }

    public void addResult(CreateCacheResult<I> result) {
        var cacheOpResult = new CacheOperationResultDetails<>(indexSupplier, keySupplier, valueSupplier);
        cacheOpResult.requestId.copyFrom(result.requestId);
        cacheOpResult.getCacheId().copyFrom(result.getCacheId());
        cacheOpResult.operationStatus = result.status;
        operations.add(cacheOpResult);
    }

    public void addResult(RemoveCacheEntryResult<I, K> result) {
        var cacheOpResult = new CacheOperationResultDetails<>(indexSupplier, keySupplier, valueSupplier);
        cacheOpResult.requestId.copyFrom(result.requestId);
        cacheOpResult.getCacheId().copyFrom(result.getCacheId());
        cacheOpResult.operationStatus = result.status;
        cacheOpResult.key.copyFrom(result.getKey());
        operations.add(cacheOpResult);
    }

    public void addResult(IncrementCounterResult<I, K> result) {
        var cacheOpResult = new CacheOperationResultDetails<>(indexSupplier, keySupplier, valueSupplier);
        cacheOpResult.requestId.copyFrom(result.requestId);
        cacheOpResult.getCacheId().copyFrom(result.getCacheId());
        cacheOpResult.operationStatus = result.status;
        cacheOpResult.getKey().copyFrom(result.getKey());
        operations.add(cacheOpResult);
    }

    public void addResult(DecrementCounterResult<I, K> result) {
        var cacheOpResult = new CacheOperationResultDetails<>(indexSupplier, keySupplier, valueSupplier);
        cacheOpResult.requestId.copyFrom(result.requestId);
        cacheOpResult.getCacheId().copyFrom(result.getCacheId());
        cacheOpResult.operationStatus = result.status;
        cacheOpResult.getKey().copyFrom(result.getKey());
        operations.add(cacheOpResult);
    }

    public void addResult(SetCounterResult<I, K> result) {
        var cacheOpResult = new CacheOperationResultDetails<>(indexSupplier, keySupplier, valueSupplier);
        cacheOpResult.requestId.copyFrom(result.requestId);
        cacheOpResult.getCacheId().copyFrom(result.getCacheId());
        cacheOpResult.operationStatus = result.status;
        cacheOpResult.getKey().copyFrom(result.getKey());
        operations.add(cacheOpResult);
    }

    public void addResult(AddCacheEntryResult<I, K> result) {
        var cacheOpResult = new CacheOperationResultDetails<>(indexSupplier, keySupplier, valueSupplier);
        cacheOpResult.requestId.copyFrom(result.requestId);
        cacheOpResult.getCacheId().copyFrom(result.getCacheId());
        cacheOpResult.operationStatus = result.status;
        cacheOpResult.getKey().copyFrom(result.getEntryKey());
        operations.add(cacheOpResult);
    }

    public <VT extends Reusable> void addResult(GetCacheEntryResult<I, K, VT> result) {
        var cacheOpResult = new CacheOperationResultDetails<>(indexSupplier, keySupplier, valueSupplier);
        cacheOpResult.requestId.copyFrom(result.requestId);
        cacheOpResult.getCacheId().copyFrom(result.getCacheId());
        cacheOpResult.operationStatus = result.status;
        cacheOpResult.getKey().copyFrom(result.getEntryKey());
        cacheOpResult.getValue().copyFrom(result.getEntryValue());
        operations.add(cacheOpResult);
    }

    public <VT extends Reusable> void addResult(PatchValueResult<I, K, VT> result) {
        var cacheOpResult = new CacheOperationResultDetails<>(indexSupplier, keySupplier, valueSupplier);
        cacheOpResult.requestId.copyFrom(result.requestId);
        cacheOpResult.getCacheId().copyFrom(result.getCacheId());
        cacheOpResult.operationStatus = result.status;
        cacheOpResult.getKey().copyFrom(result.getEntryKey());
        operations.add(cacheOpResult);
    }

    public void addOperationResult(CacheOperationStatus cacheOperationStatus, String requestId, I cacheId, K key, V value) {
        var cacheOpResult = new CacheOperationResultDetails<>(indexSupplier, keySupplier, valueSupplier);
        cacheOpResult.requestId.setRequestId(requestId);
        cacheOpResult.getCacheId().copyFrom(cacheId);
        cacheOpResult.operationStatus = cacheOperationStatus;
        cacheOpResult.getKey().copyFrom(key);
        cacheOpResult.getValue().copyFrom(value);
        operations.add(cacheOpResult);
    }
}
