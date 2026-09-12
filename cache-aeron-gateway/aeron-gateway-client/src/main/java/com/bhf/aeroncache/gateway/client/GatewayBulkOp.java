package com.bhf.aeroncache.gateway.client;

import com.bhf.aeroncache.gateway.messages.BulkOperationType;

/**
 * A single operation within a {@link GatewayClient#bulkOperations bulk request}.
 * <p>
 * Mirrors the cluster's {@code CacheOperationRequest}: one bulk request batches many of these, each
 * carrying its own {@code requestId} so the caller can correlate it to the matching
 * {@link GatewayBulkOpResult} in the response (results are also returned in request order). A single
 * bulk request may freely mix regular-cache and counter operations; the cluster applies them in order.
 *
 * @param operationType the operation to perform.
 * @param ttl           the entry time-to-live in millis, where applicable ({@code 0} for none).
 * @param counterValue  the counter amount/value, for counter operations ({@code 0} when not applicable).
 * @param requestId     the per-operation id, echoed on the matching result.
 * @param cacheId       the target cache id.
 * @param key           the entry key, where applicable.
 * @param value         the entry value, where applicable.
 */
public record GatewayBulkOp(BulkOperationType operationType, long ttl, long counterValue,
                            String requestId, String cacheId, String key, String value) {

    /** A regular-cache add-item operation. */
    public static GatewayBulkOp addItem(String requestId, String cacheId, String key, String value, long ttl) {
        return new GatewayBulkOp(BulkOperationType.ADD_ITEM, ttl, 0L, requestId, cacheId, key, value);
    }

    /** A regular-cache get-item operation; the value is returned on the matching result. */
    public static GatewayBulkOp getItem(String requestId, String cacheId, String key) {
        return new GatewayBulkOp(BulkOperationType.GET_ITEM, 0L, 0L, requestId, cacheId, key, null);
    }

    /** A regular-cache create-cache operation. */
    public static GatewayBulkOp createCache(String requestId, String cacheId) {
        return new GatewayBulkOp(BulkOperationType.CREATE_CACHE, 0L, 0L, requestId, cacheId, null, null);
    }

    /** A counter increment operation; the resulting running value is returned on the matching result. */
    public static GatewayBulkOp incrementCounter(String requestId, String cacheId, String key, long delta) {
        return new GatewayBulkOp(BulkOperationType.INCREMENT_COUNTER, 0L, delta, requestId, cacheId, key, null);
    }
}
