package com.bhf.aeroncache.codecs.response;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.DirectBuffer;

/**
 * Decode responses from Aeron Cache.
 */
public class ReusableStringCacheResponseDecoder implements CacheResponseDecoder<ReusableString, ReusableString, ReusableString> {

    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final CacheCreatedDecoder cacheCreatedDecoder = new CacheCreatedDecoder();
    private final AllCacheEntriesResultDecoder allCacheEntriesResultDecoder = new AllCacheEntriesResultDecoder();
    private final CacheEntryResultDecoder getCacheEntryDecoder = new CacheEntryResultDecoder();
    private final CacheEntryCreatedDecoder addCacheEntryDecoder = new CacheEntryCreatedDecoder();
    private final CacheEntryRemovedDecoder cacheEntryRemovedDecoder = new CacheEntryRemovedDecoder();
    private final CacheClearedDecoder cacheClearedDecoder = new CacheClearedDecoder();
    private final CacheDeletedDecoder cacheDeletedDecoder = new CacheDeletedDecoder();
    private final AllCacheStatsResultDecoder allCacheStatsResultDecoder = new AllCacheStatsResultDecoder();
    private final CacheSubscriptionResponseDecoder cacheSubscriptionResponseDecoder = new CacheSubscriptionResponseDecoder();
    private final CacheUnsubscribeResponseDecoder cacheUnsubscribeResponseDecoder = new CacheUnsubscribeResponseDecoder();
    private final CacheEntryUpdateDecoder cacheEntryUpdateDecoder = new CacheEntryUpdateDecoder();
    private final BulkOperationResponseDecoder bulkOperationResponseDecoder = new BulkOperationResponseDecoder();

    @Override
    public void decodeCacheCreated(DirectBuffer buffer, int offset, CreateCacheResult<ReusableString> createCacheResult) {
        cacheCreatedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var status = getOperationStatus(cacheCreatedDecoder.status());
        var cacheId = cacheCreatedDecoder.cacheId();
        var requestId = cacheCreatedDecoder.requestId();

        createCacheResult.clear();
        createCacheResult.getCacheId().copyFrom(cacheId);
        createCacheResult.setRequestId(requestId);
        createCacheResult.setStatus(status);
    }

    private CacheOperationStatus getOperationStatus(OperationStatus status) {
        switch(status){
            case OperationStatus.CACHE_EXISTS -> {
                return CacheOperationStatus.CACHE_EXISTS;
            }
            case OperationStatus.DUPLICATE_SUBSCRIPTION -> {
                return CacheOperationStatus.DUPLICATE_SUBSCRIPTION;
            }
            case OperationStatus.UNKNOWN_CACHE -> {
                return CacheOperationStatus.UNKNOWN_CACHE;
            }
            case OperationStatus.UNKNOWN_KEY -> {
                return CacheOperationStatus.UNKNOWN_KEY;
            }
            case OperationStatus.UNKNOWN_SUBSCRIPTION -> {
                return CacheOperationStatus.UNKNOWN_SUBSCRIPTION;
            }
            case OperationStatus.SUCCESS -> {
                return CacheOperationStatus.SUCCESS;
            }
            case OperationStatus.NULL_VAL -> {
                return CacheOperationStatus.NULL_VAL;
            }
            case OperationStatus.NONE -> {
                return CacheOperationStatus.NONE;
            }
            case OperationStatus.ERROR -> {
                return CacheOperationStatus.ERROR;
            }
        }

        return CacheOperationStatus.NONE;
    }

    @Override
    public void decodeAllCacheEntriesResult(DirectBuffer buffer, int offset, GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString> getCacheEntriesResult) {
        allCacheEntriesResultDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var status = getOperationStatus(allCacheEntriesResultDecoder.status());
        var eob = allCacheEntriesResultDecoder.endOfBatch();

        getCacheEntriesResult.clear();
        getCacheEntriesResult.setStatus(status);

        // process group of key-value from the decoder directly into the flyweight

        for (AllCacheEntriesResultDecoder.ItemsDecoder item : allCacheEntriesResultDecoder.items()) {
            var key = new ReusableString();
            var value = new ReusableString();
            key.copyFrom(item.key());
            value.copyFrom(item.value());
            getCacheEntriesResult.getValues().put(key, value);
        }

        var requestId = allCacheEntriesResultDecoder.requestId();
        getCacheEntriesResult.setRequestId(requestId);

        var cacheID = allCacheEntriesResultDecoder.cacheId();
        getCacheEntriesResult.getCacheId().copyFrom(cacheID);
    }

    @Override
    public void decodeGetCacheEntryResult(DirectBuffer buffer, int offset, GetCacheEntryResult<ReusableString, ReusableString, ReusableString> getCacheEntryResult) {
        getCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var status = getOperationStatus(getCacheEntryDecoder.status());
        var cacheID = getCacheEntryDecoder.cacheId();
        var key = getCacheEntryDecoder.key();
        var value = getCacheEntryDecoder.value();
        var requestId = getCacheEntryDecoder.requestId();

        getCacheEntryResult.clear();
        getCacheEntryResult.getCacheId().copyFrom(cacheID);
        getCacheEntryResult.getEntryKey().copyFrom(key);
        getCacheEntryResult.getEntryValue().copyFrom(value);
        getCacheEntryResult.setRequestId(requestId);
        getCacheEntryResult.setStatus(status);
    }

    @Override
    public void decodeAddCacheEntryResult(DirectBuffer buffer, int offset, AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult) {
        addCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var status = getOperationStatus(addCacheEntryDecoder.status());
        var cacheId = addCacheEntryDecoder.cacheId();
        var key = addCacheEntryDecoder.key();
        var requestId = addCacheEntryDecoder.requestId();

        addCacheEntryResult.clear();
        addCacheEntryResult.setEntryAdded(true);
        addCacheEntryResult.getEntryKey().copyFrom(key);
        addCacheEntryResult.getCacheId().copyFrom(cacheId);
        addCacheEntryResult.setRequestId(requestId);
        addCacheEntryResult.setStatus(status);
    }

    @Override
    public void decodeCacheEntryRemoved(DirectBuffer buffer, int offset, RemoveCacheEntryResult<ReusableString, ReusableString> removeCacheEntryResult) {
        cacheEntryRemovedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var status = getOperationStatus(cacheEntryRemovedDecoder.status());
        var cacheId = cacheEntryRemovedDecoder.cacheId();
        var key = cacheEntryRemovedDecoder.key();
        var requestId = cacheEntryRemovedDecoder.requestId();

        removeCacheEntryResult.clear();
        removeCacheEntryResult.getKey().copyFrom(key);
        removeCacheEntryResult.getCacheId().copyFrom(cacheId);
        removeCacheEntryResult.setRequestId(requestId);
        removeCacheEntryResult.setStatus(status);
    }

    @Override
    public void decodeCacheCleared(DirectBuffer buffer, int offset, ClearCacheResult<ReusableString> clearCacheResult) {
        cacheClearedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var status = getOperationStatus(cacheClearedDecoder.status());
        var cacheId = cacheClearedDecoder.cacheId();
        var requestId = cacheClearedDecoder.requestId();

        clearCacheResult.clear();
        clearCacheResult.getCacheId().copyFrom(cacheId);
        clearCacheResult.setRequestId(requestId);
        clearCacheResult.setStatus(status);
    }

    @Override
    public void decodeCacheDeleted(DirectBuffer buffer, int offset, DeleteCacheResult<ReusableString> deleteCacheResult) {
        cacheDeletedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var status = getOperationStatus(cacheDeletedDecoder.status());
        var cacheId = cacheDeletedDecoder.cacheId();
        var requestId = cacheDeletedDecoder.requestId();

        deleteCacheResult.clear();
        deleteCacheResult.getCacheId().copyFrom(cacheId);
        deleteCacheResult.setRequestId(requestId);
        deleteCacheResult.setStatus(status);
    }

    @Override
    public void decodeAllCacheStatsResult(DirectBuffer buffer, int offset, CacheStatsResult<ReusableString> cacheStatsResult) {
        allCacheStatsResultDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var status = getOperationStatus(allCacheStatsResultDecoder.status());
        cacheStatsResult.clear();
        cacheStatsResult.setOperationStatus(status);

        for (AllCacheStatsResultDecoder.StatsDecoder item : allCacheStatsResultDecoder.stats()) {
            var added = item.added();
            var removed = item.removed();
            var cleared = item.cleared();
            var size = item.size();
            var cacheId = item.cacheId();
            var id = new ReusableString();
            id.copyFrom(cacheId);
            var stats = new CacheStats<>(id);
            stats.addedCount = added;
            stats.removedCount = removed;
            stats.clearedCount = cleared;
            stats.size = size;
            cacheStatsResult.getStats().add(stats);
        }

        var requestId = allCacheStatsResultDecoder.requestId();
        cacheStatsResult.setRequestId(requestId);
    }

    @Override
    public void decodeCacheSubscribeResult(DirectBuffer buffer, int offset, CacheSubscriptionResult<ReusableString,ReusableString,ReusableString> cacheSubscriptionResult) {

        cacheSubscriptionResult.clear();

        if(cacheSubscriptionResult.getEntries()!=null) {
            cacheSubscriptionResult.getEntries().clear();
        }

        cacheSubscriptionResponseDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var status = getOperationStatus(cacheSubscriptionResponseDecoder.status());

        // process group of key-value from the decoder directly into the flyweight

        for (var item : cacheSubscriptionResponseDecoder.items()) {
            var key = new ReusableString();
            var value = new ReusableString();
            key.copyFrom(item.key());
            value.copyFrom(item.value());
            item.cacheId();
            cacheSubscriptionResult.getEntries().put(key, value);
        }

        var cacheId = cacheSubscriptionResponseDecoder.cacheId();
        var requestId = cacheSubscriptionResponseDecoder.requestId();

        cacheSubscriptionResult.getCacheId().copyFrom(cacheId);
        cacheSubscriptionResult.setStatus(status);
        cacheSubscriptionResult.setRequestId(requestId);
    }

    @Override
    public void decodeCacheUnsubscribeResult(DirectBuffer buffer, int offset, CacheUnsubscribeResult<ReusableString> cacheUnsubscribeResult) {
        cacheUnsubscribeResponseDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var status = getOperationStatus(cacheUnsubscribeResponseDecoder.status());
        var cacheId = cacheUnsubscribeResponseDecoder.cacheId();
        var requestId = cacheUnsubscribeResponseDecoder.requestId();

        cacheUnsubscribeResult.clear();
        cacheUnsubscribeResult.getCacheId().copyFrom(cacheId);
        cacheUnsubscribeResult.setStatus(status);
        cacheUnsubscribeResult.setRequestId(requestId);
    }

    @Override
    public void decodeCacheEntryUpdated(DirectBuffer buffer, int offset, CacheEntryUpdateResult<ReusableString, ReusableString, ReusableString> cacheEntryUpdateResult) {
        cacheEntryUpdateDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var cacheId = cacheEntryUpdateDecoder.cacheId();
        var key = cacheEntryUpdateDecoder.key();
        var value = cacheEntryUpdateDecoder.value();
        var requestId = cacheEntryUpdateDecoder.requestId();

        cacheEntryUpdateResult.clear();
        cacheEntryUpdateResult.getCacheId().copyFrom(cacheId);
        cacheEntryUpdateResult.setRequestId(requestId);
        cacheEntryUpdateResult.getKey().copyFrom(key);
        cacheEntryUpdateResult.getValue().copyFrom(value);
    }

    @Override
    public void decodeBulkCacheOpsResult(DirectBuffer buffer, int offset, BulkCacheOpsResult<ReusableString, ReusableString, ReusableString> bulkCacheOpsResult) {
        bulkCacheOpsResult.clear();
        bulkOperationResponseDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var itemsDecoder = bulkOperationResponseDecoder.items();

        for(var op: itemsDecoder) {
            var opType = op.operationStatus();
            var requestId = op.requestId();

            var cacheId = new ReusableString();
            cacheId.copyFrom(op.cacheId());

            var key = new ReusableString();
            key.copyFrom(op.key());

            var value = new ReusableString();
            value.copyFrom(op.value());

            bulkCacheOpsResult.addOperationResult(
                    CacheOperationStatus.valueOf(opType.toString()),
                    requestId, cacheId, key, value);
        }

        var requestId = bulkOperationResponseDecoder.requestId();
        bulkCacheOpsResult.setRequestId(requestId);
    }
}
