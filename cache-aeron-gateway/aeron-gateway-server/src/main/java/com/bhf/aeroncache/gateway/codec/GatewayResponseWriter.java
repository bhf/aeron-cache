package com.bhf.aeroncache.gateway.codec;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.gateway.messages.GatewayBulkResponseEncoder;
import com.bhf.aeroncache.gateway.messages.GatewayCommandResponseEncoder;
import com.bhf.aeroncache.gateway.messages.GatewayEntriesEncoder;
import com.bhf.aeroncache.gateway.messages.GatewayErrorEncoder;
import com.bhf.aeroncache.gateway.messages.GatewayStatsEncoder;
import com.bhf.aeroncache.gateway.messages.GatewayStreamUpdateEncoder;
import com.bhf.aeroncache.gateway.messages.GatewaySubscribeAckEncoder;
import com.bhf.aeroncache.gateway.messages.GatewayTimersEncoder;
import com.bhf.aeroncache.gateway.messages.TimerType;
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
    private final GatewaySubscribeAckEncoder subscribeAckEncoder = new GatewaySubscribeAckEncoder();
    private final GatewayBulkResponseEncoder bulkResponseEncoder = new GatewayBulkResponseEncoder();
    private final GatewayTimersEncoder timersEncoder = new GatewayTimersEncoder();

    /**
     * A single cache stats record, decoupled from the cluster domain types.
     * <p>
     * A mutable carrier (rather than a record) so callers on the single response-writing thread can reuse
     * instances across batches via a {@link com.bhf.aeroncache.gateway.application.FlyweightList} instead
     * of allocating one per stat. The all-args constructor is retained for one-off construction and tests.
     */
    public static final class StatEntry {
        private String cacheId;
        private long addedCount;
        private long removedCount;
        private long clearedCount;
        private long size;

        public StatEntry() {
        }

        public StatEntry(String cacheId, long addedCount, long removedCount, long clearedCount, long size) {
            set(cacheId, addedCount, removedCount, clearedCount, size);
        }

        public void set(String cacheId, long addedCount, long removedCount, long clearedCount, long size) {
            this.cacheId = cacheId;
            this.addedCount = addedCount;
            this.removedCount = removedCount;
            this.clearedCount = clearedCount;
            this.size = size;
        }

        public String cacheId() {
            return cacheId;
        }

        public long addedCount() {
            return addedCount;
        }

        public long removedCount() {
            return removedCount;
        }

        public long clearedCount() {
            return clearedCount;
        }

        public long size() {
            return size;
        }
    }

    /**
     * A single pending TTL removal timer, decoupled from the cluster domain types. Mutable so response-side
     * callers can reuse instances across batches; see {@link StatEntry}.
     */
    public static final class TimerEntry {
        private String timerType;
        private String cacheId;
        private String key;
        private long deadline;

        public TimerEntry() {
        }

        public TimerEntry(String timerType, String cacheId, String key, long deadline) {
            set(timerType, cacheId, key, deadline);
        }

        public void set(String timerType, String cacheId, String key, long deadline) {
            this.timerType = timerType;
            this.cacheId = cacheId;
            this.key = key;
            this.deadline = deadline;
        }

        public String timerType() {
            return timerType;
        }

        public String cacheId() {
            return cacheId;
        }

        public String key() {
            return key;
        }

        public long deadline() {
            return deadline;
        }
    }

    /**
     * A single bulk operation result, decoupled from the cluster domain types. Mutable so response-side
     * callers can reuse instances across batches; see {@link StatEntry}.
     */
    public static final class BulkOpResultEntry {
        private CacheOperationStatus status;
        private String requestId;
        private String cacheId;
        private String key;
        private String value;

        public BulkOpResultEntry() {
        }

        public BulkOpResultEntry(CacheOperationStatus status, String requestId, String cacheId, String key, String value) {
            set(status, requestId, cacheId, key, value);
        }

        public void set(CacheOperationStatus status, String requestId, String cacheId, String key, String value) {
            this.status = status;
            this.requestId = requestId;
            this.cacheId = cacheId;
            this.key = key;
            this.value = value;
        }

        public CacheOperationStatus status() {
            return status;
        }

        public String requestId() {
            return requestId;
        }

        public String cacheId() {
            return cacheId;
        }

        public String key() {
            return key;
        }

        public String value() {
            return value;
        }
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
     * Encode and publish a batch of pending TTL removal timers in response to a getTimers command.
     * <p>
     * Mirrors the cluster {@code AllTimersResult}: timers are delivered as a group, and the final
     * batch for a request carries {@code endOfBatch=true}.
     *
     * @param timers     the timer records in this batch.
     * @param endOfBatch {@code true} when this is the final batch for the request.
     */
    public void writeTimers(Publication publication, String correlationId, CacheOperationStatus status,
                            List<TimerEntry> timers, boolean endOfBatch) {
        var timersEnc = timersEncoder.wrapAndApplyHeader(buffer, 0, headerEncoder)
                .status(mapStatus(status))
                .endOfBatch(mapBoolean(endOfBatch));
        var groupEnc = timersEnc.timersCount(timers.size());
        for (TimerEntry timer : timers) {
            groupEnc.next()
                    .timerType(mapTimerType(timer.timerType()))
                    .deadline(timer.deadline())
                    .cacheId(nullSafe(timer.cacheId()))
                    .key(nullSafe(timer.key()));
        }
        timersEnc.correlationId(nullSafe(correlationId));
        offer(publication, timersEncoder.limit());
    }

    private static TimerType mapTimerType(String timerType) {
        return "COUNTER".equals(timerType) ? TimerType.COUNTER : TimerType.CACHE;
    }

    /**
     * Encode and publish a streaming update frame.
     */
    public void writeStreamUpdate(Publication publication, CacheUpdateEvent<?> event) {
        writeStreamUpdate(publication, event.eventType(), event.cacheId(), event.itemKey(), event.itemValue(), event.requestId());
    }

    /**
     * Encode and publish a streaming update frame from raw fields, so callers can drive it from a reused
     * flyweight without allocating a {@link CacheUpdateEvent} per update.
     */
    public void writeStreamUpdate(Publication publication, CacheUpdateEvent.EventType eventType,
                                  String cacheId, String key, Object itemValue, String requestId) {
        var value = itemValue == null ? null : String.valueOf(itemValue);
        streamUpdateEncoder.wrapAndApplyHeader(buffer, 0, headerEncoder)
                .eventType(mapEventType(eventType))
                .cacheId(nullSafe(cacheId))
                .key(nullSafe(key))
                .value(nullSafe(value))
                .correlationId(nullSafe(requestId));
        offer(publication, streamUpdateEncoder.limit());
    }

    /**
     * Encode and publish a subscription-confirmed ack frame.
     * <p>
     * Emitted once per subscribe request when the cluster has registered the subscription, so the client
     * knows updates for the requested caches will now be delivered.
     *
     * @param correlationId the correlation id echoed from the subscribe request.
     * @param cacheIds      the caches the subscription now covers.
     */
    public void writeSubscribeAck(Publication publication, String correlationId, CacheOperationStatus status,
                                  List<String> cacheIds) {
        var ackEnc = subscribeAckEncoder.wrapAndApplyHeader(buffer, 0, headerEncoder)
                .status(mapStatus(status));
        var idsEnc = ackEnc.cacheIdsCount(cacheIds.size());
        for (String cacheId : cacheIds) {
            idsEnc.next().cacheId(nullSafe(cacheId));
        }
        ackEnc.correlationId(nullSafe(correlationId));
        offer(publication, subscribeAckEncoder.limit());
    }

    /**
     * Encode and publish a batch of bulk operation results.
     * <p>
     * Mirrors the cluster {@code BulkCacheOpsResult}: results are delivered in one or more batches (each
     * result in request order, echoing its operation's own {@code requestId}), and the final batch for a
     * request carries {@code endOfBatch=true}.
     *
     * @param correlationId the correlation id echoed from the bulk request.
     * @param operations    the per-operation results in this batch, in request order.
     * @param endOfBatch    {@code true} when this is the final batch for the request.
     */
    public void writeBulkResponse(Publication publication, String correlationId, List<BulkOpResultEntry> operations,
                                  boolean endOfBatch) {
        var bulkEnc = bulkResponseEncoder.wrapAndApplyHeader(buffer, 0, headerEncoder)
                .endOfBatch(mapBoolean(endOfBatch));
        var groupEnc = bulkEnc.operationsCount(operations.size());
        for (BulkOpResultEntry op : operations) {
            groupEnc.next()
                    .status(mapStatus(op.status()))
                    .requestId(nullSafe(op.requestId()))
                    .cacheId(nullSafe(op.cacheId()))
                    .key(nullSafe(op.key()))
                    .value(nullSafe(op.value()));
        }
        bulkEnc.correlationId(nullSafe(correlationId));
        offer(publication, bulkResponseEncoder.limit());
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
            case PATCH_ITEM -> UpdateEventType.PATCH_ITEM;
            case REMOVE_ITEM -> UpdateEventType.REMOVE_ITEM;
            case CLEAR_CACHE -> UpdateEventType.CLEAR_CACHE;
            case DELETE_CACHE -> UpdateEventType.DELETE_CACHE;
        };
    }
}
