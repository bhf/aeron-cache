package com.bhf.aeroncache.integration.streaming;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.StreamingTestEndpointsProvider;
import org.awaitility.Awaitility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class StreamingHelperUtil {

    public static List<CompletableFuture<List<CacheUpdateEvent>>> getPerStreamEvents(StreamingHelper[] streamingHelpers,
                                                                                     BackendTestResource backend, StreamingTestEndpointsProvider endpointsProvider, String cacheId, int eventCount) {
        List<CompletableFuture<Void>> readyFutures = new ArrayList<>();
        var perStreamingSourceEvents = Arrays.stream(streamingHelpers)
                .map(helper -> {
                    CompletableFuture<Void> ready = new CompletableFuture<>();
                    readyFutures.add(ready);
                    return helper.getEvents(backend, endpointsProvider, cacheId, eventCount, ready);
                })
                .collect(Collectors.toList());

        readyFutures.forEach(f -> Awaitility.await().atMost(60, TimeUnit.SECONDS).until(f::isDone));

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return perStreamingSourceEvents;
    }

    public static List<CompletableFuture<List<CacheUpdateEvent>>> getPerStreamEventsMultipleCaches(StreamingHelper[] streamingHelpers,
                                                                                                   BackendTestResource backend, StreamingTestEndpointsProvider endpointsProvider,
                                                                                                   List<String> cacheIds, int eventCount) {
        List<CompletableFuture<Void>> readyFutures = new ArrayList<>();
        var perStreamingSourceEvents = Arrays.stream(streamingHelpers)
                .map(helper -> {
                    CompletableFuture<Void> ready = new CompletableFuture<>();
                    readyFutures.add(ready);
                    return helper.getEventsMultipleCaches(backend, endpointsProvider, cacheIds, eventCount, ready);
                })
                .collect(Collectors.toList());

        readyFutures.forEach(f -> Awaitility.await().atMost(60, TimeUnit.SECONDS).until(f::isDone));

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return perStreamingSourceEvents;
    }

    public static List<CompletableFuture<List<CacheUpdateEvent>>> getPerStreamEventsWithHydration(StreamingHelper[] streamingHelpers,
                                                                                                  BackendTestResource backend, StreamingTestEndpointsProvider endpointsProvider,
                                                                                                  String cacheId, int eventCount) {
        List<CompletableFuture<Void>> readyFutures = new ArrayList<>();
        var perStreamingSourceEvents = Arrays.stream(streamingHelpers)
                .map(helper -> {
                    CompletableFuture<Void> ready = new CompletableFuture<>();
                    readyFutures.add(ready);
                    return helper.getEventsWithHydration(backend, endpointsProvider, cacheId, eventCount, ready);
                })
                .collect(Collectors.toList());

        readyFutures.forEach(f -> Awaitility.await().atMost(60, TimeUnit.SECONDS).until(f::isDone));

        return perStreamingSourceEvents;
    }

    public static List<CompletableFuture<List<CacheUpdateEvent>>> getPerStreamEventsMultipleCachesWithHydration(StreamingHelper[] streamingHelpers,
                                                                                                  BackendTestResource backend, StreamingTestEndpointsProvider endpointsProvider,
                                                                                                                List<String> cacheIds, int eventCount) {
        List<CompletableFuture<Void>> readyFutures = new ArrayList<>();
        var perStreamingSourceEvents = Arrays.stream(streamingHelpers)
                .map(helper -> {
                    CompletableFuture<Void> ready = new CompletableFuture<>();
                    readyFutures.add(ready);
                    return helper.getEventsMultipleCachesWithHydration(backend, endpointsProvider, cacheIds, eventCount, ready);
                })
                .collect(Collectors.toList());

        readyFutures.forEach(f -> Awaitility.await().atMost(60, TimeUnit.SECONDS).until(f::isDone));

        return perStreamingSourceEvents;
    }

}
