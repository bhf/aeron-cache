package com.bhf.aeroncache.http.handlers;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.http.application.HttpApplication;
import com.bhf.aeroncache.http.responses.CacheDetails;
import com.bhf.aeroncache.http.responses.CounterItem;
import com.bhf.aeroncache.http.responses.GetCounterResponse;
import com.bhf.aeroncache.http.responses.GetCountersResponse;
import com.bhf.aeroncache.http.responses.PrimeNearCacheResponse;
import com.bhf.aeroncache.http.responses.RequestErrorResponse;
import com.bhf.aeroncache.http.responses.StopNearCacheResponse;
import com.bhf.aeroncache.models.ErrorMessages;
import com.bhf.aeroncache.models.ReusableLong;
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
 * Handlers for near counter cache reads and their management endpoints.
 */
@Log4j2
public class CountersNearCacheRouteHandlers {

    private final NearCacheManager<Object> nearCacheManager;
    private final CacheSubscriptionRequestPublisher<ReusableString, ReusableString, ReusableLong> subscriptionService;
    private final Supplier<AeronCache> clusterSupplier;

    /**
     * @param nearCacheManager     the local near counters store kept warm by subscription updates.
     * @param subscriptionService  the subscription service providing the shared near cache management API
     *                             (prime / stop / warm listing), backed by {@code nearCacheManager}.
     * @param clusterSupplier      supplies the current cluster connection for prime/stop requests.
     */
    public CountersNearCacheRouteHandlers(NearCacheManager<Object> nearCacheManager,
                                          CacheSubscriptionRequestPublisher<ReusableString, ReusableString, ReusableLong> subscriptionService,
                                          Supplier<AeronCache> clusterSupplier) {
        this.nearCacheManager = nearCacheManager;
        this.subscriptionService = subscriptionService;
        this.clusterSupplier = clusterSupplier;
    }

    /**
     * List the counter caches currently held warm in the near cache, with their local item counts.
     *
     * @param ctx The context.
     */
    public void handleGetNearCachesRequest(Context ctx) {
        log.info("Got request to list warm near counter caches");
        List<CacheDetails> warmCaches = new ArrayList<>();
        for (var cacheId : subscriptionService.warmNearCaches()) {
            warmCaches.add(new CacheDetails(cacheId, subscriptionService.nearCacheItemCount(cacheId)));
        }
        ctx.status(HTTPStatusUtils.OK);
        ctx.json(warmCaches);
    }

    /**
     * Stop near caching a counter cache: drop the internal subscription and remove the local copy.
     *
     * @param ctx The context.
     */
    public void handleStopNearCacheRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            log.info("Got request to stop near caching counters cacheId {}", cacheId);

            var wasCached = subscriptionService.stopNearCaching(clusterSupplier.get(), cacheId);

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
     * Prime a near counter cache up front: start the internal near cache subscription (requesting a
     * snapshot) so the local copy is warmed before any read or hydrating subscription arrives. Idempotent -
     * priming a cache that is already warm succeeds with {@code wasAlreadyWarm=true} and does not
     * re-subscribe.
     *
     * @param ctx The context.
     */
    public void handlePrimeNearCacheRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            log.info("Got request to prime near caching counters cacheId {}", cacheId);

            var primed = subscriptionService.primeNearCache(clusterSupplier.get(), cacheId);

            ctx.status(HTTPStatusUtils.OK);
            ctx.json(new PrimeNearCacheResponse(cacheId, !primed, CacheOperationStatus.SUCCESS));
        } catch (Exception e) {
            var errorMsg = "Badly formed request to prime near caching counters cacheId " + ctx.pathParam("cacheId");
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
                if (localCache.containsKey(key)) {
                    var result = localCache.get(key);
                    ctx.status(HTTPStatusUtils.OK);
                    ctx.json(new GetCounterResponse(cacheId, key, (Long) result, CacheOperationStatus.SUCCESS));
                    return;
                }
            }

            subscriptionService.primeNearCache(clusterSupplier.get(), cacheId);
            ctx.status(HTTPStatusUtils.getHTTPCode(CacheOperationStatus.UNKNOWN_KEY));
            ctx.json(new GetCounterResponse(cacheId, key, 0L, CacheOperationStatus.UNKNOWN_KEY));
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
                        .map(e -> new CounterItem(e.getKey(), (Long) e.getValue()))
                        .toList();
                var response = new GetCountersResponse(cacheId, CacheOperationStatus.SUCCESS, items);
                ctx.status(HTTPStatusUtils.getHTTPCode(CacheOperationStatus.SUCCESS));
                ctx.json(response);
            } else {
                subscriptionService.primeNearCache(clusterSupplier.get(), cacheId);
                ctx.status(HTTPStatusUtils.getHTTPCode(CacheOperationStatus.UNKNOWN_CACHE));
                ctx.json(new GetCountersResponse(cacheId, CacheOperationStatus.UNKNOWN_CACHE, List.of()));
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
}
