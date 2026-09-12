package com.bhf.aeroncache.integration.streaming;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.http.responses.SubscriptionAck;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.StreamingTestEndpointsProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

@RequiredArgsConstructor
public class WSStreamingHelper implements StreamingHelper {


    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final StreamingTestEndpointsProvider endpointsProvider;

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEvents(BackendTestResource backend, String cacheId, int count, CompletableFuture<Void> ready) {
        CountDownLatch latch = new CountDownLatch(count);
        CompletableFuture<List<CacheUpdateEvent>> messageFuture = new CompletableFuture<>();
        List<CacheUpdateEvent> events = new ArrayList<>();

        var cacheSubscriptionURI = backend.getBaseWsUri() + ":"
                + backend.getWsPort() + endpointsProvider.getStreamingApiPrefix() + cacheId;

        connect(cacheSubscriptionURI, latch, messageFuture, events, ready);

        return messageFuture;
    }

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEventsMultipleCaches(BackendTestResource backend, List<String> cacheIds, int count, CompletableFuture<Void> ready) {
        CountDownLatch latch = new CountDownLatch(count);
        CompletableFuture<List<CacheUpdateEvent>> messageFuture = new CompletableFuture<>();
        List<CacheUpdateEvent> events = new ArrayList<>();

        var cacheSubscriptionURI = backend.getBaseWsUri() + ":"
                + backend.getWsPort() + endpointsProvider.getStreamingMultiCacheApiPrefix() + String.join(",", cacheIds);

        connect(cacheSubscriptionURI, latch, messageFuture, events, ready);

        return messageFuture;
    }

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEventsWithHydration(BackendTestResource backend, String cacheId, int count, CompletableFuture<Void> ready) {
        CountDownLatch latch = new CountDownLatch(count);
        CompletableFuture<List<CacheUpdateEvent>> messageFuture = new CompletableFuture<>();
        List<CacheUpdateEvent> events = new ArrayList<>();

        var cacheSubscriptionURI = backend.getBaseWsUri() + ":"
                + backend.getWsPort() + endpointsProvider.getStreamingHydrateApiPrefix() + cacheId;

        connect(cacheSubscriptionURI, latch, messageFuture, events, ready);

        return messageFuture;
    }

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEventsMultipleCachesWithHydration(BackendTestResource backend, List<String> cacheIds, int count, CompletableFuture<Void> ready) {
        CountDownLatch latch = new CountDownLatch(count);
        CompletableFuture<List<CacheUpdateEvent>> messageFuture = new CompletableFuture<>();
        List<CacheUpdateEvent> events = new ArrayList<>();

        var cacheSubscriptionURI = backend.getBaseWsUri() + ":"
                + backend.getWsPort() + endpointsProvider.getStreamingMultiCacheHydrateApiPrefix() + String.join(",", cacheIds);

        connect(cacheSubscriptionURI, latch, messageFuture, events, ready);

        return messageFuture;
    }

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEventsForKeys(BackendTestResource backend, String cacheId, List<String> keys, int count, CompletableFuture<Void> ready) {
        CountDownLatch latch = new CountDownLatch(count);
        CompletableFuture<List<CacheUpdateEvent>> messageFuture = new CompletableFuture<>();
        List<CacheUpdateEvent> events = new ArrayList<>();

        var cacheSubscriptionURI = backend.getBaseWsUri() + ":"
                + backend.getWsPort() + endpointsProvider.getStreamingApiPrefix() + cacheId
                + "?keys=" + String.join(",", keys);

        connect(cacheSubscriptionURI, latch, messageFuture, events, ready);

        return messageFuture;
    }

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getPatchEvents(BackendTestResource backend, String cacheId, int count, CompletableFuture<Void> ready) {
        CountDownLatch latch = new CountDownLatch(count);
        CompletableFuture<List<CacheUpdateEvent>> messageFuture = new CompletableFuture<>();
        List<CacheUpdateEvent> events = new ArrayList<>();

        var cacheSubscriptionURI = backend.getBaseWsUri() + ":"
                + backend.getWsPort() + endpointsProvider.getStreamingApiPrefix() + cacheId
                + "?mode=patch";

        connect(cacheSubscriptionURI, latch, messageFuture, events, ready);

        return messageFuture;
    }

    private void connect(String uri, CountDownLatch latch, CompletableFuture<List<CacheUpdateEvent>> messageFuture, List<CacheUpdateEvent> events, CompletableFuture<Void> ready) {

        var client = new OkHttpClient();
        var request = new Request.Builder().url(uri).build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                // Connection open is not readiness: readiness is the subscription ack (see onMessage).
                System.out.println("WS OPEN");
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                System.out.println("WS MESSAGE: " + text);
                final JsonNode node;
                try {
                    node = OBJECT_MAPPER.readTree(text);
                } catch (JsonProcessingException e) {
                    messageFuture.completeExceptionally(e);
                    return;
                }
                // The subscription-confirmed ack marks the point from which updates are guaranteed.
                if (SubscriptionAck.SUBSCRIBED.equals(node.path("type").asText(null))) {
                    ready.complete(null);
                    return;
                }
                final CacheUpdateEvent event;
                try {
                    event = OBJECT_MAPPER.treeToValue(node, CacheUpdateEvent.class);
                } catch (JsonProcessingException e) {
                    messageFuture.completeExceptionally(e);
                    return;
                }
                synchronized (events) {
                    events.add(event);
                    latch.countDown();
                }
                if (latch.getCount() == 0) {
                    System.out.println("COMPLETING ON WS DATA");
                    messageFuture.complete(events);
                    webSocket.close(1000, "Done");
                }
            }

            @Override
            public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                System.out.println("WS CLOSING: " + code + " " + reason);
                if (!messageFuture.isDone()) {
                    System.out.println("WS closed before all events received – reconnecting");
                    scheduleReconnect(uri, latch, messageFuture, events, ready);
                }
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
                System.out.println("GOT WS FAILURE: " + t);
                if (!messageFuture.isDone()) {
                    System.out.println("WS failure before all events received – reconnecting");
                    scheduleReconnect(uri, latch, messageFuture, events, ready);
                }
            }
        });
    }

    private void scheduleReconnect(String uri,
                                   CountDownLatch latch,
                                   CompletableFuture<List<CacheUpdateEvent>> messageFuture,
                                   List<CacheUpdateEvent> events,
                                   CompletableFuture<Void> ready) {
        if (messageFuture.isDone()) return;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            messageFuture.completeExceptionally(e);
            return;
        }
        if (!messageFuture.isDone()) {
            System.out.println("WS reconnecting to " + uri);
            connect(uri, latch, messageFuture, events, ready);
        }
    }
}
