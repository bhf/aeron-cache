package com.bhf.aeroncache.gateway.codec;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.gateway.messages.GatewayCommandResponseEncoder;
import com.bhf.aeroncache.gateway.messages.GatewayEntriesEncoder;
import com.bhf.aeroncache.gateway.messages.GatewayErrorEncoder;
import com.bhf.aeroncache.gateway.messages.GatewayStatsEncoder;
import com.bhf.aeroncache.gateway.messages.GatewayStreamUpdateEncoder;
import com.bhf.aeroncache.gateway.messages.BooleanType;
import com.bhf.aeroncache.gateway.messages.MessageHeaderEncoder;
import com.bhf.aeroncache.gateway.messages.OperationStatus;
import com.bhf.aeroncache.gateway.messages.UpdateEventType;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import io.aeron.Publication;
import lombok.extern.log4j.Log4j2;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;

import java.util.List;
import java.util.Map;

/**
 * Encodes gateway response/update/error frames (SBE) and publishes them onto a
 * client session's Aeron response {@link Publication}.
 * <p>
 * A single instance is used per response-writing thread (the cluster egress
 * listener thread), so the internal buffer is not shared across threads.
 */
@Log4j2
public class GatewayResponseWriter {

    private static final int MAX_OFFER_ATTEMPTS = 1000;

    private final MutableDirectBuffer buffer = new ExpandableArrayBuffer(4096);
    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final GatewayCommandResponseEncoder commandResponseEncoder = new GatewayCommandResponseEncoder();
    private final GatewayStreamUpdateEncoder streamUpdateEncoder = new GatewayStreamUpdateEncoder();
    private final GatewayEntriesEncoder entriesEncoder = new GatewayEntriesEncoder();
    private final GatewayStatsEncoder statsEncoder = new GatewayStatsEncoder();
    private final GatewayErrorEncoder errorEncoder = new GatewayErrorEncoder();

    /**
     * A single cache stats record, decoupled from the cluster domain types.
     */
    public record StatEntry(String cacheId, long addedCount, long removedCount, long clearedCount, long size) {
    }

    /**
     * Encode and publish a command response frame.
     */
    public void writeCommandResponse(Publication publication, String correlationId, CacheOperationStatus status,
                                     String cacheId, String key, String value) {
        commandResponseEncoder.wrapAndApplyHeader(buffer, 0, headerEncoder)
                .status(mapStatus(status))
                .correlationId(nullSafe(correlationId))
                .cacheId(nullSafe(cacheId))
                .key(nullSafe(key))
                .value(nullSafe(value));
        offer(publication, commandResponseEncoder.limit());
    }

    /**
     * Encode and publish a batch of streamed cache/counter entries in response to a getEntries command.
     * <p>
     * Mirrors the cluster {@code AllCacheEntriesResult}: entries are delivered in one or more batches,
     * and the final batch for a request carries {@code endOfBatch=true}.
     *
     * @param items      the key/value pairs in this batch (may be empty for a terminal end-of-batch frame).
     * @param endOfBatch {@code true} when this is the final batch for the request.
     */
    public void writeEntries(Publication publication, String correlationId, CacheOperationStatus status,
                             String cacheId, Map<String, String> items, boolean endOfBatch) {
        var entriesEnc = entriesEncoder.wrapAndApplyHeader(buffer, 0, headerEncoder)
                .status(mapStatus(status))
                .endOfBatch(mapBoolean(endOfBatch));
        var itemsEnc = entriesEnc.itemsCount(items.size());
        for (Map.Entry<String, String> entry : items.entrySet()) {
            itemsEnc.next()
                    .key(nullSafe(entry.getKey()))
                    .value(nullSafe(entry.getValue()));
        }
        entriesEnc.correlationId(nullSafe(correlationId))
                .cacheId(nullSafe(cacheId));
        offer(publication, entriesEncoder.limit());
    }

    /**
     * Encode and publish a batch of cache stats in response to a getStats command.
     * <p>
     * Mirrors the cluster {@code AllCacheStatsResult}: stats are delivered as a group,
     * and the final batch for a request carries {@code endOfBatch=true}.
     *
     * @param stats      the stats records in this batch.
     * @param endOfBatch {@code true} when this is the final batch for the request.
     */
    public void writeStats(Publication publication, String correlationId, CacheOperationStatus status,
                           List<StatEntry> stats, boolean endOfBatch) {
        var statsEnc = statsEncoder.wrapAndApplyHeader(buffer, 0, headerEncoder)
                .status(mapStatus(status))
                .endOfBatch(mapBoolean(endOfBatch));
        var groupEnc = statsEnc.statsCount(stats.size());
        for (StatEntry stat : stats) {
            groupEnc.next()
                    .addedCount(stat.addedCount())
                    .removedCount(stat.removedCount())
                    .clearedCount(stat.clearedCount())
                    .size(stat.size())
                    .cacheId(nullSafe(stat.cacheId()));
        }
        statsEnc.correlationId(nullSafe(correlationId));
        offer(publication, statsEncoder.limit());
    }

    /**
     * Encode and publish a streaming update frame.
     */
    public void writeStreamUpdate(Publication publication, CacheUpdateEvent<?> event) {
        var value = event.itemValue() == null ? null : String.valueOf(event.itemValue());
        streamUpdateEncoder.wrapAndApplyHeader(buffer, 0, headerEncoder)
                .eventType(mapEventType(event.eventType()))
                .cacheId(nullSafe(event.cacheId()))
                .key(nullSafe(event.itemKey()))
                .value(nullSafe(value))
                .correlationId(nullSafe(event.requestId()));
        offer(publication, streamUpdateEncoder.limit());
    }

    /**
     * Encode and publish an error frame.
     */
    public void writeError(Publication publication, String correlationId, CacheOperationStatus status, String message) {
        errorEncoder.wrapAndApplyHeader(buffer, 0, headerEncoder)
                .status(mapStatus(status))
                .correlationId(nullSafe(correlationId))
                .message(nullSafe(message));
        offer(publication, errorEncoder.limit());
    }

    private void offer(Publication publication, int length) {
        if (publication == null || !publication.isConnected()) {
            log.debug("Response publication not connected, dropping frame of length {}", length);
            return;
        }
        for (int attempt = 0; attempt < MAX_OFFER_ATTEMPTS; attempt++) {
            long result = publication.offer(buffer, 0, length);
            if (result > 0) {
                return;
            }
            if (result == Publication.CLOSED || result == Publication.NOT_CONNECTED || result == Publication.MAX_POSITION_EXCEEDED) {
                log.debug("Dropping frame, publication result {}", result);
                return;
            }
        }
        log.warn("Gave up offering frame of length {} after {} attempts", length, MAX_OFFER_ATTEMPTS);
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static BooleanType mapBoolean(boolean value) {
        return value ? BooleanType.T : BooleanType.F;
    }

    private static OperationStatus mapStatus(CacheOperationStatus status) {
        if (status == null) {
            return OperationStatus.NONE;
        }
        try {
            return OperationStatus.valueOf(status.name());
        } catch (IllegalArgumentException e) {
            return OperationStatus.ERROR;
        }
    }

    private static UpdateEventType mapEventType(CacheUpdateEvent.EventType eventType) {
        return switch (eventType) {
            case ADD_ITEM -> UpdateEventType.ADD_ITEM;
            case REMOVE_ITEM -> UpdateEventType.REMOVE_ITEM;
            case CLEAR_CACHE -> UpdateEventType.CLEAR_CACHE;
            case DELETE_CACHE -> UpdateEventType.DELETE_CACHE;
        };
    }
}
