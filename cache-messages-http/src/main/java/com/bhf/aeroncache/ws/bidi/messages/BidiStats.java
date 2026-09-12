package com.bhf.aeroncache.ws.bidi.messages;

import com.bhf.aeroncache.models.results.CacheOperationStatus;

import java.util.List;

/**
 * A batch of cache stats streamed in response to a getStats command.
 *
 * <p>The JSON mirror of the gateway's {@code GatewayStats}. Mirrors the cluster
 * {@code AllCacheStatsResult}: the final batch for a request carries {@code endOfBatch=true}.</p>
 *
 * @param type          the frame discriminator; always {@link BidiServerMessage#STATS}.
 * @param correlationId the correlation id echoed from the originating command.
 * @param status        the operation status.
 * @param stats         the stats records in this batch.
 * @param endOfBatch    {@code true} when this is the final batch for the request.
 */
public record BidiStats(String type,
                        String correlationId,
                        CacheOperationStatus status,
                        List<StatEntry> stats,
                        boolean endOfBatch) implements BidiServerMessage {

    /**
     * A single cache stats record, decoupled from the cluster domain types.
     *
     * @param cacheId      the cache id.
     * @param addedCount   the number of entries added over the cache's lifetime.
     * @param removedCount the number of entries removed over the cache's lifetime.
     * @param clearedCount the number of clear operations over the cache's lifetime.
     * @param size         the current number of entries.
     */
    public record StatEntry(String cacheId, long addedCount, long removedCount, long clearedCount, long size) {
    }

    /**
     * Build a stats batch frame, normalising a {@code null} status to {@link CacheOperationStatus#NONE}.
     *
     * @param correlationId the correlation id to echo.
     * @param status        the operation status.
     * @param stats         the stats records in this batch.
     * @param endOfBatch    whether this is the final batch for the request.
     * @return the frame.
     */
    public static BidiStats of(String correlationId, CacheOperationStatus status,
                               List<StatEntry> stats, boolean endOfBatch) {
        return new BidiStats(STATS, correlationId,
                status == null ? CacheOperationStatus.NONE : status,
                stats == null ? List.of() : stats, endOfBatch);
    }
}
