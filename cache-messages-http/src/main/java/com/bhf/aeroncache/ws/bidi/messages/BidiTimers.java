package com.bhf.aeroncache.ws.bidi.messages;

import com.bhf.aeroncache.models.results.CacheOperationStatus;

import java.util.List;

/**
 * A batch of pending TTL removal timers streamed in response to a getTimers command.
 *
 * <p>The JSON mirror of the gateway's {@code GatewayTimers}. Mirrors the cluster
 * {@code AllTimersResult}: the final batch for a request carries {@code endOfBatch=true}.
 * Each timer is tagged with its type so cache timers can be told from counter timers.</p>
 *
 * @param type          the frame discriminator; always {@link BidiServerMessage#TIMERS}.
 * @param correlationId the correlation id echoed from the originating command.
 * @param status        the operation status.
 * @param timers        the timer records in this batch.
 * @param endOfBatch    {@code true} when this is the final batch for the request.
 */
public record BidiTimers(String type,
                         String correlationId,
                         CacheOperationStatus status,
                         List<TimerEntry> timers,
                         boolean endOfBatch) implements BidiServerMessage {

    /**
     * A single pending timer record, decoupled from the cluster domain types.
     *
     * @param timerType the type of timer, either {@code CACHE} or {@code COUNTER}.
     * @param cacheId   the cache (or counter cache) id the entry will be removed from.
     * @param key       the key that will be removed.
     * @param deadline  the epoch time (millis) at which the removal is scheduled to fire.
     */
    public record TimerEntry(String timerType, String cacheId, String key, long deadline) {
    }

    /**
     * Build a timers batch frame, normalising a {@code null} status to {@link CacheOperationStatus#NONE}.
     *
     * @param correlationId the correlation id to echo.
     * @param status        the operation status.
     * @param timers        the timer records in this batch.
     * @param endOfBatch    whether this is the final batch for the request.
     * @return the frame.
     */
    public static BidiTimers of(String correlationId, CacheOperationStatus status,
                                List<TimerEntry> timers, boolean endOfBatch) {
        return new BidiTimers(TIMERS, correlationId,
                status == null ? CacheOperationStatus.NONE : status,
                timers == null ? List.of() : timers, endOfBatch);
    }
}
