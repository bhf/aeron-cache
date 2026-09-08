package com.bhf.aeroncache.http.handlers;

import com.bhf.aeroncache.http.application.HttpApplication;
import com.bhf.aeroncache.http.requests.CreateCacheRequest;
import com.bhf.aeroncache.http.responses.*;
import com.bhf.aeroncache.http.responses.CacheStats;
import com.bhf.aeroncache.models.ErrorMessages;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.impl.ObservingCacheRequestPublisher;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.HTTPStatusUtils;
import io.javalin.http.Context;
import io.opentelemetry.api.trace.Span;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Log4j2
public abstract class AbstractRouteHandlers<V extends Reusable, BV> {

    protected final ObservingCacheRequestPublisher<ReusableString, ReusableString, V, String, String, BV> publisher;

    protected AbstractRouteHandlers(ObservingCacheRequestPublisher<ReusableString, ReusableString, V, String, String, BV> publisher) {
        this.publisher = publisher;
    }

    /**
     * Handle a request to get cache stats.
     *
     * @param ctx The context.
     */
    public void handleGetStatsRequest(Context ctx) {
        log.info("Got request to get cache stats");

        try {
            var requestId = getRequestId(ctx);
            CompletableFuture<CacheStats> future = new CompletableFuture<>();
            Consumer<CacheStatsResult<ReusableString>> consumer = HTTPConsumerUtils.getCacheStatsResultConsumer(future, HttpApplication.statsTracker, HttpApplication.cacheToSize, HttpApplication.allCaches);

            CompletableFuture.runAsync(() -> publisher.getAllCacheStats(requestId, consumer));
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
     * Handle getting details of available caches.
     *
     * @param context The context.
     */
    public void handleGetCachesRequest(Context context) {
        log.info("Got request to get all cache details");
        List<CacheDetails> cacheDetails = new ArrayList<>();
        for (var l : HttpApplication.allCaches) {
            var itemCount = HttpApplication.cacheToSize.getOrDefault(l, 0L);
            cacheDetails.add(new CacheDetails(l, itemCount));
        }
        context.json(cacheDetails);
    }

    /**
     * Handle a request to clear a cache.
     *
     * @param ctx The context.
     */
    public void handleClearCacheRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            log.info("Got clear request on cacheId {}", cacheId);

            var requestId = getRequestId(ctx);
            CompletableFuture<ClearCacheResponse> future = new CompletableFuture<>();
            Consumer<ClearCacheResult<ReusableString>> consumer = HTTPConsumerUtils.getClearCacheResultConsumer(cacheId, future);
            CompletableFuture.runAsync(() -> publisher.clearCache(requestId, cacheId, consumer));

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
     * Handle a request to create a cache.
     *
     * @param ctx The context.
     */
    public void handleCreateCacheRequest(Context ctx) {
        try {
            var request = ctx.bodyAsClass(CreateCacheRequest.class);
            log.info("Got create cache request on cacheId {}", request.cacheId());

            if (!isCreateRequestValid(request, ctx)) {
                return;
            }

            var requestId = getRequestId(ctx);

            CompletableFuture<CreateCacheResponse> future = new CompletableFuture<>();
            Consumer<CreateCacheResult<ReusableString>> consumer = HTTPConsumerUtils.getCreateCacheResultConsumer(future);
            CompletableFuture.runAsync(() -> publisher.sendCreateCache(requestId, request.cacheId(), consumer));

            var response = future.get();

            if (response.operationStatus() == CacheOperationStatus.SUCCESS) {
                onCreateSuccess(response);
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
     * Handle a request to delete a cache.
     *
     * @param ctx The context.
     */
    public void handleDeleteCacheRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            log.info("Got delete cache request for cacheId {}", cacheId);

            var requestId = getRequestId(ctx);
            CompletableFuture<DeleteCacheResponse> future = new CompletableFuture<>();
            Consumer<DeleteCacheResult<ReusableString>> consumer = HTTPConsumerUtils.getDeleteCacheResultConsumer(future);
            CompletableFuture.runAsync(() -> publisher.deleteCache(requestId, cacheId, consumer));

            var response = future.get();

            if (response.operationStatus() == CacheOperationStatus.SUCCESS) {
                onDeleteCacheSuccess(response);
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
    public void handleDeleteItemRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            var key = ctx.pathParam("key");
            log.info("Got delete item request on cacheId {}, key {}",
                    cacheId, key);

            var requestId = getRequestId(ctx);

            CompletableFuture<DeleteItemResponse> future = new CompletableFuture<>();
            Consumer<RemoveCacheEntryResult<ReusableString, ReusableString>> consumer = HTTPConsumerUtils.getRemoveCacheEntryResultConsumer(future);
            CompletableFuture.runAsync(() -> publisher.removeCacheEntry(requestId, cacheId, key, consumer));

            var response = future.get();

            if (response.operationStatus() == CacheOperationStatus.SUCCESS) {
                onDeleteItemSuccess(response);
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
     * Handle a request to cancel a scheduled removal of an item from a cache.
     *
     * @param ctx The context.
     */
    public void handleCancelItemRemovalRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            var key = ctx.pathParam("key");
            log.info("Got cancel item removal request on cacheId {}, key {}",
                    cacheId, key);

            var requestId = getRequestId(ctx);

            CompletableFuture<CancelItemRemovalResponse> future = new CompletableFuture<>();
            Consumer<CancelItemRemovalResult<ReusableString, ReusableString>> consumer = HTTPConsumerUtils.getCancelItemRemovalResultConsumer(future);
            CompletableFuture.runAsync(() -> publisher.cancelItemRemoval(requestId, cacheId, key, consumer));

            var response = future.get();

            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg =
                    "Badly formed request to cancel removal of item with key " + ctx.pathParam("key") + " from cache with Id: " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES,
                    CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    protected boolean isCreateRequestValid(CreateCacheRequest request, Context ctx) {
        return true;
    }

    protected void onCreateSuccess(CreateCacheResponse response) {
    }

    protected void onDeleteCacheSuccess(DeleteCacheResponse response) {
    }

    protected void onDeleteItemSuccess(DeleteItemResponse response) {
    }

    public abstract void handleGetItemRequest(Context ctx);

    public abstract void handleGetCacheRequest(Context ctx);

    public abstract void handlePutItemRequest(Context ctx);

    public abstract void handlePutTimedItemRequest(Context ctx);


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
