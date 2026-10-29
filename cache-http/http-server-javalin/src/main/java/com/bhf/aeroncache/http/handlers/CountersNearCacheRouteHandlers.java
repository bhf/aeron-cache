package com.bhf.aeroncache.http.handlers;

import com.bhf.aeroncache.http.application.HttpApplication;
import com.bhf.aeroncache.http.responses.CacheDetails;
import com.bhf.aeroncache.http.responses.CounterItem;
import com.bhf.aeroncache.http.responses.GetCounterResponse;
import com.bhf.aeroncache.http.responses.GetCountersResponse;
import com.bhf.aeroncache.http.responses.RequestErrorResponse;
import com.bhf.aeroncache.http.responses.StopNearCacheResponse;
import com.bhf.aeroncache.models.ErrorMessages;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.GetAllCacheEntriesResult;
import com.bhf.aeroncache.models.results.GetCacheEntryResult;
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
 * Handlers for the near counter cache read endpoints ({@code GET /api/v1/near/counters/<cacheId>} and
 * {@code GET /api/v1/near/counters/<cacheId>/<key>}) and their management endpoints. The counter
 * equivalent of {@link NearCacheRouteHandlers}: reads are served from a local {@link NearCacheManager}
 * of {@code Long} values kept warm by a cluster subscription, falling back to the source counter cache
 * on a miss. Only wired up when {@code NEAR_CACHE_ENABLED} is set.
 */
@Log4j2
public class CountersNearCacheRouteHandlers {

    private final ObservingCacheRequestPublisher<ReusableString, ReusableString, ReusableLong, String, String, Long> publisher;
    private final NearCacheManager<Long> nearCacheManager;
    private final Consumer<String> subscribeToCache;
    private final Consumer<String> unsubscribeFromCache;

    /**
     * @param publisher            the publisher used to fetch from the source counter cache over the cluster.
     * @param nearCacheManager     the local near counters store kept warm by subscription updates.
     * @param subscribeToCache     subscribes to a counter cache's updates so the local copy is kept in sync.
     * @param unsubscribeFromCache drops the cluster subscription for a counter cache so updates stop flowing.
     */
    public CountersNearCacheRouteHandlers(ObservingCacheRequestPublisher<ReusableString, ReusableString, ReusableLong, String, String, Long> publisher,
                                          NearCacheManager<Long> nearCacheManager,
                                          Consumer<String> subscribeToCache,
                                          Consumer<String> unsubscribeFromCache) {
        this.publisher = publisher;
        this.nearCacheManager = nearCacheManager;
        this.subscribeToCache = subscribeToCache;
        this.unsubscribeFromCache = unsubscribeFromCache;
    }

    /**
     * List the counter caches currently held warm in the near cache, with their local item counts.
     *
     * @param ctx The context.
     */
    public void handleGetNearCachesRequest(Context ctx) {
        log.info("Got request to list warm near counter caches");
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
     * Stop near caching a counter cache: drop the cluster subscription and remove the local copy.
     *
     * @param ctx The context.
     */
    public void handleStopNearCacheRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            log.info("Got request to stop near caching counters cacheId {}", cacheId);

            unsubscribeFromCache.accept(cacheId);
            var wasCached = nearCacheManager.remove(cacheId);

            ctx.status(HTTPStatusUtils.OK);
            ctx.json(new StopNearCacheResponse(cacheId, wasCached, CacheOperationStatus.SUCCESS));
        } catch (Exception e) {
            var errorMsg = "Badly formed request to stop near caching counters cacheId " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES,
                    CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    /**
     * Handle a request to get a counter from a counter cache.
     *
     * @param ctx The context.
     */
    public void handleGetItemRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            var key = ctx.pathParam("key");
            log.info("Got get near counter request on cacheId {}, key {}", cacheId, key);

            if (nearCacheManager.contains(cacheId)) {
                var localCache = nearCacheManager.get(cacheId);
                handleExistingLocalCache(ctx, localCache, key, cacheId);
            } else {
                handleNewNearCacheRequest(ctx, cacheId, key);
            }
        } catch (Exception e) {
            var errorMsg = "Badly formed request to get counter with key " + ctx.pathParam("key") + " from cache with Id: " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES,
                    CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    /**
     * Handle a request to get a whole counter cache snapshot from the local near cache.
     *
     * @param ctx The context.
     */
    public void handleGetCacheRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            log.info("Got get near counter cache content request on cacheId {}", cacheId);

            if (nearCacheManager.contains(cacheId)) {
                var localCache = nearCacheManager.get(cacheId);
                var items = localCache.snapshot().entrySet().stream()
                        .map(e -> new CounterItem(e.getKey(), e.getValue()))
                        .toList();
                var response = new GetCountersResponse(cacheId, CacheOperationStatus.SUCCESS, items);
                ctx.status(HTTPStatusUtils.getHTTPCode(CacheOperationStatus.SUCCESS));
                ctx.json(response);
            } else {
                handleNewFullNearCacheRequest(ctx, cacheId);
            }
        } catch (Exception e) {
            var errorMsg = "Badly formed request to get counter cache content for cache ID " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES,
                    CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    private void handleNewNearCacheRequest(Context ctx, String cacheId, String key) throws InterruptedException, ExecutionException {
        subscribeToCache.accept(cacheId);
        var response = getItemFromSourceCache(ctx, cacheId, key).get();
        ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
        ctx.json(response);
    }

    private void handleExistingLocalCache(Context ctx, NearCache<Long> localCache, String key, String cacheId) throws InterruptedException, ExecutionException {
        if (localCache.containsKey(key)) {
            var result = localCache.get(key);
            ctx.status(HTTPStatusUtils.OK);
            ctx.json(new GetCounterResponse(cacheId, key, result, CacheOperationStatus.SUCCESS));
        } else {
            returnItemFromSourceCache(ctx, key, cacheId);
        }
    }

    private void handleNewFullNearCacheRequest(Context ctx, String cacheId) throws InterruptedException, ExecutionException {
        subscribeToCache.accept(cacheId);

        var requestId = AbstractRouteHandlers.getRequestId(ctx);
        CompletableFuture<GetCountersResponse> future = new CompletableFuture<>();
        Consumer<GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableLong>> consumer = getAllCountersResultConsumer(future);
        CompletableFuture.runAsync(() -> publisher.getCacheEntries(requestId, cacheId, consumer));

        var response = future.get();
        ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
        ctx.json(response);
    }

    private void returnItemFromSourceCache(Context ctx, String key, String cacheId) throws InterruptedException, ExecutionException {
        var response = getItemFromSourceCache(ctx, cacheId, key).get();

        if (response.operationStatus() == CacheOperationStatus.SUCCESS) {
            nearCacheManager.put(cacheId, key, response.value());
        }
        ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
        ctx.json(response);
    }

    private CompletableFuture<GetCounterResponse> getItemFromSourceCache(Context ctx, String cacheId, String key) {
        var requestId = AbstractRouteHandlers.getRequestId(ctx);
        CompletableFuture<GetCounterResponse> future = new CompletableFuture<>();
        Consumer<GetCacheEntryResult<ReusableString, ReusableString, ReusableLong>> consumer = c -> {
            log.info("Get counter response from cluster on cacheId {}, key {}, value {}", c.getCacheId(),
                    c.getEntryKey(), c.getEntryValue());
            var noCache = c.getStatus() == CacheOperationStatus.UNKNOWN_CACHE;
            var response = noCache ?
                    new GetCounterResponse("0", "NA", 0L, c.getStatus()) :
                    new GetCounterResponse(c.getCacheId().value().toString(), c.getEntryKey().value().toString(),
                            c.getEntryValue().value(), c.getStatus());
            future.complete(response);
        };
        CompletableFuture.runAsync(() -> publisher.getCacheEntry(requestId, cacheId, key, consumer));
        return future;
    }

    private Consumer<GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableLong>> getAllCountersResultConsumer(CompletableFuture<GetCountersResponse> future) {
        final List<CounterItem> accumulated = new ArrayList<>();
        return c -> {
            log.info("Get counter cache content response batch from cluster on cacheId {}, endOfBatch {}", c.getCacheId(), c.isEndOfBatch());
            var noCache = c.getStatus() == CacheOperationStatus.UNKNOWN_CACHE;
            if (noCache) {
                future.complete(new GetCountersResponse(c.getCacheId().toString(), CacheOperationStatus.UNKNOWN_CACHE, List.of()));
                return;
            }
            c.getValues().forEach((key, value) -> accumulated.add(new CounterItem(key.value(), value.value())));
            if (c.isEndOfBatch()) {
                future.complete(new GetCountersResponse(c.getCacheId().toString(), c.getStatus(), new ArrayList<>(accumulated)));
            }
        };
    }
}
