package com.bhf.aeroncache.http.handlers;

import com.bhf.aeroncache.http.application.HttpApplication;
import com.bhf.aeroncache.http.requests.CreateCacheRequest;
import com.bhf.aeroncache.http.requests.PutItemRequest;
import com.bhf.aeroncache.http.requests.PutTimedItemRequest;
import com.bhf.aeroncache.http.responses.*;
import com.bhf.aeroncache.http.responses.CacheStats;
import com.bhf.aeroncache.models.ErrorMessages;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.HTTPStatusUtils;
import io.javalin.http.Context;
import io.opentelemetry.api.trace.Span;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Log4j2
public class CacheRouteHandlers {

    static final Pattern specialCharacters = Pattern.compile("[$&+,:;=\\\\?@#|/'<>.^*()%!]");
    static final Set<String> invalidCacheNames = Set.of("bulkops", "timed");

    /**
     * Handle a request to get cache stats.
     *
     * @param ctx The context.
     */
    public static void handleGetStatsRequest(Context ctx) {
        log.info("Got request to get cache stats");

        try {
            var requestId = getRequestId(ctx);
            CompletableFuture<CacheStats> future = new CompletableFuture<>();
            Consumer<CacheStatsResult<ReusableString>> consumer = HTTPConsumerUtils.getCacheStatsResultConsumer(future, HttpApplication.statsTracker, HttpApplication.cacheToSize, HttpApplication.allCaches);

            CompletableFuture.runAsync(() -> HttpApplication.getCachePublisher().getAllCacheStats(requestId, consumer));
            var response = future.get();

            ctx.status(HTTPStatusUtils.OK);
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed request to get cache stats";
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    /**
     * Handle getting details of available caches. Currently only
     * implemented in memory on the HTTP side. Information returned is
     * populated from any previous call to get the stats from the
     * Aeron Cache instance.
     *
     * @param context The context.
     */
    public static void handleGetCachesRequest(Context context) {
        log.info("Got request to get all cache details");
        List<CacheDetails> cacheDetails = new ArrayList<>();
        for (var l : HttpApplication.allCaches) {
            var itemCount = HttpApplication.cacheToSize.getOrDefault(l, 0L);
            cacheDetails.add(new CacheDetails(l, itemCount));
        }
        context.json(cacheDetails);
    }

    /**
     * Handle a request to delete a cache.
     *
     * @param ctx The context.
     */
    public static void handleDeleteCacheRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            log.info("Got delete cache request for cacheId {}", cacheId);

            var requestId = getRequestId(ctx);
            CompletableFuture<DeleteCacheResponse> future = new CompletableFuture<>();
            Consumer<DeleteCacheResult<ReusableString>> consumer = HTTPConsumerUtils.getDeleteCacheResultConsumer(future);
            CompletableFuture.runAsync(() -> HttpApplication.getCachePublisher().deleteCache(requestId, cacheId, consumer));

            var response = future.get();

            if (response.operationStatus() == CacheOperationStatus.SUCCESS) {
                HttpApplication.allCaches.remove(response.cacheId());
                HttpApplication.statsTracker.getTotalCaches().decrementAndGet();
            }

            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed request to delete cache with Id: " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES,
                    CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    /**
     * Handle a request to delete an item from a cache.
     *
     * @param ctx The context.
     */
    public static void handleDeleteItemRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            var key = ctx.pathParam("key");
            log.info("Got delete item request on cacheId {}, key {}",
                    cacheId, key);

            var requestId = getRequestId(ctx);

            CompletableFuture<DeleteItemResponse> future = new CompletableFuture<>();
            Consumer<RemoveCacheEntryResult<ReusableString, ReusableString>> consumer = HTTPConsumerUtils.getRemoveCacheEntryResultConsumer(future);
            CompletableFuture.runAsync(() -> HttpApplication.getCachePublisher().removeCacheEntry(requestId, cacheId, key, consumer));

            var response = future.get();

            if (response.operationStatus() == CacheOperationStatus.SUCCESS) {
                HttpApplication.statsTracker.getTotalItems().decrementAndGet();
            }

            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg =
                    "Badly formed request to delete item with key " + ctx.pathParam("key") + " from cache with Id: " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES,
                    CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    /**
     * Handle a request to clear a cache.
     *
     * @param ctx The context.
     */
    public static void handleClearCacheRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            log.info("Got clear request on cacheId {}", cacheId);

            var requestId = getRequestId(ctx);
            CompletableFuture<ClearCacheResponse> future = new CompletableFuture<>();
            Consumer<ClearCacheResult<ReusableString>> consumer = HTTPConsumerUtils.getClearCacheResultConsumer(cacheId, future);
            CompletableFuture.runAsync(() -> HttpApplication.getCachePublisher().clearCache(requestId, cacheId, consumer));

            var response = future.get();
            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed request to clear cache with ID " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    /**
     * Handle a request to get an item from a cache.
     *
     * @param ctx The context.
     */
    public static void handleGetItemRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            var key = ctx.pathParam("key");
            log.info("Got get item request on cacheId {}, key {}",
                    cacheId, key);

            var requestId = getRequestId(ctx);
            CompletableFuture<GetItemResponse> future = new CompletableFuture<>();
            Consumer<GetCacheEntryResult<ReusableString,ReusableString,ReusableString>> consumer = HTTPConsumerUtils.getGetCacheEntryResultConsumer(future);
            CompletableFuture.runAsync(() -> HttpApplication.getCachePublisher().getCacheEntry(requestId, cacheId, key, consumer));

            var response = future.get();
            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg =
                    "Badly formed request to get item with key " + ctx.pathParam("key") + " from cache with Id: " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES,
                    CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    /**
     * Handle a request to add an item to a cache.
     *
     * @param ctx The context.
     */
    public static void handlePutItemRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            var request = ctx.bodyAsClass(PutItemRequest.class);
            log.info("Got put item request on cacheId {}, key {}, value {}",
                    cacheId, request.key(), request.value());

            var requestId = getRequestId(ctx);
            CompletableFuture<PutItemResponse> future = new CompletableFuture<>();
            Consumer<AddCacheEntryResult<ReusableString, ReusableString>> consumer = HTTPConsumerUtils.getAddCacheEntryResultConsumer(request.key(), future);
            long ttl = 0;
            CompletableFuture.runAsync(() -> HttpApplication.getCachePublisher().addCacheEntry(requestId, cacheId,
                    request.key(), request.value(), ttl, consumer));

            var response = future.get();

            if (response.operationStatus() == CacheOperationStatus.SUCCESS) {
                HttpApplication.statsTracker.getTotalItems().incrementAndGet();
            }

            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed request to put item from request: " + ctx.body();
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    /**
     * Handle a request to add a timed item to a cache.
     *
     * @param ctx The context.
     */
    public static void handlePutTimedItemRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            var request = ctx.bodyAsClass(PutTimedItemRequest.class);
            log.info("Got put item request on cacheId {}, key {}, value {}, ttl {}",
                    cacheId, request.key(), request.value(), request.ttl());

            var requestId = getRequestId(ctx);
            CompletableFuture<PutItemResponse> future = new CompletableFuture<>();
            Consumer<AddCacheEntryResult<ReusableString, ReusableString>> consumer = HTTPConsumerUtils.getAddCacheEntryResultConsumer(request.key(), future);
            long ttl = request.ttl();
            CompletableFuture.runAsync(() -> HttpApplication.getCachePublisher().addCacheEntry(requestId, cacheId,
                    request.key(), request.value(), ttl, consumer));

            var response = future.get();

            if (response.operationStatus() == CacheOperationStatus.SUCCESS) {
                HttpApplication.statsTracker.getTotalItems().incrementAndGet();
            }

            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed request to put timed item from request: " + ctx.body();
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    /**
     * Handle a request to create a cache.
     *
     * @param ctx The context.
     */
    public static void handleCreateCacheRequest(Context ctx) {
        try {
            var request = ctx.bodyAsClass(CreateCacheRequest.class);
            log.info("Got create cache request on cacheId {}", request.cacheId());

            if (specialCharacters.matcher(request.cacheId()).find()) {
                var errorMsg = "Cache ID shouldn't contain special characters";
                var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CACHE_ID_NO_SPECIAL_CHARACTERS,
                        CacheOperationStatus.ERROR);
                ctx.status(HTTPStatusUtils.BAD_REQUEST);
                ctx.json(badRequest);
                return;
            }

            if(invalidCacheNames.contains(request.cacheId())){
                var errorMsg = "Cache ID shouldn't be a reserved name";
                var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CACHE_ID_NO_RESERVED_NAMES,
                        CacheOperationStatus.ERROR);
                ctx.status(HTTPStatusUtils.BAD_REQUEST);
                ctx.json(badRequest);
                return;
            }

            var requestId = getRequestId(ctx);

            CompletableFuture<CreateCacheResponse> future = new CompletableFuture<>();
            Consumer<CreateCacheResult<ReusableString>> consumer = HTTPConsumerUtils.getCreateCacheResultConsumer(future);
            CompletableFuture.runAsync(() -> HttpApplication.getCachePublisher().sendCreateCache(requestId, request.cacheId(), consumer));

            var response = future.get();

            if (response.operationStatus() == CacheOperationStatus.SUCCESS) {
                HttpApplication.allCaches.add(response.cacheId());
                HttpApplication.statsTracker.getTotalCaches().incrementAndGet();
            }

            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed request to create cache from request: " + ctx.body();
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    /**
     * Handle a request to get a whole cache.
     *
     * @param ctx The context.
     */
    public static void handleGetCacheRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            log.info("Got get cache content request on cacheId {}", cacheId);

            var requestId = getRequestId(ctx);
            CompletableFuture<GetCacheResponse> future = new CompletableFuture<>();
            var consumer = HTTPConsumerUtils.getGetAllCacheEntriesResultConsumer(future);
            CompletableFuture.runAsync(() -> HttpApplication.getCachePublisher().getCacheEntries(requestId, cacheId, consumer));

            var response = future.get();
            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed request to get cache content for cache ID " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    /**
     * Build the requestId based on whether tracing is enabled.
     *
     * @param ctx The Context.
     * @return A requestId
     */
    public static String getRequestId(Context ctx) {
        return HttpApplication.tracingServiceName != null ? getTraceBasedRequestId(ctx) : UUID.randomUUID().toString();
    }

    /**
     * Use the current span and trace Ids to build a requestId to
     * be sent to the Aeron Cache cluster.
     *
     * @param ctx The Context.
     * @return A requestId
     */
    static String getTraceBasedRequestId(Context ctx) {
        var currentSpanId = Span.current().getSpanContext().getSpanId();
        var currentTraceId = Span.current().getSpanContext().getTraceId();
        log.info("Creating requestId using traceID {} and spanID {}", currentTraceId, currentSpanId);
        return currentTraceId + "@" + currentSpanId;
    }
}
