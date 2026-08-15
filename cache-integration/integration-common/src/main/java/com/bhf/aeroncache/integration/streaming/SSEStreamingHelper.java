package com.bhf.aeroncache.integration.streaming;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.StreamingTestEndpointsProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class SSEStreamingHelper implements StreamingHelper{

    private static final String STREAMING_API_PREFIX = "/api/sse/v1/cache/";
    private static final String STREAMING_MULTI_CACHE_API_PREFIX = "/api/sse/v1/caches/";
    private static final String STREAMING_HYDRATION_API_PREFIX = "/api/sse/v1/cache/hydrate/";
    private static final String STREAMING_MULTI_CACHE_HYDRATION_API_PREFIX = "/api/sse/v1/caches/hydrate/";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEvents(BackendTestResource backend, StreamingTestEndpointsProvider endpointsProvider, String cacheId, int count, CompletableFuture<Void> connectionReady) {
        CountDownLatch latch = new CountDownLatch(count);
        List<CacheUpdateEvent> events = new ArrayList<>();
        CompletableFuture<List<CacheUpdateEvent>> eventData = new CompletableFuture<>();

        var cacheSubscriptionURI = backend.getBaseSSEUri() + ":"
                + backend.getSsePort() + STREAMING_API_PREFIX + cacheId;

        connect(cacheSubscriptionURI, latch, eventData, events, connectionReady);

        return eventData;
    }

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEventsMultipleCaches(BackendTestResource backend, StreamingTestEndpointsProvider endpointsProvider, List<String> cacheIds, int count, CompletableFuture<Void> connectionReady) {
        CountDownLatch latch = new CountDownLatch(count);
        List<CacheUpdateEvent> events = new ArrayList<>();
        CompletableFuture<List<CacheUpdateEvent>> eventData = new CompletableFuture<>();

        var cacheSubscriptionURI = backend.getBaseSSEUri() + ":"
                + backend.getSsePort() + STREAMING_MULTI_CACHE_API_PREFIX + String.join(",", cacheIds);

        connect(cacheSubscriptionURI, latch, eventData, events, connectionReady);

        return eventData;
    }

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEventsWithHydration(BackendTestResource backend, StreamingTestEndpointsProvider endpointsProvider, String cacheId, int count, CompletableFuture<Void> connectionReady) {
        CountDownLatch latch = new CountDownLatch(count);
        List<CacheUpdateEvent> events = new ArrayList<>();
        CompletableFuture<List<CacheUpdateEvent>> eventData = new CompletableFuture<>();

        var cacheSubscriptionURI = backend.getBaseSSEUri() + ":"
                + backend.getSsePort() + STREAMING_HYDRATION_API_PREFIX + cacheId;

        connect(cacheSubscriptionURI, latch, eventData, events, connectionReady);

        return eventData;
    }

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEventsMultipleCachesWithHydration(BackendTestResource backend, StreamingTestEndpointsProvider endpointsProvider, List<String> cacheIds, int count, CompletableFuture<Void> connectionReady) {
        CountDownLatch latch = new CountDownLatch(count);
        List<CacheUpdateEvent> events = new ArrayList<>();
        CompletableFuture<List<CacheUpdateEvent>> eventData = new CompletableFuture<>();

        var cacheSubscriptionURI = backend.getBaseSSEUri() + ":"
                + backend.getSsePort() + STREAMING_MULTI_CACHE_HYDRATION_API_PREFIX + String.join(",", cacheIds);

        connect(cacheSubscriptionURI, latch, eventData, events, connectionReady);

        return eventData;
    }

    private void connect(String uri, CountDownLatch latch, CompletableFuture<List<CacheUpdateEvent>> eventData, List<CacheUpdateEvent> events, CompletableFuture<Void> connectionReady) {

        Request request = new Request.Builder().url(uri).build();
        var httpClient = new OkHttpClient();
        var factory = EventSources.createFactory(httpClient);

        factory.newEventSource(request, new EventSourceListener() {
            @Override
            public void onEvent(@NotNull EventSource eventSource, @Nullable String id, @Nullable String type, @NotNull String data) {
                try {
                    CacheUpdateEvent event = OBJECT_MAPPER.readValue(data, CacheUpdateEvent.class);
                    synchronized (events) {
                        events.add(event);
                        latch.countDown();
                    }
                } catch (JsonProcessingException e) {
                    eventData.completeExceptionally(e);
                }
                if (latch.getCount() == 0) {
                    eventData.complete(events);
                }
            }

            @Override
            public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
                System.out.println("SSE CONNECTION NOW OPEN");
                connectionReady.complete(null);
            }

            @Override
            public void onFailure(@NotNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
                System.out.println("GOT SSE FAILURE: " + eventSource.request());
                if (!eventData.isDone()) {
                    System.out.println("SSE failure before all events received – reconnecting");
                    scheduleReconnect(uri, latch, eventData, events, connectionReady);
                }
            }
        });
    }

    private void scheduleReconnect(String uri, CountDownLatch latch, CompletableFuture<List<CacheUpdateEvent>> eventData,
                                   List<CacheUpdateEvent> events, CompletableFuture<Void> connectionReady) {
        if (eventData.isDone()) return;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            eventData.completeExceptionally(e);
            return;
        }
        if (!eventData.isDone()) {
            System.out.println("SSE reconnecting to " + uri);
            connect(uri, latch, eventData, events, connectionReady);
        }
    }
}
