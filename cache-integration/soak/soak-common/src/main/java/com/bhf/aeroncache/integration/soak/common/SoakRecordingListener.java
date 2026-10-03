package com.bhf.aeroncache.integration.soak.common;

import com.bhf.aeroncache.gateway.client.GatewayClientListener;
import com.bhf.aeroncache.gateway.client.GatewayStat;
import com.bhf.aeroncache.gateway.messages.OperationStatus;
import com.bhf.aeroncache.gateway.messages.UpdateEventType;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Records the gateway callbacks soak workloads assert on: command responses, accumulated
 * getEntries/getCounterEntries batches, streamed updates (for the streaming soak) and subscribe acks.
 * Callbacks fire on the client's agent thread, so the collections are concurrent; the single workload
 * thread drains and <em>removes</em> correlation ids as soon as it has consumed them, and drains the
 * stream-update queue per round, so nothing accumulates unboundedly over an arbitrarily long run.
 *
 * <p>Listener arguments are reused by the client across frames, so everything retained here is copied
 * (the records and {@link List#copyOf} snapshots).
 */
public final class SoakRecordingListener implements GatewayClientListener {

    public record CommandResponse(OperationStatus status, String cacheId, String key, String value) {
    }

    public record StreamUpdate(String correlationId, UpdateEventType eventType, String cacheId, String key, String value) {
    }

    public record SubscribeAck(OperationStatus status, List<String> cacheIds) {
    }

    public final Map<String, CommandResponse> commandResponses = new ConcurrentHashMap<>();
    public final Map<String, Map<String, String>> entriesAccumulated = new ConcurrentHashMap<>();
    public final Map<String, OperationStatus> entriesStatus = new ConcurrentHashMap<>();
    public final Map<String, Boolean> entriesComplete = new ConcurrentHashMap<>();
    public final Queue<StreamUpdate> streamUpdates = new ConcurrentLinkedQueue<>();
    public final Map<String, SubscribeAck> subscribeAcks = new ConcurrentHashMap<>();
    public final Map<String, String> errors = new ConcurrentHashMap<>();

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
        // Unused by soak workloads.
    }

    @Override
    public void onStreamUpdate(String correlationId, UpdateEventType eventType, String cacheId, String key, String value) {
        streamUpdates.add(new StreamUpdate(correlationId, eventType, cacheId, key, value));
    }

    @Override
    public void onSubscribeAck(String correlationId, OperationStatus status, List<String> cacheIds) {
        subscribeAcks.put(correlationId, new SubscribeAck(status, List.copyOf(cacheIds)));
    }

    @Override
    public void onError(String correlationId, OperationStatus status, String message) {
        errors.put(correlationId, message);
    }
}
