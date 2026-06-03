package com.bhf.aeroncache.integration.streaming;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.integration.BackendTestResource;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface StreamingHelper {
    CompletableFuture<List<CacheUpdateEvent>> getEvents(BackendTestResource backend, String cacheId, int count, CompletableFuture<Void> ready);
    CompletableFuture<List<CacheUpdateEvent>> getEventsMultipleCaches(BackendTestResource backend, List<String> cacheIds, int count, CompletableFuture<Void> ready);
    CompletableFuture<List<CacheUpdateEvent>> getEventsWithHydration(BackendTestResource backend, String cacheId, int count, CompletableFuture<Void> ready);
    CompletableFuture<List<CacheUpdateEvent>> getEventsMultipleCachesWithHydration(BackendTestResource backend, List<String> cacheIds, int count, CompletableFuture<Void> ready);
}
