package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.requests.DeleteCacheRequestDetails;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.MutableDirectBuffer;
import org.apache.logging.log4j.util.Strings;

public class ReusableStringCacheResponseEncoder implements com.bhf.aeroncache.codecs.CacheResponseEncoder<ReusableString, ReusableString, ReusableString> {

    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final CacheCreatedEncoder cacheCreatedEncoder = new CacheCreatedEncoder();
    private final CacheEntryCreatedEncoder entryCreatedEncoder = new CacheEntryCreatedEncoder();
    private final CacheEntryUpdateEncoder entryUpdateEncoder = new CacheEntryUpdateEncoder();
    private final CacheEntryResultEncoder cacheEntryResultEncoder = new CacheEntryResultEncoder();
    private final AllCacheEntriesResultEncoder allCacheEntriesResultEncoder = new AllCacheEntriesResultEncoder();
    private final CacheEntryRemovedEncoder entryRemovedEncoder = new CacheEntryRemovedEncoder();
    private final CacheClearedEncoder cacheClearedEncoder = new CacheClearedEncoder();
    private final CacheDeletedEncoder cacheDeletedEncoder = new CacheDeletedEncoder();
    private final AllCacheStatsResultEncoder cacheStatsResultEncoder = new AllCacheStatsResultEncoder();
    private final CacheSubscriptionResponseEncoder cacheSubscriptionResponseEncoder = new CacheSubscriptionResponseEncoder();
    private final CacheUnsubscribeResponseEncoder cacheUnsubscribeResponseEncoder = new CacheUnsubscribeResponseEncoder();

    @Override
    public int encodeCacheCreationResult(ReusableString cacheId, CreateCacheResult<ReusableString> cacheCreationResult, MutableDirectBuffer egressBuffer) {
        cacheCreatedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheCreatedEncoder
                .status(cacheCreationResult.getStatus())
                .cacheId(cacheId.value())
                .requestId(cacheCreationResult.getRequestId());
        return cacheCreatedEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeAddCacheEntryResult(ReusableString cacheId, ReusableString key, AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult, MutableDirectBuffer egressBuffer) {
        entryCreatedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryCreatedEncoder
                .status(addCacheEntryResult.getStatus())
                .cacheId(cacheId.value())
                .key(key.value())
                .requestId(addCacheEntryResult.getRequestId());

        return entryCreatedEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeEntryUpdated(ReusableString key, ReusableString value, AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult, MutableDirectBuffer egressBuffer) {
        entryUpdateEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryUpdateEncoder.cacheId((String) addCacheEntryResult.getCacheId().value())
                .key(key.value())
                .value(value.value())
                .requestId(addCacheEntryResult.getRequestId());

        return entryUpdateEncoder.encodedLength()+headerEncoder.encodedLength();
    }

    @Override
    public int encodeCacheEntryResult(ReusableString cacheId, GetCacheEntryResult<ReusableString, ReusableString, ReusableString> getCacheEntryResult, MutableDirectBuffer egressBuffer) {
        cacheEntryResultEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheEntryResultEncoder
                .status(getCacheEntryResult.getStatus())
                .cacheId(cacheId.value())
                .key(getCacheEntryResult.getEntryKey().value());

        if (getCacheEntryResult.getEntryValue() != null) {
            cacheEntryResultEncoder.value(getCacheEntryResult.getEntryValue().value());
        } else {
            cacheEntryResultEncoder.value(Strings.EMPTY);
        }

        cacheEntryResultEncoder.requestId(getCacheEntryResult.getRequestId());

        return cacheEntryResultEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeAllCacheEntriesResult(ReusableString cacheId, GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString> getAllCacheEntriesResult, MutableDirectBuffer egressBuffer) {
        allCacheEntriesResultEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        allCacheEntriesResultEncoder
                .status(getAllCacheEntriesResult.getStatus())
                .endOfBatch(BooleanType.T);

        var values = getAllCacheEntriesResult.getValues();
        int size = values.size();
        var itemsEncoder = allCacheEntriesResultEncoder.itemsCount(size);

        values.forEach((key, value) -> {
            itemsEncoder.next();
            itemsEncoder.key(key.value()).value(value.value());
        });

        allCacheEntriesResultEncoder
                .requestId(getAllCacheEntriesResult.getRequestId())
                .cacheId(cacheId.value());

        return allCacheEntriesResultEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeRemoveCacheEntryResult(ReusableString cacheId, ReusableString key, RemoveCacheEntryResult<ReusableString, ReusableString> removeCacheEntryResult, MutableDirectBuffer egressBuffer) {
        entryRemovedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryRemovedEncoder
                .status(removeCacheEntryResult.getStatus())
                .cacheId(cacheId.value())
                .key(key.value())
                .requestId(removeCacheEntryResult.getRequestId());

        return entryRemovedEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeCacheCleared(ReusableString cacheId, ClearCacheResult<ReusableString> clearCacheResult, MutableDirectBuffer egressBuffer) {
        cacheClearedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheClearedEncoder
                .status(clearCacheResult.getStatus())
                .cacheId(cacheId.value())
                .requestId(clearCacheResult.getRequestId());

        return cacheClearedEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeDeleteCache(ReusableString cacheId, DeleteCacheResult<ReusableString> deleteCacheResult, DeleteCacheRequestDetails<ReusableString> requestDetails, MutableDirectBuffer egressBuffer) {
        cacheDeletedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheDeletedEncoder
                .status(deleteCacheResult.getStatus())
                .cacheId(cacheId.value())
                .requestId(requestDetails.getRequestId());
        return cacheDeletedEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeCacheStatsResult(CacheStatsResult<ReusableString> cacheStatsResult, MutableDirectBuffer egressBuffer) {
        cacheStatsResultEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheStatsResultEncoder.status(OperationStatus.SUCCESS);

        var values = cacheStatsResult.getStats();
        int size = values.size();
        var itemsEncoder = cacheStatsResultEncoder.statsCount(size);
        values.forEach(v -> {
            itemsEncoder.next();
            itemsEncoder.added(v.addedCount);
            itemsEncoder.removed(v.removedCount);
            itemsEncoder.cleared(v.clearedCount);
            itemsEncoder.size(v.size);
            itemsEncoder.cacheId(v.getCacheId().value());
        });

        cacheStatsResultEncoder.requestId(cacheStatsResult.getRequestId());

        return cacheStatsResultEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeCacheSubscriptionResult(CacheSubscriptionResult<ReusableString> subscriptionRequestResult, MutableDirectBuffer egressBuffer) {
        cacheSubscriptionResponseEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheSubscriptionResponseEncoder
                .status(subscriptionRequestResult.getStatus())
                .cacheId(subscriptionRequestResult.getCacheId().value())
                .requestId(subscriptionRequestResult.getRequestId());

        return cacheSubscriptionResponseEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeCacheUnsubscribeRequest(CacheUnsubscribeResult<ReusableString> unsubscribeResponse, MutableDirectBuffer egressBuffer) {
        cacheUnsubscribeResponseEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheUnsubscribeResponseEncoder
                .status(unsubscribeResponse.getStatus())
                .cacheId(unsubscribeResponse.getCacheId().value())
                .requestId(unsubscribeResponse.getRequestId());

        return cacheUnsubscribeResponseEncoder.encodedLength() + headerEncoder.encodedLength();
    }
}
