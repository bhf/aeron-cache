package com.bhf.aeroncache.integration.clients;

import com.bhf.aeroncache.integration.BackendTestResource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class WSStreamingHelper implements StreamingHelper{

    private static final String STREAMING_API_PREFIX = "/api/ws/v1/cache/";
    private static final String KNOWN_CACHE_ID = "1";

    @Override
    public CompletableFuture<String> getSingleValue(BackendTestResource backend) {
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        var httpClient = HttpClient.newHttpClient();
        var cacheSubscriptionURI = backend.getBaseWsUri() + ":"
                + backend.getWsPort() + STREAMING_API_PREFIX + KNOWN_CACHE_ID;

        httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(cacheSubscriptionURI), new WebSocket.Listener() {

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket,
                                                     CharSequence data,
                                                     boolean last) {
                        messageFuture.complete(data.toString());
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }
                }).join();

        return messageFuture;
    }
}
