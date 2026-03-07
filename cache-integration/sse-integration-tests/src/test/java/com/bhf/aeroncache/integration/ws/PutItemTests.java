package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import io.restassured.http.ContentType;
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
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(BackendTestLauncher.class)
@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = true)
class PutItemTests {

    private static final String STREAMING_API_PREFIX = "/api/sse/v1/cache/";
    private static final String PUT_ITEM_ENDPOINT = "/api/v1/cache/";
    private static final String KNOWN_CACHE_ID = "1";
    private static final String KNOWN_KEY = "SomeKey";
    private static final String KNOWN_VALUE = "SomeValue";

    @BeforeAll
    static void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend);
    }

    @Test
    @DisplayName("Should get a streaming update when putting into a known cache")
    @HappyPath
    void shouldGetStreamingUpdateWhenPuttingIntoKnownCache(BackendTestResource backend) {
        // Arrange
        var httpClient = new OkHttpClient();
        var cacheSubscriptionURI = backend.getBaseSSEUri() + ":"
                + backend.getSsePort() + STREAMING_API_PREFIX + KNOWN_CACHE_ID;

        Request request = new Request.Builder()
                .url(cacheSubscriptionURI)
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> eventData = new AtomicReference<>();
        AtomicBoolean isOpen = new AtomicBoolean();

        var factory = EventSources.createFactory(httpClient);
        factory.newEventSource(request, new EventSourceListener() {

            @Override
            public void onEvent(@NotNull okhttp3.sse.EventSource eventSource, @Nullable String id, @Nullable String type, @NotNull String data) {
                eventData.set(data);
                latch.countDown();
            }

            @Override
            public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
                isOpen.set(true);
            }
        });

        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAtomic(isOpen, Matchers.equalTo(true));

        // Act
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE, backend);

        // Assert
         Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        assertThat(eventData.get(), Matchers.notNullValue())
                );
    }

}
