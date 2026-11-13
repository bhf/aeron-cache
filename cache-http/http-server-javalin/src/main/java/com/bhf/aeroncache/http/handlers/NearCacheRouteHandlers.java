package com.bhf.aeroncache.http.handlers;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.http.application.HttpApplication;
import com.bhf.aeroncache.http.responses.CacheDetails;
import com.bhf.aeroncache.http.responses.CacheItem;
import com.bhf.aeroncache.http.responses.GetCacheResponse;
import com.bhf.aeroncache.http.responses.GetItemResponse;
import com.bhf.aeroncache.http.responses.PrimeNearCacheResponse;
import com.bhf.aeroncache.http.responses.RequestErrorResponse;
import com.bhf.aeroncache.http.responses.StopNearCacheResponse;
import com.bhf.aeroncache.models.ErrorMessages;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.near.NearCacheManager;
import com.bhf.aeroncache.services.cache.subscription.CacheSubscriptionRequestPublisher;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.HTTPStatusUtils;
import io.javalin.http.Context;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Handlers for near cache.
 */
@Log4j2
public class NearCacheRouteHandlers {

    private final NearCacheManager<Object> nearCacheManager;
    private final CacheSubscriptionRequestPublisher<ReusableString, ReusableString, ReusableString> subscriptionService;
    private final Supplier<AeronCache> clusterSupplier;

    /**
     * @param nearCacheManager     the local near cache store kept warm by subscription updates.
     * @param subscriptionService  the subscription service providing the shared near cache management API
     *                             (prime / stop / warm listing), backed by {@code nearCacheManager}.
     * @param clusterSupplier      supplies the current cluster connection for prime/stop requests.
     */
    public NearCacheRouteHandlers(NearCacheManager<Object> nearCacheManager,
                                  CacheSubscriptionRequestPublisher<ReusableString, ReusableString, ReusableString> subscriptionService,
                                  Supplier<AeronCache> clusterSupplier) {
        this.nearCacheManager = nearCacheManager;
        this.subscriptionService = subscriptionService;
        this.clusterSupplier = clusterSupplier;
    }

    /**
     * List the caches currently held warm in the near cache, with their local item counts.
     *
     * @param ctx The context.
     */
    public void handleGetNearCachesRequest(Context ctx) {
        log.info("Got request to list warm near caches");
        List<CacheDetails> warmCaches = new ArrayList<>();
        for (var cacheId : subscriptionService.warmNearCaches()) {
            warmCaches.add(new CacheDetails(cacheId, subscriptionService.nearCacheItemCount(cacheId)));
        }
        ctx.status(HTTPStatusUtils.OK);
        ctx.json(warmCaches);
    }

    /**
     * Stop near caching a cache: drop the internal near cache subscription so updates stop flowing and
     * remove the local copy. Idempotent - stopping a cache that is not currently warm succeeds with
     * {@code wasCached=false}.
     *
     * @param ctx The context.
     */
    public void handleStopNearCacheRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            log.info("Got request to stop near caching cacheId {}", cacheId);

            var wasCached = subscriptionService.stopNearCaching(clusterSupplier.get(), cacheId);

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
     * Prime a near cache up front: start the internal near cache subscription (requesting a snapshot) so
     * the local copy is warmed before any read or hydrating subscription arrives. Idempotent - priming a
     * cache that is already warm succeeds with {@code wasAlreadyWarm=true} and does not re-subscribe.
     *
     * <p>The snapshot that populates the local copy arrives asynchronously over the subscription, so a
     * {@code SUCCESS} response means priming was started; the item count becomes visible on the warm
     * caches listing shortly afterwards (the cache itself is listed as warm immediately).
     *
     * @param ctx The context.
     */
    public void handlePrimeNearCacheRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            log.info("Got request to prime near caching cacheId {}", cacheId);

            var primed = subscriptionService.primeNearCache(clusterSupplier.get(), cacheId);

            ctx.status(HTTPStatusUtils.OK);
            ctx.json(new PrimeNearCacheResponse(cacheId, !primed, CacheOperationStatus.SUCCESS));
        } catch (Exception e) {
            var errorMsg = "Badly formed request to prime near caching cacheId " + ctx.pathParam("cacheId");
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

            if (nearCacheManager.contains(cacheId)) {
                var localCache = nearCacheManager.get(cacheId);
                if (localCache.containsKey(key)) {
                    var result = localCache.get(key);
                    ctx.status(HTTPStatusUtils.OK);
                    ctx.json(new GetItemResponse(cacheId, key, (String) result, CacheOperationStatus.SUCCESS));
                    return;
                }
            }

            subscriptionService.primeNearCache(clusterSupplier.get(), cacheId);
            ctx.status(HTTPStatusUtils.getHTTPCode(CacheOperationStatus.UNKNOWN_KEY));
            ctx.json(new GetItemResponse(cacheId, key, "", CacheOperationStatus.UNKNOWN_KEY));
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
                        .map(e -> new CacheItem(e.getKey(), (String) e.getValue()))
                        .toList();
                var response = new GetCacheResponse(cacheId, CacheOperationStatus.SUCCESS, items);
                ctx.status(HTTPStatusUtils.getHTTPCode(CacheOperationStatus.SUCCESS));
                ctx.json(response);
            } else {
                subscriptionService.primeNearCache(clusterSupplier.get(), cacheId);
                ctx.status(HTTPStatusUtils.getHTTPCode(CacheOperationStatus.UNKNOWN_CACHE));
                ctx.json(new GetCacheResponse(cacheId, CacheOperationStatus.UNKNOWN_CACHE, List.of()));
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
}
