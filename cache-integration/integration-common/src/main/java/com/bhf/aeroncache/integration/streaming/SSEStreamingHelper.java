package com.bhf.aeroncache.integration.streaming;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.integration.BackendTestResource;
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
    private static final String KNOWN_CACHE_ID = "1";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEvents(BackendTestResource backend, int count, CompletableFuture<Void> connectionReady) {
        CountDownLatch latch = new CountDownLatch(count);
        List<CacheUpdateEvent> events = new ArrayList<>();
        CompletableFuture<List<CacheUpdateEvent>> eventData = new CompletableFuture<>();

        var cacheSubscriptionURI = backend.getBaseSSEUri() + ":"
                + backend.getSsePort() + STREAMING_API_PREFIX + KNOWN_CACHE_ID;

        Request request = new Request.Builder()
                .url(cacheSubscriptionURI)
                .build();

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
                System.out.println("GOT SSE FAILURE:"+eventSource.request());
                connectionReady.completeExceptionally(t != null ? t : new RuntimeException("SSE Failure"));
                eventData.completeExceptionally(t != null ? t : new RuntimeException("SSE Failure"));
            }
        });

        return eventData;
    }

}
