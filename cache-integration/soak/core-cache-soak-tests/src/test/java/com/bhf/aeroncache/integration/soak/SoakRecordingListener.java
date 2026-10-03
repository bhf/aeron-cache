package com.bhf.aeroncache.integration.soak;

import com.bhf.aeroncache.gateway.client.GatewayClientListener;
import com.bhf.aeroncache.gateway.client.GatewayStat;
import com.bhf.aeroncache.gateway.messages.OperationStatus;
import com.bhf.aeroncache.gateway.messages.UpdateEventType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records just the gateway callbacks the soak workload needs: command responses, accumulated
 * getEntries/getCounterEntries batches, and errors. Callbacks fire on the client's agent thread, so the
 * maps are concurrent; the (single) workload thread drains and <em>removes</em> each correlation id as
 * soon as it has consumed the response, so these maps stay bounded over an arbitrarily long run rather
 * than leaking one entry per operation.
 *
 * <p>The workload does not subscribe, so streaming-update and subscribe-ack callbacks are intentionally
 * left as the interface defaults (no-ops) and nothing accumulates for them.
 */
final class SoakRecordingListener implements GatewayClientListener {

    record CommandResponse(OperationStatus status, String cacheId, String key, String value) {
    }

    final Map<String, CommandResponse> commandResponses = new ConcurrentHashMap<>();
    final Map<String, Map<String, String>> entriesAccumulated = new ConcurrentHashMap<>();
    final Map<String, OperationStatus> entriesStatus = new ConcurrentHashMap<>();
    final Map<String, Boolean> entriesComplete = new ConcurrentHashMap<>();
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
        // Unused by the soak workload (reconciliation reads entries directly).
    }

    @Override
    public void onStreamUpdate(String correlationId, UpdateEventType eventType, String cacheId, String key, String value) {
        // Unused: the soak workload never subscribes.
    }

    @Override
    public void onError(String correlationId, OperationStatus status, String message) {
        errors.put(correlationId, message);
    }
}
