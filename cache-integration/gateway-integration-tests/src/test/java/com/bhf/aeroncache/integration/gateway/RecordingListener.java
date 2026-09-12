package com.bhf.aeroncache.integration.gateway;

import com.bhf.aeroncache.gateway.client.GatewayBulkOpResult;
import com.bhf.aeroncache.gateway.client.GatewayClient;
import com.bhf.aeroncache.gateway.client.GatewayClientListener;
import com.bhf.aeroncache.gateway.client.GatewayStat;
import com.bhf.aeroncache.gateway.messages.OperationStatus;
import com.bhf.aeroncache.gateway.messages.UpdateEventType;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Records callbacks from the {@link GatewayClient} for assertion. Entries and stats are accumulated
 * per correlation id across batches until an end-of-batch frame is seen.
 */
final class RecordingListener implements GatewayClientListener {

    record CommandResponse(OperationStatus status, String cacheId, String key, String value) {
    }

    record StreamUpdate(UpdateEventType eventType, String cacheId, String key, String value) {
    }

    record SubscribeAck(OperationStatus status, List<String> cacheIds) {
    }

    final Map<String, CommandResponse> commandResponses = new ConcurrentHashMap<>();
    final Map<String, Map<String, String>> entriesAccumulated = new ConcurrentHashMap<>();
    final Map<String, OperationStatus> entriesStatus = new ConcurrentHashMap<>();
    final Map<String, Boolean> entriesComplete = new ConcurrentHashMap<>();
    final Map<String, List<GatewayStat>> statsAccumulated = new ConcurrentHashMap<>();
    final Map<String, Boolean> statsComplete = new ConcurrentHashMap<>();
    final Queue<StreamUpdate> streamUpdates = new ConcurrentLinkedQueue<>();
    final Map<String, SubscribeAck> subscribeAcks = new ConcurrentHashMap<>();
    final Map<String, List<GatewayBulkOpResult>> bulkResponses = new ConcurrentHashMap<>();
    final Map<String, String> errors = new ConcurrentHashMap<>();

    @Override
    public void onCommandResponse(String correlationId, OperationStatus status, String cacheId, String key, String value) {
        commandResponses.put(correlationId, new CommandResponse(status, cacheId, key, value));
    }

    @Override
    public void onEntries(String correlationId, OperationStatus status, String cacheId, Map<String, String> items, boolean endOfBatch) {
        entriesAccumulated.computeIfAbsent(correlationId, k -> new ConcurrentHashMap<>()).putAll(items);
        entriesStatus.put(correlationId, status);
        if (endOfBatch) {
            entriesComplete.put(correlationId, Boolean.TRUE);
        }
    }

    @Override
    public void onStats(String correlationId, OperationStatus status, List<GatewayStat> stats, boolean endOfBatch) {
        statsAccumulated.computeIfAbsent(correlationId, k -> new CopyOnWriteArrayList<>()).addAll(stats);
        if (endOfBatch) {
            statsComplete.put(correlationId, Boolean.TRUE);
        }
    }

    @Override
    public void onStreamUpdate(String correlationId, UpdateEventType eventType, String cacheId, String key, String value) {
        streamUpdates.add(new StreamUpdate(eventType, cacheId, key, value));
    }

    @Override
    public void onSubscribeAck(String correlationId, OperationStatus status, List<String> cacheIds) {
        subscribeAcks.put(correlationId, new SubscribeAck(status, List.copyOf(cacheIds)));
    }

    @Override
    public void onBulkResponse(String correlationId, List<GatewayBulkOpResult> results) {
        bulkResponses.put(correlationId, List.copyOf(results));
    }

    @Override
    public void onError(String correlationId, OperationStatus status, String message) {
        errors.put(correlationId, message);
    }
}
