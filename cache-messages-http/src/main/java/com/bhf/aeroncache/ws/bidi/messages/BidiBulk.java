package com.bhf.aeroncache.ws.bidi.messages;

import com.bhf.aeroncache.models.bulk.requests.CacheOperationRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A batch of cache/counter operations sent over the bidirectional websocket protocol.
 *
 * <p>The JSON mirror of the Aeron gateway's {@code GatewayBulkRequest}. Each {@link CacheOperationRequest}
 * carries its own {@code requestId} (echoed on the matching {@link BidiBulkResponse} entry) so the caller
 * can correlate individual operations; results are also returned in request order. A single bulk request
 * may freely mix regular-cache and counter operations, which the cluster applies in order.</p>
 *
 * @param correlationId the client correlation id, echoed on the bulk response.
 * @param operations    the operations to apply, in order.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BidiBulk(String correlationId,
                       List<CacheOperationRequest> operations) implements BidiClientMessage {
}
