package com.bhf.aeroncache.integration.streaming;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.models.requests.SubscriptionMode;
import com.bhf.aeroncache.ws.bidi.messages.BidiClientMessage;
import com.bhf.aeroncache.ws.bidi.messages.BidiServerMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * A {@link StreamingHelper} that observes cache updates over the bidirectional websocket endpoint
 * ({@code /api/ws/v1/bidi}) rather than the one-directional connect-URL routes.
 *
 * <p>Each subscription is expressed as a {@code subscribe} JSON frame sent once the socket opens; the
 * helper then collects {@code streamUpdate} frames and adapts them back into the shared
 * {@link CacheUpdateEvent} the abstract streaming test suites assert on. This lets the existing streaming
 * suites run unchanged against the BIDI subscribe path &mdash; mutations still go over HTTP, updates arrive
 * over BIDI.</p>
 *
 * <p>The {@code counters} flag selects the counters cache flavour (mirroring the separate
 * cache/counters endpoint providers used by {@link WSStreamingHelper}).</p>
 */
public class BidiWsStreamingHelper implements StreamingHelper {

    private static final String BIDI_ENDPOINT = "/api/ws/v1/bidi";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final boolean counters;

    public BidiWsStreamingHelper() {
        this(false);
    }

    public BidiWsStreamingHelper(boolean counters) {
        this.counters = counters;
    }

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEvents(BackendTestResource backend, String cacheId, int count, CompletableFuture<Void> ready) {
        return subscribeAndCollect(backend, List.of(cacheId), null, SubscriptionMode.FULL, false, count, ready);
    }

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEventsMultipleCaches(BackendTestResource backend, List<String> cacheIds, int count, CompletableFuture<Void> ready) {
        return subscribeAndCollect(backend, cacheIds, null, SubscriptionMode.FULL, false, count, ready);
    }

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEventsWithHydration(BackendTestResource backend, String cacheId, int count, CompletableFuture<Void> ready) {
        return subscribeAndCollect(backend, List.of(cacheId), null, SubscriptionMode.FULL, true, count, ready);
    }

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEventsMultipleCachesWithHydration(BackendTestResource backend, List<String> cacheIds, int count, CompletableFuture<Void> ready) {
        return subscribeAndCollect(backend, cacheIds, null, SubscriptionMode.FULL, true, count, ready);
    }

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEventsForKeys(BackendTestResource backend, String cacheId, List<String> keys, int count, CompletableFuture<Void> ready) {
        List<String> cacheIds = new ArrayList<>();
        List<String> keyList = new ArrayList<>();
        for (var key : keys) {
            cacheIds.add(cacheId);
            keyList.add(key);
        }
        return subscribeAndCollect(backend, cacheIds, keyList, SubscriptionMode.FULL, false, count, ready);
    }

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getPatchEvents(BackendTestResource backend, String cacheId, int count, CompletableFuture<Void> ready) {
        return subscribeAndCollect(backend, List.of(cacheId), null, SubscriptionMode.PATCH, false, count, ready);
    }

    private CompletableFuture<List<CacheUpdateEvent>> subscribeAndCollect(BackendTestResource backend, List<String> cacheIds,
                                                                         List<String> keys, SubscriptionMode mode,
                                                                         boolean sendSnapshot, int count,
                                                                         CompletableFuture<Void> ready) {
        var latch = new CountDownLatch(count);
        var messageFuture = new CompletableFuture<List<CacheUpdateEvent>>();
        var events = new ArrayList<CacheUpdateEvent>();
        var uri = backend.getBaseWsUri() + ":" + backend.getWsPort() + BIDI_ENDPOINT;
        var subscribeFrame = buildSubscribe(cacheIds, keys, mode, sendSnapshot);
        connect(uri, subscribeFrame, latch, messageFuture, events, ready);
        return messageFuture;
    }

    private String buildSubscribe(List<String> cacheIds, List<String> keys, SubscriptionMode mode, boolean sendSnapshot) {
        ObjectNode frame = OBJECT_MAPPER.createObjectNode();
        frame.put("type", BidiClientMessage.SUBSCRIBE);
        frame.put("correlationId", UUID.randomUUID().toString());
        frame.put("counters", counters);
        frame.put("sendSnapshot", sendSnapshot);
        ArrayNode caches = frame.putArray("caches");
        for (int i = 0; i < cacheIds.size(); i++) {
            ObjectNode selector = caches.addObject();
            selector.put("cacheId", cacheIds.get(i));
            var key = keys == null ? null : keys.get(i);
            if (key != null) {
                selector.put("key", key);
            }
            selector.put("mode", mode.name());
        }
        return frame.toString();
    }

    private void connect(String uri, String subscribeFrame, CountDownLatch latch,
                         CompletableFuture<List<CacheUpdateEvent>> messageFuture, List<CacheUpdateEvent> events,
                         CompletableFuture<Void> ready) {
        var client = new OkHttpClient();
        var request = new Request.Builder().url(uri).build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                // Connection open is not readiness: readiness is the subscription ack (see onMessage).
                System.out.println("BIDI WS OPEN");
                webSocket.send(subscribeFrame);
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                System.out.println("BIDI WS MESSAGE: " + text);
                final JsonNode node;
                try {
                    node = OBJECT_MAPPER.readTree(text);
                } catch (Exception e) {
                    messageFuture.completeExceptionally(e);
                    return;
                }
                var type = node.path("type").asText("");
                // The subscription-confirmed ack marks the point from which updates are guaranteed.
                if (BidiServerMessage.SUBSCRIBED.equals(type)) {
                    ready.complete(null);
                    return;
                }
                if (!BidiServerMessage.STREAM_UPDATE.equals(type)) {
                    // Ignore command responses / errors / batches on the streaming path.
                    return;
                }
                synchronized (events) {
                    events.add(toCacheUpdateEvent(node));
                    latch.countDown();
                }
                if (latch.getCount() == 0) {
                    System.out.println("COMPLETING ON BIDI WS DATA");
                    messageFuture.complete(events);
                    webSocket.close(1000, "Done");
                }
            }

            @Override
            public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                if (!messageFuture.isDone()) {
                    scheduleReconnect(uri, subscribeFrame, latch, messageFuture, events, ready);
                }
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
                System.out.println("GOT BIDI WS FAILURE: " + t);
                if (!messageFuture.isDone()) {
                    scheduleReconnect(uri, subscribeFrame, latch, messageFuture, events, ready);
                }
            }
        });
    }

    private void scheduleReconnect(String uri, String subscribeFrame, CountDownLatch latch,
                                   CompletableFuture<List<CacheUpdateEvent>> messageFuture, List<CacheUpdateEvent> events,
                                   CompletableFuture<Void> ready) {
        if (messageFuture.isDone()) {
            return;
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            messageFuture.completeExceptionally(e);
            return;
        }
        if (!messageFuture.isDone()) {
            connect(uri, subscribeFrame, latch, messageFuture, events, ready);
        }
    }

    private static CacheUpdateEvent<Object> toCacheUpdateEvent(JsonNode node) {
        return new CacheUpdateEvent<>(
                textOrNull(node, "cacheId"),
                CacheUpdateEvent.EventType.valueOf(node.path("eventType").asText()),
                textOrNull(node, "key"),
                valueOrNull(node, "value"),
                textOrNull(node, "correlationId"));
    }

    private static String textOrNull(JsonNode node, String field) {
        var value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /**
     * Extract a field preserving its native JSON type (number vs string), matching how
     * {@link WSStreamingHelper} deserializes {@link CacheUpdateEvent#itemValue()} for the one-directional
     * routes. Counter values thus arrive as numbers and regular cache values as strings.
     */
    private static Object valueOrNull(JsonNode node, String field) {
        var value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(value, Object.class);
    }
}
