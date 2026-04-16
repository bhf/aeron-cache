package com.bhf.aeroncache.integration.streaming;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class WSStreamingHelper implements StreamingHelper{

    private static final String STREAMING_API_PREFIX = "/api/ws/v1/cache/";
    private static final String KNOWN_CACHE_ID = "1";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEvents(BackendTestResource backend, int count) {
        CountDownLatch latch = new CountDownLatch(count);
        CompletableFuture<List<CacheUpdateEvent>> messageFuture = new CompletableFuture<>();
        List<CacheUpdateEvent> events = new ArrayList<>();

        var cacheSubscriptionURI = backend.getBaseWsUri() + ":"
                + backend.getWsPort() + STREAMING_API_PREFIX + KNOWN_CACHE_ID;

        var httpClient = HttpClient.newHttpClient();
        AtomicBoolean isOpen = new AtomicBoolean();

        var socketFuture = httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(cacheSubscriptionURI), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        isOpen.set(true);
                        WebSocket.Listener.super.onOpen(webSocket);
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        isOpen.set(false);
                        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        try {
                            CacheUpdateEvent event = OBJECT_MAPPER.readValue(data.toString(), CacheUpdateEvent.class);
                            synchronized (events) {
                                events.add(event);
                            }
                        } catch (JsonProcessingException e) {
                            messageFuture.completeExceptionally(e);
                        }

                        latch.countDown();
                        if (latch.getCount() == 0) {
                            messageFuture.complete(events);
                            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Done");
                        }
                        return null;
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        messageFuture.completeExceptionally(error);
                        WebSocket.Listener.super.onError(webSocket, error);
                    }
                });

        socketFuture.join();
        Awaitility.await().atMost(60, TimeUnit.SECONDS).untilAtomic(isOpen, Matchers.equalTo(true));

        return messageFuture;
    }
}
