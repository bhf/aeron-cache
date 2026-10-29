package com.bhf.aeroncache.http.handlers;

import com.bhf.aeroncache.http.application.HttpApplication;
import com.bhf.aeroncache.http.responses.CacheDetails;
import com.bhf.aeroncache.http.responses.CacheItem;
import com.bhf.aeroncache.http.responses.GetCacheResponse;
import com.bhf.aeroncache.http.responses.GetItemResponse;
import com.bhf.aeroncache.http.responses.RequestErrorResponse;
import com.bhf.aeroncache.http.responses.StopNearCacheResponse;
import com.bhf.aeroncache.models.ErrorMessages;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.near.NearCache;
import com.bhf.aeroncache.near.NearCacheManager;
import com.bhf.aeroncache.services.cache.impl.ObservingCacheRequestPublisher;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.HTTPStatusUtils;
import io.javalin.http.Context;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Handlers for the near cache read endpoints ({@code GET /api/v1/near/cache/<cacheId>} and
 * {@code GET /api/v1/near/cache/<cacheId>/<key>}).
 *
 * <p>Reads are served from a local {@link NearCacheManager} that is kept warm by a cluster
 * subscription: the first read of a cache subscribes to it (requesting a snapshot), and subsequent
 * update events populate the local copy. A read that misses the local cache falls back to fetching
 * from the source cache over the cluster. These handlers are only wired up by
 * {@link HttpApplication} when the near cache is enabled via the {@code NEAR_CACHE_ENABLED}
 * environment variable.
 */
@Log4j2
public class NearCacheRouteHandlers {

    private final ObservingCacheRequestPublisher<ReusableString, ReusableString, ReusableString, String, String, String> publisher;
    private final NearCacheManager<String> nearCacheManager;
    private final Consumer<String> subscribeToCache;
    private final Consumer<String> unsubscribeFromCache;

    /**
     * @param publisher            the publisher used to fetch from the source cache over the cluster.
     * @param nearCacheManager     the local near cache store kept warm by subscription updates.
     * @param subscribeToCache     subscribes to a cache's updates so the local copy is kept in sync.
     * @param unsubscribeFromCache drops the cluster subscription for a cache so updates stop flowing.
     */
    public NearCacheRouteHandlers(ObservingCacheRequestPublisher<ReusableString, ReusableString, ReusableString, String, String, String> publisher,
                                  NearCacheManager<String> nearCacheManager,
                                  Consumer<String> subscribeToCache,
                                  Consumer<String> unsubscribeFromCache) {
        this.publisher = publisher;
        this.nearCacheManager = nearCacheManager;
        this.subscribeToCache = subscribeToCache;
        this.unsubscribeFromCache = unsubscribeFromCache;
    }

    /**
     * List the caches currently held warm in the near cache, with their local item counts.
     *
     * @param ctx The context.
     */
    public void handleGetNearCachesRequest(Context ctx) {
        log.info("Got request to list warm near caches");
        List<CacheDetails> warmCaches = new ArrayList<>();
        for (var cacheId : nearCacheManager.cacheIds()) {
            var localCache = nearCacheManager.get(cacheId);
            var itemCount = localCache == null ? 0 : localCache.size();
            warmCaches.add(new CacheDetails(cacheId, itemCount));
        }
        ctx.status(HTTPStatusUtils.OK);
        ctx.json(warmCaches);
    }

    /**
     * Stop near caching a cache: drop the cluster subscription so updates stop flowing and remove the
     * local copy. Idempotent - stopping a cache that is not currently warm succeeds with
     * {@code wasCached=false}.
     *
     * @param ctx The context.
     */
    public void handleStopNearCacheRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            log.info("Got request to stop near caching cacheId {}", cacheId);

            unsubscribeFromCache.accept(cacheId);
            var wasCached = nearCacheManager.remove(cacheId);

            ctx.status(HTTPStatusUtils.OK);
            ctx.json(new StopNearCacheResponse(cacheId, wasCached, CacheOperationStatus.SUCCESS));
        } catch (Exception e) {
            var errorMsg = "Badly formed request to stop near caching cacheId " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES,
                    CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    /**
     * Handle a request to get an item from a cache.
     *
     * @param ctx The context.
     */
    public void handleGetItemRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            var key = ctx.pathParam("key");
            log.info("Got get item request on cacheId {}, key {}", cacheId, key);

            // check if the item exists in the local cache, otherwise get it from the remote cache
            if (nearCacheManager.contains(cacheId)) {
                var localCache = nearCacheManager.get(cacheId);
                handleExistingLocalCache(ctx, localCache, key, cacheId);
            } else {
                handleNewNearCacheRequest(ctx, cacheId, key);
            }
        } catch (Exception e) {
            var errorMsg = "Badly formed request to get item with key " + ctx.pathParam("key") + " from cache with Id: " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES,
                    CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    /**
     * Handle a request to get a whole cache snapshot from the local near cache.
     *
     * @param ctx The context.
     */
    public void handleGetCacheRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            log.info("Got get cache content request on cacheId {}", cacheId);

            if (nearCacheManager.contains(cacheId)) {
                var localCache = nearCacheManager.get(cacheId);
                var items = localCache.snapshot().entrySet().stream()
                        .map(e -> new CacheItem(e.getKey(), e.getValue()))
                        .toList();
                var response = new GetCacheResponse(cacheId, CacheOperationStatus.SUCCESS, items);
                ctx.status(HTTPStatusUtils.getHTTPCode(CacheOperationStatus.SUCCESS));
                ctx.json(response);
            } else {
                handleNewFullNearCacheRequest(ctx, cacheId);
            }
        } catch (Exception e) {
            var errorMsg = "Badly formed request to get cache content for cache ID " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES,
                    CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    /**
     * Get an item from the source cache and also subscribe to updates for the cache.
     *
     * @param ctx     The context.
     * @param cacheId The cache ID.
     * @param key     The key.
     */
    private void handleNewNearCacheRequest(Context ctx, String cacheId, String key) throws InterruptedException, ExecutionException {
        subscribeToCache.accept(cacheId);
        CompletableFuture<GetItemResponse> future = getItemFromSourceCache(ctx, cacheId, key);
        var response = future.get();

        ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
        ctx.json(response);
    }

    /**
     * Handle a request where the local near cache already exists.
     *
     * @param ctx        The context.
     * @param localCache The local near cache.
     * @param key        The key.
     * @param cacheId    The cache ID.
     */
    private void handleExistingLocalCache(Context ctx, NearCache<String> localCache, String key, String cacheId) throws InterruptedException, ExecutionException {
        if (localCache.containsKey(key)) {
            var result = localCache.get(key);
            ctx.status(HTTPStatusUtils.OK);
            ctx.json(new GetItemResponse(cacheId, key, result, CacheOperationStatus.SUCCESS));
        } else {
            returnItemFromSourceCache(ctx, key, cacheId);
        }
    }

    /**
     * Handle a new request for the full cache by fetching from source and subscribing.
     *
     * @param ctx     The context.
     * @param cacheId The cache ID.
     */
    private void handleNewFullNearCacheRequest(Context ctx, String cacheId) throws InterruptedException, ExecutionException {
        subscribeToCache.accept(cacheId);

        var requestId = AbstractRouteHandlers.getRequestId(ctx);
        CompletableFuture<GetCacheResponse> future = new CompletableFuture<>();
        var consumer = HTTPConsumerUtils.getGetAllCacheEntriesResultConsumer(future);
        CompletableFuture.runAsync(() -> publisher.getCacheEntries(requestId, cacheId, consumer));

        var response = future.get();
        ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
        ctx.json(response);
    }

    /**
     * Get an item from the source cache and update the local near cache.
     *
     * @param ctx     The context.
     * @param key     The key.
     * @param cacheId The cache ID.
     */
    private void returnItemFromSourceCache(Context ctx, String key, String cacheId) throws InterruptedException, ExecutionException {
        CompletableFuture<GetItemResponse> future = getItemFromSourceCache(ctx, cacheId, key);
        var response = future.get();

        if (response.operationStatus() == CacheOperationStatus.SUCCESS) {
            nearCacheManager.put(cacheId, key, response.value());
        }
        ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
        ctx.json(response);
    }

    private CompletableFuture<GetItemResponse> getItemFromSourceCache(Context ctx, String cacheId, String key) {
        var requestId = AbstractRouteHandlers.getRequestId(ctx);
        CompletableFuture<GetItemResponse> future = new CompletableFuture<>();
        var consumer = HTTPConsumerUtils.getGetCacheEntryResultConsumer(future);
        CompletableFuture.runAsync(() -> publisher.getCacheEntry(requestId, cacheId, key, consumer));
        return future;
    }
}
