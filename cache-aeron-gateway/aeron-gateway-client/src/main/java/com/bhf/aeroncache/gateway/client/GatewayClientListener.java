package com.bhf.aeroncache.gateway.client;

import com.bhf.aeroncache.gateway.messages.OperationStatus;
import com.bhf.aeroncache.gateway.messages.UpdateEventType;

import java.util.List;
import java.util.Map;

/**
 * Callback interface for responses, streamed data and updates received by a {@link GatewayClient}.
 * <p>
 * All callbacks are invoked on the {@link GatewayClient}'s agent thread (the thread driving
 * {@link GatewayClient#doWork()}), so implementations must not block.
 */
public interface GatewayClientListener {

    /**
     * A response to a request/response command (create/add/get-entry/clear/delete/remove/counter ops).
     *
     * @param correlationId the correlation id echoed from the originating command.
     * @param status        the operation status.
     * @param cacheId       the cache the command targeted.
     * @param key           the entry key, if applicable (may be empty).
     * @param value         the entry value, if applicable (may be empty).
     */
    void onCommandResponse(String correlationId, OperationStatus status, String cacheId, String key, String value);

    /**
     * A batch of entries streamed in response to a getEntries command.
     * <p>
     * Entries are delivered in one or more batches; the final batch carries {@code endOfBatch=true}.
     * Accumulate across invocations with the same {@code correlationId} until an end-of-batch frame.
     *
     * @param correlationId the correlation id echoed from the originating command.
     * @param status        the operation status.
     * @param cacheId       the cache the entries belong to.
     * @param items         the key/value pairs in this batch (may be empty on a terminal frame).
     * @param endOfBatch    {@code true} when this is the final batch for the request.
     */
    void onEntries(String correlationId, OperationStatus status, String cacheId, Map<String, String> items, boolean endOfBatch);

    /**
     * A batch of cache stats streamed in response to a getStats command.
     *
     * @param correlationId the correlation id echoed from the originating command.
     * @param status        the operation status.
     * @param stats         the stats records in this batch.
     * @param endOfBatch    {@code true} when this is the final batch for the request.
     */
    void onStats(String correlationId, OperationStatus status, List<GatewayStat> stats, boolean endOfBatch);

    /**
     * A streaming cache update pushed to a subscribed client.
     *
     * @param correlationId the subscription's correlation id.
     * @param eventType     the type of update.
     * @param cacheId       the cache the update belongs to.
     * @param key           the entry key, if applicable (may be empty).
     * @param value         the entry value, if applicable (may be empty).
     */
    void onStreamUpdate(String correlationId, UpdateEventType eventType, String cacheId, String key, String value);

    /**
     * An error correlated to a request.
     *
     * @param correlationId the correlation id echoed from the originating command.
     * @param status        the error status.
     * @param message       a human readable error message.
     */
    void onError(String correlationId, OperationStatus status, String message);
}
