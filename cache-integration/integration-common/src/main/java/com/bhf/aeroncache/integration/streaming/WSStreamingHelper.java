package com.bhf.aeroncache.integration.streaming;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.StreamingTestEndpointsProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class WSStreamingHelper implements StreamingHelper {

    private static final String STREAMING_API_PREFIX = "/api/ws/v1/cache/";
    private static final String STREAMING_MULTI_CACHE_API_PREFIX = "/api/ws/v1/caches/";
    private static final String STREAMING_HYDRATE_API_PREFIX = "/api/ws/v1/cache/hydrate/";
    private static final String STREAMING_MULTI_CACHE_HYDRATE_API_PREFIX = "/api/ws/v1/caches/hydrate/";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEvents(BackendTestResource backend, StreamingTestEndpointsProvider endpointsProvider, String cacheId, int count, CompletableFuture<Void> ready) {
        CountDownLatch latch = new CountDownLatch(count);
        CompletableFuture<List<CacheUpdateEvent>> messageFuture = new CompletableFuture<>();
        List<CacheUpdateEvent> events = new ArrayList<>();

        var cacheSubscriptionURI = backend.getBaseWsUri() + ":"
                + backend.getWsPort() + STREAMING_API_PREFIX + cacheId;

        connect(cacheSubscriptionURI, latch, messageFuture, events, ready);

        return messageFuture;
    }

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEventsMultipleCaches(BackendTestResource backend, StreamingTestEndpointsProvider endpointsProvider, List<String> cacheIds, int count, CompletableFuture<Void> ready) {
        CountDownLatch latch = new CountDownLatch(count);
        CompletableFuture<List<CacheUpdateEvent>> messageFuture = new CompletableFuture<>();
        List<CacheUpdateEvent> events = new ArrayList<>();

        var cacheSubscriptionURI = backend.getBaseWsUri() + ":"
                + backend.getWsPort() + STREAMING_MULTI_CACHE_API_PREFIX + String.join(",", cacheIds);

        connect(cacheSubscriptionURI, latch, messageFuture, events, ready);

        return messageFuture;
    }

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEventsWithHydration(BackendTestResource backend, StreamingTestEndpointsProvider endpointsProvider, String cacheId, int count, CompletableFuture<Void> ready) {
        CountDownLatch latch = new CountDownLatch(count);
        CompletableFuture<List<CacheUpdateEvent>> messageFuture = new CompletableFuture<>();
        List<CacheUpdateEvent> events = new ArrayList<>();

        var cacheSubscriptionURI = backend.getBaseWsUri() + ":"
                + backend.getWsPort() + STREAMING_HYDRATE_API_PREFIX + cacheId;

        connect(cacheSubscriptionURI, latch, messageFuture, events, ready);

        return messageFuture;
    }

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEventsMultipleCachesWithHydration(BackendTestResource backend, StreamingTestEndpointsProvider endpointsProvider, List<String> cacheIds, int count, CompletableFuture<Void> ready) {
        CountDownLatch latch = new CountDownLatch(count);
        CompletableFuture<List<CacheUpdateEvent>> messageFuture = new CompletableFuture<>();
        List<CacheUpdateEvent> events = new ArrayList<>();

        var cacheSubscriptionURI = backend.getBaseWsUri() + ":"
                + backend.getWsPort() + STREAMING_MULTI_CACHE_HYDRATE_API_PREFIX + String.join(",", cacheIds);

        connect(cacheSubscriptionURI, latch, messageFuture, events, ready);

        return messageFuture;
    }

    private void connect(String uri, CountDownLatch latch, CompletableFuture<List<CacheUpdateEvent>> messageFuture, List<CacheUpdateEvent> events, CompletableFuture<Void> ready) {

        var client = new OkHttpClient();
        var request = new Request.Builder().url(uri).build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                System.out.println("WS OPEN");
                ready.complete(null);
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                try {
                    CacheUpdateEvent event = OBJECT_MAPPER.readValue(text, CacheUpdateEvent.class);
                    synchronized (events) {
                        events.add(event);
                        latch.countDown();
                    }
                } catch (JsonProcessingException e) {
                    messageFuture.completeExceptionally(e);
                    return;
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
