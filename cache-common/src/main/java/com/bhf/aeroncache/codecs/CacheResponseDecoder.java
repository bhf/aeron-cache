package com.bhf.aeroncache.codecs;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.DirectBuffer;

/**
 * Decode responses from Aeron Cache.
 */
public class CacheResponseDecoder {

    public static void decodeCacheCreated(CreateCacheResult<ReusableLong> createCacheResult, CacheCreatedDecoder cacheCreatedDecoder, MessageHeaderDecoder headerDecoder, DirectBuffer buffer, int offset) {
        cacheCreatedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = cacheCreatedDecoder.cacheId();
        var requestId = cacheCreatedDecoder.requestId();
        var status = cacheCreatedDecoder.status();

        createCacheResult.clear();
        createCacheResult.getCacheId().copyFrom(cacheId);
        createCacheResult.setRequestId(requestId);
        createCacheResult.setStatus(status);
    }

    public static void decodeAllCacheEntriesResult(AllCacheEntriesResultDecoder decoder, MessageHeaderDecoder headerDecoder, GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString> getCacheEntriesResult, DirectBuffer buffer, int offset) {
        decoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheID = decoder.cacheId();
        var status = decoder.status();
        var eob = decoder.endOfBatch();

        getCacheEntriesResult.clear();
        getCacheEntriesResult.getCacheId().copyFrom(cacheID);
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
    }

    public static void decodeGetCacheEntryResult(CacheEntryResultDecoder getCacheEntryDecoder, MessageHeaderDecoder headerDecoder, GetCacheEntryResult<ReusableLong, ReusableString, ReusableString> getCacheEntryResult, DirectBuffer buffer, int offset) {
        getCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheID = getCacheEntryDecoder.cacheId();
        var status = getCacheEntryDecoder.status();
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

    public static void decodeAddCacheEntryResult(CacheEntryCreatedDecoder addCacheEntryDecoder, MessageHeaderDecoder headerDecoder, AddCacheEntryResult<ReusableLong, ReusableString> addCacheEntryResult, DirectBuffer buffer, int offset) {
        addCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = addCacheEntryDecoder.cacheId();
        var key = addCacheEntryDecoder.key();
        var requestId = addCacheEntryDecoder.requestId();
        var status = addCacheEntryDecoder.status();

        addCacheEntryResult.clear();
        addCacheEntryResult.setEntryAdded(true);
        addCacheEntryResult.getEntryKey().copyFrom(key);
        addCacheEntryResult.getCacheId().copyFrom(cacheId);
        addCacheEntryResult.setRequestId(requestId);
        addCacheEntryResult.setStatus(status);
    }

    public static void decodeCacheEntryRemoved(CacheEntryRemovedDecoder cacheEntryRemovedDecoder, MessageHeaderDecoder headerDecoder, RemoveCacheEntryResult<ReusableLong, ReusableString> removeCacheEntryResult, DirectBuffer buffer, int offset) {
        cacheEntryRemovedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = cacheEntryRemovedDecoder.cacheId();
        var key = cacheEntryRemovedDecoder.key();
        var requestId = cacheEntryRemovedDecoder.requestId();
        var status = cacheEntryRemovedDecoder.status();

        removeCacheEntryResult.clear();
        removeCacheEntryResult.getKey().copyFrom(key);
        removeCacheEntryResult.getCacheId().copyFrom(cacheId);
        removeCacheEntryResult.setRequestId(requestId);
        removeCacheEntryResult.setStatus(status);
    }

    public static void decodeCacheCleared(CacheClearedDecoder cacheClearedDecoder, MessageHeaderDecoder headerDecoder, ClearCacheResult<ReusableLong> clearCacheResult, DirectBuffer buffer, int offset) {
        cacheClearedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = cacheClearedDecoder.cacheId();
        var requestId = cacheClearedDecoder.requestId();
        var status = cacheClearedDecoder.status();

        clearCacheResult.clear();
        clearCacheResult.getCacheId().copyFrom(cacheId);
        clearCacheResult.setRequestId(requestId);
        clearCacheResult.setStatus(status);
    }

    public static void decodeCacheDeleted(CacheDeletedDecoder cacheDeletedDecoder, MessageHeaderDecoder headerDecoder, DeleteCacheResult<ReusableLong> deleteCacheResult, DirectBuffer buffer, int offset) {
        cacheDeletedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = cacheDeletedDecoder.cacheId();
        var requestId = cacheDeletedDecoder.requestId();
        var status = cacheDeletedDecoder.status();

        deleteCacheResult.clear();
        deleteCacheResult.getCacheId().copyFrom(cacheId);
        deleteCacheResult.setRequestId(requestId);
        deleteCacheResult.setStatus(status);
    }

    public static void decodeAllCacheStatsResult(AllCacheStatsResultDecoder allCacheStatsResultDecoder, MessageHeaderDecoder headerDecoder, CacheStatsResult<ReusableLong> cacheStatsResult, DirectBuffer buffer, int offset) {
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
            var id = new ReusableLong();
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

    public static void decodeCacheSubscribeResult(CacheSubscriptionResponseDecoder cacheSubscriptionResponseDecoder, MessageHeaderDecoder headerDecoder, CacheSubscriptionResult<ReusableLong> cacheSubscriptionResult, DirectBuffer buffer, int offset) {
        cacheSubscriptionResponseDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = cacheSubscriptionResponseDecoder.cacheId();
        var status = cacheSubscriptionResponseDecoder.status();
        var requestId = cacheSubscriptionResponseDecoder.requestId();

        cacheSubscriptionResult.clear();
        cacheSubscriptionResult.getCacheId().copyFrom(cacheId);
        cacheSubscriptionResult.setStatus(status);
        cacheSubscriptionResult.setRequestId(requestId);
    }

    public static void decodeCacheUnsubscribeResult(CacheUnsubscribeResponseDecoder cacheUnsubscribeResponseDecoder, MessageHeaderDecoder headerDecoder, CacheUnsubscribeResult<ReusableLong> cacheUnsubscribeResult, DirectBuffer buffer, int offset) {
        cacheUnsubscribeResponseDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var cacheId = cacheUnsubscribeResponseDecoder.cacheId();
        var status = cacheUnsubscribeResponseDecoder.status();
        var requestId = cacheUnsubscribeResponseDecoder.requestId();

        cacheUnsubscribeResult.clear();
        cacheUnsubscribeResult.getCacheId().copyFrom(cacheId);
        cacheUnsubscribeResult.setStatus(status);
        cacheUnsubscribeResult.setRequestId(requestId);
    }

    public static void decodeCacheEntryUpdated(CacheEntryUpdateDecoder cacheEntryUpdateDecoder, MessageHeaderDecoder headerDecoder, CacheEntryUpdateResult<ReusableLong, ReusableString, ReusableString> cacheEntryUpdateResult, DirectBuffer buffer, int offset) {
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
