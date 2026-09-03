package com.bhf.aeroncache.http.handlers;

import com.bhf.aeroncache.http.application.HttpApplication;
import com.bhf.aeroncache.http.requests.CreateCacheRequest;
import com.bhf.aeroncache.http.requests.PutCounterRequest;
import com.bhf.aeroncache.http.requests.PutTimedCounterRequest;
import com.bhf.aeroncache.http.responses.*;
import com.bhf.aeroncache.models.ErrorMessages;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.impl.ObservingCacheRequestPublisher;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.HTTPStatusUtils;
import io.javalin.http.Context;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.bhf.aeroncache.http.handlers.CacheRouteHandlers.invalidCacheNames;
import static com.bhf.aeroncache.http.handlers.CacheRouteHandlers.specialCharacters;

@Log4j2
public class CountersRouteHandlers extends AbstractRouteHandlers<ReusableLong, Long> {

    public CountersRouteHandlers(ObservingCacheRequestPublisher<ReusableString, ReusableString, ReusableLong, String, String, Long> countersPublisher) {
        super(countersPublisher);
    }

    @Override
    public void handleGetItemRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            var key = ctx.pathParam("key");
            log.info("Got get counter request on cacheId {}, key {}", cacheId, key);

            var requestId = getRequestId(ctx);
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

            var response = future.get();
            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed request to get counter with key " + ctx.pathParam("key") + " from cache with Id: " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    @Override
    public void handleGetCacheRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            log.info("Got get counter cache content request on cacheId {}", cacheId);

            var requestId = getRequestId(ctx);
            CompletableFuture<GetCountersResponse> future = new CompletableFuture<>();
            Consumer<GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableLong>> consumer = c -> {
                log.info("Get counter cache content response from cluster on cacheId {}", c.getCacheId());
                var noCache = c.getStatus() == CacheOperationStatus.UNKNOWN_CACHE;
                if (noCache) {
                    future.complete(new GetCountersResponse(c.getCacheId().toString(), CacheOperationStatus.UNKNOWN_CACHE, List.of()));
                } else {
                    List<CounterItem> items = new ArrayList<>();
                    c.getValues().forEach((key, value) -> items.add(new CounterItem(key.value(), value.value())));
                    future.complete(new GetCountersResponse(c.getCacheId().toString(), c.getStatus(), items));
                }
            };
            CompletableFuture.runAsync(() -> publisher.getCacheEntries(requestId, cacheId, consumer));

            var response = future.get();
            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed request to get counter cache content for cache ID " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    @Override
    public void handlePutItemRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            var request = ctx.bodyAsClass(PutCounterRequest.class);
            log.info("Got put counter request on cacheId {}, key {}, value {}", cacheId, request.key(), request.value());

            var requestId = getRequestId(ctx);
            CompletableFuture<PutItemResponse> future = new CompletableFuture<>();
            var consumer = HTTPConsumerUtils.getAddCacheEntryResultConsumer(request.key(), future);
            long ttl = 0;
            CompletableFuture.runAsync(() -> publisher.addCacheEntry(requestId, cacheId,
                    request.key(), request.value(), ttl, consumer));

            var response = future.get();
            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed request to put counter from request: " + ctx.body();
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    @Override
    public void handlePutTimedItemRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            var request = ctx.bodyAsClass(PutTimedCounterRequest.class);
            log.info("Got put timed counter request on cacheId {}, key {}, value {}, ttl {}",
                    cacheId, request.key(), request.value(), request.ttl());

            var requestId = getRequestId(ctx);
            CompletableFuture<PutItemResponse> future = new CompletableFuture<>();
            var consumer = HTTPConsumerUtils.getAddCacheEntryResultConsumer(request.key(), future);
            long ttl = request.ttl();
            CompletableFuture.runAsync(() -> publisher.addCacheEntry(requestId, cacheId,
                    request.key(), request.value(), ttl, consumer));

            var response = future.get();
            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed request to put timed counter from request: " + ctx.body();
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    @Override
    protected boolean isCreateRequestValid(CreateCacheRequest request, Context ctx) {
        if (specialCharacters.matcher(request.cacheId()).find()) {
            var errorMsg = "Cache ID shouldn't contain special characters";
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CACHE_ID_NO_SPECIAL_CHARACTERS,
                    CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
            return false;
        }

        if (invalidCacheNames.contains(request.cacheId())) {
            var errorMsg = "Cache ID shouldn't be a reserved name";
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CACHE_ID_NO_RESERVED_NAMES,
                    CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
            return false;
        }

        return true;
    }
}
