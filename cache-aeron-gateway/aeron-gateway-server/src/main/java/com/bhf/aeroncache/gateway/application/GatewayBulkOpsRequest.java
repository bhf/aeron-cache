package com.bhf.aeroncache.gateway.application;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.bulk.requests.BulkOpsRequest;
import com.bhf.aeroncache.models.bulk.requests.CacheOperation;

import java.util.List;

/**
 * A reusable, gateway-local {@link BulkOpsRequest} backed by the ingress agent's pooled operation
 * flyweights, replacing the immutable {@link com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest}
 * record on the gateway's forward-to-cluster path.
 * <p>
 * The gateway decodes an inbound bulk frame into reused {@link ReusableCacheOperation} instances and hands
 * them onward through this request. It is encoded to the cluster synchronously and never escapes the ingress
 * agent thread, so a single instance can be reused across bulk requests (see {@link ReusableCacheOperation}).
 */
@Flyweight
public class GatewayBulkOpsRequest implements BulkOpsRequest {

    private String requestId;
    private List<? extends CacheOperation> operations;

    /**
     * Point this request at the operations for the current bulk frame.
     */
    public void set(String requestId, List<? extends CacheOperation> operations) {
        this.requestId = requestId;
        this.operations = operations;
    }

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public List<? extends CacheOperation> operations() {
        return operations;
    }
}
