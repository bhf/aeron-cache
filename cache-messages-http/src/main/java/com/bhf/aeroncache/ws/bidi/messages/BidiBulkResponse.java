package com.bhf.aeroncache.ws.bidi.messages;

import com.bhf.aeroncache.models.bulk.responses.CacheOperationResponse;

import java.util.List;

/**
 * The response to a {@link BidiBulk} request, correlated via {@code correlationId}.
 *
 * <p>The JSON mirror of the gateway's {@code GatewayBulkResponse}. Carries one
 * {@link CacheOperationResponse} per requested operation, in request order; each entry echoes its
 * operation's own {@code requestId}. Read operations ({@code GET_ITEM}/{@code GET_COUNTER}) and counter
 * arithmetic report their value in {@link CacheOperationResponse#value()}.</p>
 *
 * @param type              the frame discriminator; always {@link BidiServerMessage#BULK_RESPONSE}.
 * @param correlationId     the correlation id echoed from the originating bulk request.
 * @param operationResponses the per-operation results, in request order.
 */
public record BidiBulkResponse(String type,
                               String correlationId,
                               List<CacheOperationResponse> operationResponses) implements BidiServerMessage {

    /**
     * Build a bulk response frame.
     *
     * @param correlationId      the correlation id to echo.
     * @param operationResponses the per-operation results, in request order.
     * @return the frame.
     */
    public static BidiBulkResponse of(String correlationId, List<CacheOperationResponse> operationResponses) {
        return new BidiBulkResponse(BULK_RESPONSE, correlationId,
                operationResponses == null ? List.of() : operationResponses);
    }
}
