package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.pool.DequeReusableObjectPool;
import com.bhf.aeroncache.pool.ReusableObjectPool;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class BulkCacheOpsResult <I extends Reusable, K extends Reusable, V extends Reusable> implements Reusable<BulkCacheOpsResult<I,K,V>>{

    private static final int OPERATION_RESULT_POOL_INITIAL_SIZE = 16;

    @Getter
    final List<CacheOperationResultDetails<I,K,V>> operations = new ArrayList<>();
    final RequestId requestId = new RequestId();
    @Getter
    @Setter
    boolean endOfBatch = true;
    final Supplier<I> indexSupplier;
    final Supplier<K> keySupplier;
    final Supplier<V> valueSupplier;

    private final ReusableObjectPool<CacheOperationResultDetails<I, K, V>> operationPool;

    /**
     * Create a bulk result flyweight backed by a pool that reuses the operation result instances
     * gathered while building the response.
     *
     * @param indexSupplier Factory for cache id instances.
     * @param keySupplier   Factory for key instances.
     * @param valueSupplier Factory for value instances.
     */
    public BulkCacheOpsResult(final Supplier<I> indexSupplier, final Supplier<K> keySupplier, final Supplier<V> valueSupplier) {
        this.indexSupplier = indexSupplier;
        this.keySupplier = keySupplier;
        this.valueSupplier = valueSupplier;
        this.operationPool = new DequeReusableObjectPool<>(
                () -> new CacheOperationResultDetails<>(indexSupplier, keySupplier, valueSupplier),
                OPERATION_RESULT_POOL_INITIAL_SIZE, true);
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
        recycle();
        endOfBatch = true;
    }

    private void recycle() {
        for (CacheOperationResultDetails<I, K, V> operation : operations) {
            operationPool.release(operation);
        }
        operations.clear();
    }

    @Override
    public void copyFrom(BulkCacheOpsResult<I, K, V> source) {
        this.requestId.copyFrom(source.requestId);
        this.operations.addAll(source.operations);
        this.endOfBatch = source.endOfBatch;
    }

    @Override
    public void copyFrom(Reusable<BulkCacheOpsResult<I, K, V>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public BulkCacheOpsResult<I, K, V> value() {
        return this;
    }

    /**
     * Acquire a cleared operation result from the pool and populate the fields common to every bulk
     * operation: the operation status, request id and cache id.
     */
    private CacheOperationResultDetails<I, K, V> acquireResult(CacheOperationStatus operationStatus, String requestId, I cacheId) {
        var cacheOpResult = operationPool.acquire();
        cacheOpResult.setRequestId(requestId);
        cacheOpResult.getCacheId().copyFrom(cacheId);
        cacheOpResult.operationStatus = operationStatus;
        return cacheOpResult;
    }

    /**
     * Add the result of a cache-level operation (create, clear, delete) that carries no key or value.
     */
    public void addResult(CacheOperationStatus operationStatus, String requestId, I cacheId) {
        operations.add(acquireResult(operationStatus, requestId, cacheId));
    }

    /**
     * Add the result of a keyed operation (add, remove, patch, cancel, counter ops) that carries no value.
     */
    public void addResult(CacheOperationStatus operationStatus, String requestId, I cacheId, K key) {
        var cacheOpResult = acquireResult(operationStatus, requestId, cacheId);
        cacheOpResult.getKey().copyFrom(key);
        operations.add(cacheOpResult);
    }

    /**
     * Add the result of a keyed read operation whose value is carried as its string form. The bulk
     * result carries a single (string) value type, so a read value of a different type (e.g. a numeric
     * counter) is marshalled as its string form, exactly as the non-bulk GET path returns it.
     */
    public void addResult(CacheOperationStatus operationStatus, String requestId, I cacheId, K key, String value) {
        var cacheOpResult = acquireResult(operationStatus, requestId, cacheId);
        cacheOpResult.getKey().copyFrom(key);
        if (value != null) {
            cacheOpResult.getValue().copyFrom(value);
        }
        operations.add(cacheOpResult);
    }

    public void addOperationResult(CacheOperationStatus cacheOperationStatus, String requestId, I cacheId, K key, V value) {
        var cacheOpResult = acquireResult(cacheOperationStatus, requestId, cacheId);
        cacheOpResult.getKey().copyFrom(key);
        cacheOpResult.getValue().copyFrom(value);
        operations.add(cacheOpResult);
    }
}
