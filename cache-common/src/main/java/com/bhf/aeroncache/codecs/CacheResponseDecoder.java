package com.bhf.aeroncache.codecs;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.DirectBuffer;

/**
 * Decode responses from Aeron Cache.
 */
public class CacheResponseDecoder {

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

    public void decodeCacheCreated(CreateCacheResult<ReusableString> createCacheResult, DirectBuffer buffer, int offset) {
        cacheCreatedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var status = cacheCreatedDecoder.status();
        var cacheId = cacheCreatedDecoder.cacheId();
        var requestId = cacheCreatedDecoder.requestId();

        createCacheResult.clear();
        createCacheResult.getCacheId().copyFrom(cacheId);
        createCacheResult.setRequestId(requestId);
        createCacheResult.setStatus(status);
    }

    public void decodeAllCacheEntriesResult(GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString> getCacheEntriesResult, DirectBuffer buffer, int offset) {
        allCacheEntriesResultDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var status = allCacheEntriesResultDecoder.status();
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

    public void decodeGetCacheEntryResult(GetCacheEntryResult<ReusableString, ReusableString, ReusableString> getCacheEntryResult, DirectBuffer buffer, int offset) {
        getCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var status = getCacheEntryDecoder.status();
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

    public void decodeAddCacheEntryResult(AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult, DirectBuffer buffer, int offset) {
        addCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var status = addCacheEntryDecoder.status();
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

    public void decodeCacheEntryRemoved(RemoveCacheEntryResult<ReusableString, ReusableString> removeCacheEntryResult, DirectBuffer buffer, int offset) {
        cacheEntryRemovedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var status = cacheEntryRemovedDecoder.status();
        var cacheId = cacheEntryRemovedDecoder.cacheId();
        var key = cacheEntryRemovedDecoder.key();
        var requestId = cacheEntryRemovedDecoder.requestId();

        removeCacheEntryResult.clear();
        removeCacheEntryResult.getKey().copyFrom(key);
        removeCacheEntryResult.getCacheId().copyFrom(cacheId);
        removeCacheEntryResult.setRequestId(requestId);
        removeCacheEntryResult.setStatus(status);
    }

    public void decodeCacheCleared(ClearCacheResult<ReusableString> clearCacheResult, DirectBuffer buffer, int offset) {
        cacheClearedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var status = cacheClearedDecoder.status();
        var cacheId = cacheClearedDecoder.cacheId();
        var requestId = cacheClearedDecoder.requestId();

        clearCacheResult.clear();
        clearCacheResult.getCacheId().copyFrom(cacheId);
        clearCacheResult.setRequestId(requestId);
        clearCacheResult.setStatus(status);
    }

    public void decodeCacheDeleted(DeleteCacheResult<ReusableString> deleteCacheResult, DirectBuffer buffer, int offset) {
        cacheDeletedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var status = cacheDeletedDecoder.status();
        var cacheId = cacheDeletedDecoder.cacheId();
        var requestId = cacheDeletedDecoder.requestId();

        deleteCacheResult.clear();
        deleteCacheResult.getCacheId().copyFrom(cacheId);
        deleteCacheResult.setRequestId(requestId);
        deleteCacheResult.setStatus(status);
    }

    public void decodeAllCacheStatsResult(CacheStatsResult<ReusableString> cacheStatsResult, DirectBuffer buffer, int offset) {
        allCacheStatsResultDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var status = allCacheStatsResultDecoder.status();
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

    public void decodeCacheSubscribeResult(CacheSubscriptionResult<ReusableString> cacheSubscriptionResult, DirectBuffer buffer, int offset) {
        cacheSubscriptionResponseDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var status = cacheSubscriptionResponseDecoder.status();
        var cacheId = cacheSubscriptionResponseDecoder.cacheId();
        var requestId = cacheSubscriptionResponseDecoder.requestId();

        cacheSubscriptionResult.clear();
        cacheSubscriptionResult.getCacheId().copyFrom(cacheId);
        cacheSubscriptionResult.setStatus(status);
        cacheSubscriptionResult.setRequestId(requestId);
    }

    public void decodeCacheUnsubscribeResult(CacheUnsubscribeResult<ReusableString> cacheUnsubscribeResult, DirectBuffer buffer, int offset) {
        cacheUnsubscribeResponseDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var status = cacheUnsubscribeResponseDecoder.status();
        var cacheId = cacheUnsubscribeResponseDecoder.cacheId();
        var requestId = cacheUnsubscribeResponseDecoder.requestId();

        cacheUnsubscribeResult.clear();
        cacheUnsubscribeResult.getCacheId().copyFrom(cacheId);
        cacheUnsubscribeResult.setStatus(status);
        cacheUnsubscribeResult.setRequestId(requestId);
    }

    public void decodeCacheEntryUpdated(CacheEntryUpdateResult<ReusableString, ReusableString, ReusableString> cacheEntryUpdateResult, DirectBuffer buffer, int offset) {
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
}
