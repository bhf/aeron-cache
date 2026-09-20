package com.bhf.aeroncache.models.bulk.requests;

import java.util.List;

/**
 * A batch of cache operations to be applied by the cluster in order, plus the request id the response is
 * correlated by.
 * <p>
 * Extracted so a hot path can supply its own implementation. {@link BulkCacheOpsRequest} is the default,
 * immutable, Jackson-deserializable form (its {@code operations} are concrete {@link CacheOperationRequest}
 * records). The Aeron gateway instead supplies a reusable implementation backed by pooled operation
 * flyweights, avoiding per-request allocation. The shared publish/encode path reads a request only through
 * this interface, so both forms flow through it unchanged.
 */
public interface BulkOpsRequest {

    /** @return the request id echoed on the response. */
    String requestId();

    /** @return the operations to apply, in order. */
    List<? extends CacheOperation> operations();
}
