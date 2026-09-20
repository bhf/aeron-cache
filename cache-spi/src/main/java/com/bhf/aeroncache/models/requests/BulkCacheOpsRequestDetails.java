package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.bulk.requests.BulkOperationType;
import com.bhf.aeroncache.pool.DequeReusableObjectPool;
import com.bhf.aeroncache.pool.ReusableObjectPool;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Getter
@Setter
@Flyweight
public class BulkCacheOpsRequestDetails <I extends Reusable, K extends Reusable, V extends Reusable> implements Reusable<BulkCacheOpsRequestDetails<I,K,V>>{

    private static final int OPERATION_POOL_INITIAL_SIZE = 16;

    final Supplier<I> indexSupplier;
    final Supplier<K> keySupplier;
    final Supplier<V> valueSupplier;

    final List<CacheOperationRequestDetails<I,K,V>> operations = new ArrayList<>();

    private final ReusableObjectPool<CacheOperationRequestDetails<I, K, V>> operationPool;

    final RequestId requestId = new RequestId();

    /**
     * Create a bulk request details flyweight backed by a pool that reuses the operation instances
     * gathered while decoding.
     *
     * @param indexSupplier Factory for cache id instances.
     * @param keySupplier   Factory for key instances.
     * @param valueSupplier Factory for value instances.
     */
    public BulkCacheOpsRequestDetails(final Supplier<I> indexSupplier, final Supplier<K> keySupplier, final Supplier<V> valueSupplier) {
        this.indexSupplier = indexSupplier;
        this.keySupplier = keySupplier;
        this.valueSupplier = valueSupplier;
        this.operationPool = new DequeReusableObjectPool<>(
                () -> new CacheOperationRequestDetails<>(indexSupplier, keySupplier, valueSupplier),
                OPERATION_POOL_INITIAL_SIZE, true);
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

    public void addOperation(BulkOperationType opType, long ttl, long counterValue, String requestId, I cacheId, K key, V value) {
        var details = operationPool.acquire();
        details.operationType = opType;
        details.ttl = ttl;
        details.counterValue = counterValue;
        details.setRequestId(requestId);
        details.getCacheId().copyFrom(cacheId);
        details.getKey().copyFrom(key);
        details.getValue().copyFrom(value);
        operations.add(details);
    }

    public void recycle() {
        for (CacheOperationRequestDetails<I, K, V> operation : operations) {
            operationPool.release(operation);
        }
        operations.clear();
    }

}
