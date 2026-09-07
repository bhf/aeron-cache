package com.bhf.aeroncache.http.handlers;

import com.bhf.aeroncache.http.application.CacheStatsTracker;
import com.bhf.aeroncache.http.responses.*;
import com.bhf.aeroncache.http.responses.CacheStats;
import com.bhf.aeroncache.models.bulk.responses.BulkCacheOpsResponse;
import com.bhf.aeroncache.models.bulk.responses.CacheOperationResponse;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableString;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Log4j2
public class HTTPConsumerUtils {

    @NotNull
    public static Consumer<CacheStatsResult<ReusableString>> getCacheStatsResultConsumer(
            CompletableFuture<CacheStats> future,
            CacheStatsTracker statsTracker,
            Map<String, Long> cacheToSize,
            Set<String> allCaches) {
        return c -> {
            log.info("Got cache stats, requestId {}", c.getRequestId());
            int totalOps = statsTracker.getTotalOpsCount().get();
            int totalCaches = 0;
            int totalItems = 0;

            var stats = c.getStats();
            Set<String> latestCaches = new HashSet<>();
            for (var x : stats) {
                totalCaches++;
                totalItems += x.size;
                var cacheId = x.getCacheId().value().toString();
                latestCaches.add(cacheId);
                cacheToSize.put(cacheId, x.size);
            }

            allCaches.retainAll(latestCaches);
            allCaches.addAll(latestCaches);

            var statsTrackerStats = statsTracker.getCacheStats();
            var response = new CacheStats(totalOps, totalCaches, totalItems, statsTrackerStats.errorCount());
            future.complete(response);
        };
    }

    @NotNull
    public static Consumer<DeleteCacheResult<ReusableString>> getDeleteCacheResultConsumer(CompletableFuture<DeleteCacheResponse> future) {
        return c -> {
            var deletedCacheId = c.getCacheId();
            log.info("Got delete cache response from cluster on cacheId {}", deletedCacheId);
            var response = new DeleteCacheResponse(deletedCacheId.value().toString(), c.getStatus());
            future.complete(response);
        };
    }

    @NotNull
    public static Consumer<RemoveCacheEntryResult<ReusableString, ReusableString>> getRemoveCacheEntryResultConsumer(CompletableFuture<DeleteItemResponse> future) {
        return c -> {
            log.info("Got delete item response from cluster on cacheId {}, key {}", c.getCacheId(), c.getKey());
            var response = new DeleteItemResponse(c.getCacheId().value().toString(), c.getKey().value().toString(), c.getStatus());
            future.complete(response);
        };
    }

    @NotNull
    public static Consumer<ClearCacheResult<ReusableString>> getClearCacheResultConsumer(String cacheId, CompletableFuture<ClearCacheResponse> future) {
        return c -> {
            log.info("Got clear cache response from cluster on cacheId {}", c.getCacheId());
            var response = new ClearCacheResponse(cacheId, c.getStatus());
            future.complete(response);
        };
    }

    @NotNull
    public static Consumer<GetCacheEntryResult<ReusableString,ReusableString,ReusableString>> getGetCacheEntryResultConsumer(CompletableFuture<GetItemResponse> future) {
        return c -> {
            log.info("Get item response from cluster on cacheId {}, key {}, value {}", c.getCacheId(),
                    c.getEntryKey(), c.getEntryValue());
            var noCache = c.getStatus() == CacheOperationStatus.UNKNOWN_CACHE;
            var response = noCache ?
                    new GetItemResponse("0", "NA", "NA", c.getStatus()) :
                    new GetItemResponse(c.getCacheId().value().toString(), c.getEntryKey().value().toString(),
                            c.getEntryValue().value().toString(), c.getStatus());
            future.complete(response);
        };
    }

    @NotNull
    public static Consumer<AddCacheEntryResult<ReusableString, ReusableString>> getAddCacheEntryResultConsumer(String key, CompletableFuture<PutItemResponse> future) {
        return c -> {
            var cacheId = c.getCacheId();
            log.info("Got put item response from cluster on cacheId {}", cacheId);
            var response = new PutItemResponse(cacheId.value().toString(), key, c.getStatus());
            future.complete(response);
        };
    }

    @NotNull
    public static Consumer<PatchValueResult<ReusableString, ReusableString, ReusableString>> getPatchValueResultConsumer(String key, CompletableFuture<PatchItemResponse> future) {
        return c -> {
            var cacheId = c.getCacheId();
            log.info("Got patch item response from cluster on cacheId {}, key {}", cacheId, key);
            var response = new PatchItemResponse(cacheId.value().toString(), key, c.getStatus());
            future.complete(response);
        };
    }

    @NotNull
    public static Consumer<BulkCacheOpsResult<ReusableString, ReusableString, ReusableString>> getBulkCacheOpsResultConsumer(CompletableFuture<BulkCacheOpsResponse> future, String requestId) {
        return c -> {
            List<CacheOperationResponse> operationResponses = new ArrayList<>();

            List<CacheOperationResultDetails<ReusableString, ReusableString, ReusableString>> ops = c.getOperations();
            for(var o : ops){
                var opRequestId = o.getRequestId();
                var cacheId = o.getCacheId();
                var value = o.getValue();
                var key = o.getKey();
                var status = o.getOperationStatus();
                operationResponses.add(new CacheOperationResponse(opRequestId, status, cacheId.value(), key.value(), value.value()));
            }

            BulkCacheOpsResponse response = new BulkCacheOpsResponse(requestId, operationResponses);
            future.complete(response);
        };
    }

    @NotNull
    public static Consumer<CreateCacheResult<ReusableString>> getCreateCacheResultConsumer(CompletableFuture<CreateCacheResponse> future) {
        return c -> {
            var cacheId = c.getCacheId();
            log.info("Got create cache response from cluster on cacheId {}", cacheId);
            var response = new CreateCacheResponse(cacheId.value().toString(), c.getStatus());
            future.complete(response);
        };
    }

    @NotNull
    public static Consumer<GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString>> getGetAllCacheEntriesResultConsumer(CompletableFuture<GetCacheResponse> future) {
        return c -> {
            log.info("Get cache content response from cluster on cacheId {}", c.getCacheId());
            var noCache = c.getStatus() == CacheOperationStatus.UNKNOWN_CACHE;
            var response = noCache ?
                    new GetCacheResponse(c.getCacheId().toString(), CacheOperationStatus.UNKNOWN_CACHE, List.of()) :
                    new GetCacheResponse(c.getCacheId().toString(), c.getStatus(), buildItemsList(c));
            future.complete(response);
        };
    }

    public static List<CacheItem> buildItemsList(GetAllCacheEntriesResult<ReusableString, ReusableString,
            ReusableString> c) {
        List<CacheItem> res = new ArrayList<>();
        c.getValues().forEach((key, value) -> {
            res.add(new CacheItem(key.value(), value.value()));
        });
        return res;
    }
}
