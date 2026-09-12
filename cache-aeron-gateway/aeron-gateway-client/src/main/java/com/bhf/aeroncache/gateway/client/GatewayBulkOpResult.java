package com.bhf.aeroncache.gateway.client;

import com.bhf.aeroncache.gateway.messages.OperationStatus;

/**
 * The result of a single operation within a {@link GatewayClient#bulkOperations bulk request}.
 * <p>
 * Correlated to its originating {@link GatewayBulkOp} by {@code requestId} (results are also delivered in
 * request order). For read operations ({@code GET_ITEM}/{@code GET_COUNTER}) the read value is carried in
 * {@link #value()}; counter arithmetic operations report their resulting running value there too.
 *
 * @param status    the per-operation status.
 * @param requestId the per-operation id echoed from the originating {@link GatewayBulkOp}.
 * @param cacheId   the cache the operation targeted.
 * @param key       the entry key, where applicable (may be empty).
 * @param value     the entry/counter value, where applicable (may be empty).
 */
public record GatewayBulkOpResult(OperationStatus status, String requestId, String cacheId, String key, String value) {
}
