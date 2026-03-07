package com.bhf.aeroncache.integration.streaming;

import com.bhf.aeroncache.integration.BackendTestResource;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SSEStreamingHelper implements StreamingHelper{

    private static final String STREAMING_API_PREFIX = "/api/sse/v1/cache/";
    private static final String KNOWN_CACHE_ID = "1";

    @Override
    public CompletableFuture<String> getSingleValue(BackendTestResource backend) {
        CountDownLatch latch = new CountDownLatch(1);
        CompletableFuture<String> eventData = new CompletableFuture<>();
        AtomicBoolean isOpen = new AtomicBoolean();

        var httpClient = new OkHttpClient();
        var cacheSubscriptionURI = backend.getBaseSSEUri() + ":"
                + backend.getSsePort() + STREAMING_API_PREFIX + KNOWN_CACHE_ID;

        Request request = new Request.Builder()
                .url(cacheSubscriptionURI)
                .build();

        var factory = EventSources.createFactory(httpClient);
        factory.newEventSource(request, new EventSourceListener() {

            @Override
            public void onEvent(@NotNull okhttp3.sse.EventSource eventSource, @Nullable String id, @Nullable String type, @NotNull String data) {
                eventData.complete(data);
                latch.countDown();
            }

            @Override
            public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
                isOpen.set(true);
            }
        });

        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAtomic(isOpen, Matchers.equalTo(true));

        return eventData;

    }
}
