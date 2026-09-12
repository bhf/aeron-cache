package com.bhf.aeroncache.ws.bidi.messages;

import com.bhf.aeroncache.models.results.CacheOperationStatus;

import java.util.Map;

/**
 * A batch of cache/counter entries streamed in response to a getEntries command.
 *
 * <p>The JSON mirror of the gateway's {@code GatewayEntries}. Mirrors the cluster
 * {@code AllCacheEntriesResult}: entries are delivered in one or more batches, and the final batch for a
 * request carries {@code endOfBatch=true}. Clients accumulate across frames with the same
 * {@code correlationId} until an end-of-batch frame.</p>
 *
 * @param type          the frame discriminator; always {@link BidiServerMessage#ENTRIES}.
 * @param correlationId the correlation id echoed from the originating command.
 * @param status        the operation status.
 * @param cacheId       the cache the entries belong to.
 * @param items         the key/value pairs in this batch (may be empty on a terminal frame).
 * @param endOfBatch    {@code true} when this is the final batch for the request.
 */
public record BidiEntries(String type,
                          String correlationId,
                          CacheOperationStatus status,
                          String cacheId,
                          Map<String, String> items,
                          boolean endOfBatch) implements BidiServerMessage {

    /**
     * Build an entries batch frame, normalising a {@code null} status to {@link CacheOperationStatus#NONE}.
     *
     * @param correlationId the correlation id to echo.
     * @param status        the operation status.
     * @param cacheId       the cache the entries belong to.
     * @param items         the key/value pairs in this batch.
     * @param endOfBatch    whether this is the final batch for the request.
     * @return the frame.
     */
    public static BidiEntries of(String correlationId, CacheOperationStatus status, String cacheId,
                                 Map<String, String> items, boolean endOfBatch) {
        return new BidiEntries(ENTRIES, correlationId,
                status == null ? CacheOperationStatus.NONE : status, cacheId,
                items == null ? Map.of() : items, endOfBatch);
    }
}
