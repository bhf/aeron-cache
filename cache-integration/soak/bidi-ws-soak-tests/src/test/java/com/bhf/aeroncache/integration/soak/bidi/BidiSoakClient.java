package com.bhf.aeroncache.integration.soak.bidi;

import com.bhf.aeroncache.ws.bidi.messages.BidiClientMessage;
import com.bhf.aeroncache.ws.bidi.messages.BidiServerMessage;
import com.bhf.aeroncache.ws.bidi.messages.WsOp;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * A <em>soak-grade</em> test client for the bidirectional websocket endpoint ({@code /api/ws/v1/bidi}): it
 * sends command / subscribe / unsubscribe / bulk frames over one persistent socket and routes every inbound
 * frame into bounded, correlation-id-keyed collections that the workload drains and <em>removes</em> as it
 * consumes them. This is the websocket analogue of the gateway soak's {@code SoakRecordingListener}.
 *
 * <p>It deliberately does <b>not</b> reuse {@code BidiWsHelper}: that helper retains every frame in an
 * ever-growing list and rescans it on every lookup, which is fine for a short integration test but would leak
 * memory and degrade to O(n) over a multi-hour soak. Here callbacks fire on OkHttp's (per-connection,
 * ordered) reader thread while the single workload thread drains the collections, so the collections are
 * concurrent and nothing accumulates unboundedly.
 */
final class BidiSoakClient implements AutoCloseable {

    /** A correlated command result ({@code commandResponse} frame). */
    record CommandResponse(String status, String cacheId, String key, String value) {
    }

    /** A streamed update ({@code streamUpdate} frame). The value is rendered as text (counters arrive as numbers). */
    record StreamUpdate(String correlationId, String eventType, String cacheId, String key, String value) {
    }

    /** A selector within a subscribe frame: a cache, an optional key filter, and the mode. */
    record Selector(String cacheId, String key, String mode) {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Correlation-id-keyed inbound state. The workload removes an entry as soon as it has consumed it, so
    // these never grow with run length.
    final Map<String, CommandResponse> commandResponses = new ConcurrentHashMap<>();
    final Map<String, Map<String, String>> entriesAccumulated = new ConcurrentHashMap<>();
    final Map<String, String> entriesStatus = new ConcurrentHashMap<>();
    final Map<String, Boolean> entriesComplete = new ConcurrentHashMap<>();
    final Map<String, Boolean> subscribeAcks = new ConcurrentHashMap<>();
    final Queue<StreamUpdate> streamUpdates = new ConcurrentLinkedQueue<>();
    final Map<String, String> errors = new ConcurrentHashMap<>();

    private final CompletableFuture<Void> connected = new CompletableFuture<>();
    private volatile boolean failed;

    private OkHttpClient client;
    private WebSocket webSocket;

    /** Opens the connection and blocks until the socket is established. */
    BidiSoakClient connect(String uri) {
        client = new OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
        var request = new Request.Builder().url(uri).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                connected.complete(null);
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                dispatch(text);
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                failed = true;
                if (!connected.isDone()) {
                    connected.completeExceptionally(t);
                }
                errors.put("__socket__", String.valueOf(t));
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                if (code != 1000) {
                    failed = true;
                    errors.put("__socket__", "closed code=" + code + " reason=" + reason);
                }
            }
        });
        try {
            connected.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Could not connect bidi soak websocket to " + uri, e);
        }
        return this;
    }

    /** Whether the socket has reported a failure or an unexpected close (checked by the workload's health probe). */
    boolean isHealthy() {
        return !failed;
    }

    private void dispatch(String text) {
        final JsonNode node;
        try {
            node = MAPPER.readTree(text);
        } catch (Exception e) {
            errors.put("__parse__", "unparseable frame: " + e.getMessage());
            return;
        }
        var corr = textOrNull(node, "correlationId");
        switch (node.path("type").asText("")) {
            case BidiServerMessage.COMMAND_RESPONSE -> commandResponses.put(corr, new CommandResponse(
                    node.path("status").asText(null), textOrNull(node, "cacheId"),
                    textOrNull(node, "key"), textOrNull(node, "value")));
            case BidiServerMessage.ENTRIES -> {
                var items = entriesAccumulated.computeIfAbsent(corr, k -> new ConcurrentHashMap<>());
                var itemsNode = node.get("items");
                if (itemsNode != null) {
                    itemsNode.fields().forEachRemaining(e -> items.put(e.getKey(), e.getValue().asText()));
                }
                entriesStatus.put(corr, node.path("status").asText(null));
                if (node.path("endOfBatch").asBoolean(false)) {
                    entriesComplete.put(corr, Boolean.TRUE);
                }
            }
            case BidiServerMessage.SUBSCRIBED -> subscribeAcks.put(corr, Boolean.TRUE);
            case BidiServerMessage.STREAM_UPDATE -> streamUpdates.add(new StreamUpdate(
                    corr, node.path("eventType").asText(null), textOrNull(node, "cacheId"),
                    textOrNull(node, "key"), textOrNull(node, "value")));
            case BidiServerMessage.ERROR -> errors.put(corr == null ? "__noCorr__" : corr,
                    node.path("message").asText("error"));
            default -> {
                // stats / timers / bulkResponse are not asserted by the soak workloads; ignore.
            }
        }
    }

    // ---------------------------------------------------------------- send

    void command(WsOp op, String correlationId, String cacheId, String key, String value, long ttl, long counterValue) {
        var frame = MAPPER.createObjectNode();
        frame.put("type", BidiClientMessage.COMMAND);
        frame.put("op", op.name());
        frame.put("correlationId", correlationId);
        if (cacheId != null) {
            frame.put("cacheId", cacheId);
        }
        if (key != null) {
            frame.put("key", key);
        }
        if (value != null) {
            frame.put("value", value);
        }
        frame.put("ttl", ttl);
        frame.put("counterValue", counterValue);
        webSocket.send(frame.toString());
    }

    void subscribe(String correlationId, List<Selector> selectors, boolean counters, boolean sendSnapshot) {
        var frame = MAPPER.createObjectNode();
        frame.put("type", BidiClientMessage.SUBSCRIBE);
        frame.put("correlationId", correlationId);
        frame.put("counters", counters);
        frame.put("sendSnapshot", sendSnapshot);
        ArrayNode caches = frame.putArray("caches");
        for (var selector : selectors) {
            ObjectNode node = caches.addObject();
            node.put("cacheId", selector.cacheId());
            if (selector.key() != null) {
                node.put("key", selector.key());
            }
            node.put("mode", selector.mode());
        }
        webSocket.send(frame.toString());
    }

    void unsubscribe(String correlationId, String cacheId, boolean counters) {
        var frame = MAPPER.createObjectNode();
        frame.put("type", BidiClientMessage.UNSUBSCRIBE);
        frame.put("correlationId", correlationId);
        frame.put("counters", counters);
        frame.put("cacheId", cacheId);
        webSocket.send(frame.toString());
    }

    @Override
    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "soak complete");
        }
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        var value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
