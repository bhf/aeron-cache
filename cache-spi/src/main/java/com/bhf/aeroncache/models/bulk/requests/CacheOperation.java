package com.bhf.aeroncache.models.bulk.requests;

/**
 * A single operation within a {@link BulkCacheOpsRequest}.
 * <p>
 * Extracted so that callers who hold a batch of operations can supply their own implementation. The
 * immutable {@link CacheOperationRequest} record is the default; a hot path (e.g. the Aeron gateway)
 * can instead supply a pooled, reusable flyweight to avoid allocating one instance per operation while
 * remaining compatible with the shared encode path, which reads operations only through this interface.
 */
public interface CacheOperation {

    /** @return the operation to perform. */
    BulkOperationType operationType();

    /** @return the entry time-to-live in millis, where applicable ({@code 0} for none). */
    long ttl();

    /** @return the counter amount/value, for counter operations ({@code 0} when not applicable). */
    long counterValue();

    /** @return the per-operation id, echoed on the matching result. */
    String requestId();

    /** @return the target cache id. */
    String cacheId();

    /** @return the entry key, where applicable. */
    String key();

    /** @return the entry value, where applicable. */
    String value();
}
