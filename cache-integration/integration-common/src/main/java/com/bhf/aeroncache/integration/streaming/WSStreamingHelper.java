package com.bhf.aeroncache.integration.streaming;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class WSStreamingHelper implements StreamingHelper {

    private static final String STREAMING_API_PREFIX = "/api/ws/v1/cache/";
    private static final String KNOWN_CACHE_ID = "1";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public CompletableFuture<List<CacheUpdateEvent>> getEvents(BackendTestResource backend, int count, CompletableFuture<Void> ready) {
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
                        System.out.println("WS OPEN");
                        isOpen.set(true);
                        ready.complete(null);
                        webSocket.request(1);
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        System.out.println("WS CLOSED");
                        isOpen.set(false);
                        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        try {
                            CacheUpdateEvent event = OBJECT_MAPPER.readValue(data.toString(), CacheUpdateEvent.class);
                            synchronized (events) {
                                events.add(event);
                                latch.countDown();
                            }
                        } catch (JsonProcessingException e) {
                            messageFuture.completeExceptionally(e);
                        }

                        if (latch.getCount() == 0) {
                            System.out.println("COMPLETING ON WS DATA");
                            messageFuture.complete(events);
                            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Done");
                        } else {
                            webSocket.request(1);
                        }
                        return null;
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        System.out.println("GOT WS ERROR");
                        ready.completeExceptionally(error != null ? error : new RuntimeException("WS Failure"));
                        messageFuture.completeExceptionally(error);
                    }
                });

        return messageFuture;
    }
}
