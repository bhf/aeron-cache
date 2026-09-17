package com.bhf.aeroncache.ws.bidi.messages;

import com.bhf.aeroncache.models.bulk.responses.CacheOperationResponse;

import java.util.List;

/**
 * The response to a {@link BidiBulk} request, correlated via {@code correlationId}.
 *
 * <p>The JSON mirror of the gateway's {@code GatewayBulkResponse}. Mirrors the cluster
 * {@code BulkCacheOpsResult}: per-operation results are delivered in one or more batches (each entry in
 * request order, echoing its operation's own {@code requestId}), and the final batch for a request
 * carries {@code endOfBatch=true}. Clients accumulate across frames with the same {@code correlationId}
 * until an end-of-batch frame. Read operations ({@code GET_ITEM}/{@code GET_COUNTER}) and counter
 * arithmetic report their value in {@link CacheOperationResponse#value()}.</p>
 *
 * @param type              the frame discriminator; always {@link BidiServerMessage#BULK_RESPONSE}.
 * @param correlationId     the correlation id echoed from the originating bulk request.
 * @param operationResponses the per-operation results in this batch, in request order.
 * @param endOfBatch        {@code true} when this is the final batch for the request.
 */
public record BidiBulkResponse(String type,
                               String correlationId,
                               List<CacheOperationResponse> operationResponses,
                               boolean endOfBatch) implements BidiServerMessage {

    /**
     * Build a bulk response batch frame.
     *
     * @param correlationId      the correlation id to echo.
     * @param operationResponses the per-operation results in this batch, in request order.
     * @param endOfBatch         whether this is the final batch for the request.
     * @return the frame.
     */
    public static BidiBulkResponse of(String correlationId, List<CacheOperationResponse> operationResponses,
                                      boolean endOfBatch) {
        return new BidiBulkResponse(BULK_RESPONSE, correlationId,
                operationResponses == null ? List.of() : operationResponses, endOfBatch);
    }
}
