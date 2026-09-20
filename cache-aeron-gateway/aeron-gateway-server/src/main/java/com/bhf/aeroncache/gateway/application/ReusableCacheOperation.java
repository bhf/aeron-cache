package com.bhf.aeroncache.gateway.application;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.bulk.requests.BulkOperationType;
import com.bhf.aeroncache.models.bulk.requests.CacheOperation;

/**
 * A reusable {@link CacheOperation} flyweight used by {@link GatewayIngressAgent} when decoding an inbound
 * bulk request. The agent keeps a growable list of these and rebuilds it in place on every bulk request,
 * so no {@code CacheOperationRequest} is allocated per operation. Instances never escape the ingress agent
 * thread: the request is encoded onward to the cluster synchronously before the next request is decoded,
 * and the shared encode path reads operations only through {@link CacheOperation}.
 * <p>
 * This does not implement {@code Reusable} (and so is not held in a {@code ReusableObjectPool}) because
 * {@code CacheOperation#value()} returns the entry value, which would collide with {@code Reusable#value()}.
 * A straight indexed free-list is the simpler fit for a per-request clear-and-rebuild.
 */
@Flyweight
public class ReusableCacheOperation implements CacheOperation {

    private BulkOperationType operationType = BulkOperationType.NONE;
    private long ttl;
    private long counterValue;
    private String requestId;
    private String cacheId;
    private String key;
    private String value;

    /**
     * Populate every field of this operation from a decoded bulk request frame.
     */
    public void set(BulkOperationType operationType, long ttl, long counterValue,
                    String requestId, String cacheId, String key, String value) {
        this.operationType = operationType;
        this.ttl = ttl;
        this.counterValue = counterValue;
        this.requestId = requestId;
        this.cacheId = cacheId;
        this.key = key;
        this.value = value;
    }

    @Override
    public BulkOperationType operationType() {
        return operationType;
    }

    @Override
    public long ttl() {
        return ttl;
    }

    @Override
    public long counterValue() {
        return counterValue;
    }

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public String cacheId() {
        return cacheId;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String value() {
        return value;
    }
}
