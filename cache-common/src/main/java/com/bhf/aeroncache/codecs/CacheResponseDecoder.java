package com.bhf.aeroncache.codecs;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.DirectBuffer;

/**
 * Decode responses from Aeron Cache.
 */
public class CacheResponseDecoder {

    public static void decodeCacheCreated(CreateCacheResult<ReusableString> createCacheResult, CacheCreatedDecoder cacheCreatedDecoder, MessageHeaderDecoder headerDecoder, DirectBuffer buffer, int offset) {
        cacheCreatedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var status = cacheCreatedDecoder.status();
        var cacheId = cacheCreatedDecoder.cacheId();
        var requestId = cacheCreatedDecoder.requestId();

        createCacheResult.clear();
        createCacheResult.getCacheId().copyFrom(cacheId);
        createCacheResult.setRequestId(requestId);
        createCacheResult.setStatus(status);
    }

    public static void decodeAllCacheEntriesResult(AllCacheEntriesResultDecoder decoder, MessageHeaderDecoder headerDecoder, GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString> getCacheEntriesResult, DirectBuffer buffer, int offset) {
        decoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var status = decoder.status();
        var eob = decoder.endOfBatch();

        getCacheEntriesResult.clear();
        getCacheEntriesResult.setStatus(status);

        // process group of key-value from the decoder directly into the flyweight

        for (AllCacheEntriesResultDecoder.ItemsDecoder item : decoder.items()) {
            var key = new ReusableString();
            var value = new ReusableString();
            key.copyFrom(item.key());
            value.copyFrom(item.value());
            getCacheEntriesResult.getValues().put(key, value);
        }

        var requestId = decoder.requestId();
        getCacheEntriesResult.setRequestId(requestId);

        var cacheID = decoder.cacheId();
        getCacheEntriesResult.getCacheId().copyFrom(cacheID);
    }

    public static void decodeGetCacheEntryResult(CacheEntryResultDecoder getCacheEntryDecoder, MessageHeaderDecoder headerDecoder, GetCacheEntryResult<ReusableString, ReusableString, ReusableString> getCacheEntryResult, DirectBuffer buffer, int offset) {
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

    public static void decodeAddCacheEntryResult(CacheEntryCreatedDecoder addCacheEntryDecoder, MessageHeaderDecoder headerDecoder, AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult, DirectBuffer buffer, int offset) {
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

    public static void decodeCacheEntryRemoved(CacheEntryRemovedDecoder cacheEntryRemovedDecoder, MessageHeaderDecoder headerDecoder, RemoveCacheEntryResult<ReusableString, ReusableString> removeCacheEntryResult, DirectBuffer buffer, int offset) {
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

    public static void decodeCacheCleared(CacheClearedDecoder cacheClearedDecoder, MessageHeaderDecoder headerDecoder, ClearCacheResult<ReusableString> clearCacheResult, DirectBuffer buffer, int offset) {
        cacheClearedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var status = cacheClearedDecoder.status();
        var cacheId = cacheClearedDecoder.cacheId();
        var requestId = cacheClearedDecoder.requestId();

        clearCacheResult.clear();
        clearCacheResult.getCacheId().copyFrom(cacheId);
        clearCacheResult.setRequestId(requestId);
        clearCacheResult.setStatus(status);
    }

    public static void decodeCacheDeleted(CacheDeletedDecoder cacheDeletedDecoder, MessageHeaderDecoder headerDecoder, DeleteCacheResult<ReusableString> deleteCacheResult, DirectBuffer buffer, int offset) {
        cacheDeletedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var status = cacheDeletedDecoder.status();
        var cacheId = cacheDeletedDecoder.cacheId();
        var requestId = cacheDeletedDecoder.requestId();

        deleteCacheResult.clear();
        deleteCacheResult.getCacheId().copyFrom(cacheId);
        deleteCacheResult.setRequestId(requestId);
        deleteCacheResult.setStatus(status);
    }

    public static void decodeAllCacheStatsResult(AllCacheStatsResultDecoder allCacheStatsResultDecoder, MessageHeaderDecoder headerDecoder, CacheStatsResult<ReusableString> cacheStatsResult, DirectBuffer buffer, int offset) {
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

    public static void decodeCacheSubscribeResult(CacheSubscriptionResponseDecoder cacheSubscriptionResponseDecoder, MessageHeaderDecoder headerDecoder, CacheSubscriptionResult<ReusableString> cacheSubscriptionResult, DirectBuffer buffer, int offset) {
        cacheSubscriptionResponseDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var status = cacheSubscriptionResponseDecoder.status();
        var cacheId = cacheSubscriptionResponseDecoder.cacheId();
        var requestId = cacheSubscriptionResponseDecoder.requestId();

        cacheSubscriptionResult.clear();
        cacheSubscriptionResult.getCacheId().copyFrom(cacheId);
        cacheSubscriptionResult.setStatus(status);
        cacheSubscriptionResult.setRequestId(requestId);
    }

    public static void decodeCacheUnsubscribeResult(CacheUnsubscribeResponseDecoder cacheUnsubscribeResponseDecoder, MessageHeaderDecoder headerDecoder, CacheUnsubscribeResult<ReusableString> cacheUnsubscribeResult, DirectBuffer buffer, int offset) {
        cacheUnsubscribeResponseDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var status = cacheUnsubscribeResponseDecoder.status();
        var cacheId = cacheUnsubscribeResponseDecoder.cacheId();
        var requestId = cacheUnsubscribeResponseDecoder.requestId();

        cacheUnsubscribeResult.clear();
        cacheUnsubscribeResult.getCacheId().copyFrom(cacheId);
        cacheUnsubscribeResult.setStatus(status);
        cacheUnsubscribeResult.setRequestId(requestId);
    }

    public static void decodeCacheEntryUpdated(CacheEntryUpdateDecoder cacheEntryUpdateDecoder, MessageHeaderDecoder headerDecoder, CacheEntryUpdateResult<ReusableString, ReusableString, ReusableString> cacheEntryUpdateResult, DirectBuffer buffer, int offset) {
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
