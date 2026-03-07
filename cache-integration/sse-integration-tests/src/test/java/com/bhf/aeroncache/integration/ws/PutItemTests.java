package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.clients.SSEStreamingHelper;
import com.bhf.aeroncache.integration.clients.StreamingHelper;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(BackendTestLauncher.class)
@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = true)
class PutItemTests {

    private static final String KNOWN_CACHE_ID = "1";
    private static final String KNOWN_KEY = "SomeKey";
    private static final String KNOWN_VALUE = "SomeValue";
    private final StreamingHelper streamingHelper = new SSEStreamingHelper();

    @BeforeAll
    static void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend);
    }

    @Test
    @DisplayName("Should get a streaming update when putting into a known cache")
    @HappyPath
    void shouldGetStreamingUpdateWhenPuttingIntoKnownCache(BackendTestResource backend) {
        // Arrange
        var eventData = streamingHelper.getSingleValue(backend);

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
