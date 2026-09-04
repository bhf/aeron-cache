package com.bhf.aeroncache.codecs.response;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.DirectBuffer;

public class ReusableStringCountersCacheResponseDecoder implements CountersCacheResponseDecoder<ReusableString,ReusableString, ReusableLong>{

    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final CreateCounterCacheResponseDecoder cacheCreatedDecoder = new CreateCounterCacheResponseDecoder();
    private final AllCounterCacheEntriesResultDecoder allCacheEntriesResultDecoder = new AllCounterCacheEntriesResultDecoder();
    private final CounterCacheEntryResultDecoder getCacheEntryDecoder = new CounterCacheEntryResultDecoder();
    private final AddCounterResponseDecoder addCacheEntryDecoder = new AddCounterResponseDecoder();
    private final RemoveCounterResponseDecoder cacheEntryRemovedDecoder = new RemoveCounterResponseDecoder();
    private final ClearCounterCacheResponseDecoder cacheClearedDecoder = new ClearCounterCacheResponseDecoder();
    private final DeleteCounterCacheResponseDecoder cacheDeletedDecoder = new DeleteCounterCacheResponseDecoder();
    
    private final IncrementCounterResponseDecoder incrementCounterResponseDecoder = new IncrementCounterResponseDecoder();
    private final DecrementCounterResponseDecoder decrementCounterResponseDecoder = new DecrementCounterResponseDecoder();
    private final SetCounterResponseDecoder setCounterResponseDecoder = new SetCounterResponseDecoder();
    private final CounterCacheSubscriptionResponseDecoder cacheSubscriptionResponseDecoder = new CounterCacheSubscriptionResponseDecoder();
    private final CounterCacheUnsubscribeResponseDecoder cacheUnsubscribeResponseDecoder = new CounterCacheUnsubscribeResponseDecoder();
    private final AllCounterCacheStatsResultDecoder allCacheStatsResultDecoder = new AllCounterCacheStatsResultDecoder();

    @Override
    public void decodeIncrementCounterResponse(DirectBuffer buffer, int offset, IncrementCounterResult<ReusableString, ReusableString> result) {
        incrementCounterResponseDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        result.clear();
        result.setCounterValue(incrementCounterResponseDecoder.counterValue());
        result.setStatus(getOperationStatus(incrementCounterResponseDecoder.status()));
        result.getCacheId().copyFrom(incrementCounterResponseDecoder.cacheId());
        result.setRequestId(incrementCounterResponseDecoder.requestId());
        result.getKey().copyFrom(incrementCounterResponseDecoder.counterId());
    }

    @Override
    public void decodeDecrementCounterResponse(DirectBuffer buffer, int offset, DecrementCounterResult<ReusableString, ReusableString> result) {
        decrementCounterResponseDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        result.clear();
        result.setCounterValue(decrementCounterResponseDecoder.counterValue());
        result.setStatus(getOperationStatus(decrementCounterResponseDecoder.status()));
        result.getCacheId().copyFrom(decrementCounterResponseDecoder.cacheId());
        result.setRequestId(decrementCounterResponseDecoder.requestId());
        result.getKey().copyFrom(decrementCounterResponseDecoder.counterId());
    }

    @Override
    public void decodeSetCounterResponse(DirectBuffer buffer, int offset, SetCounterResult<ReusableString, ReusableString> result) {
        setCounterResponseDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        result.clear();
        result.setCounterValue(setCounterResponseDecoder.counterValue());
        result.setStatus(getOperationStatus(setCounterResponseDecoder.status()));
        result.getCacheId().copyFrom(setCounterResponseDecoder.cacheId());
        result.setRequestId(setCounterResponseDecoder.requestId());
        result.getKey().copyFrom(setCounterResponseDecoder.counterId());
    }

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

    @Override
    public void decodeAllCacheEntriesResult(DirectBuffer buffer, int offset, GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableLong> getCacheEntriesResult) {
        allCacheEntriesResultDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var status = getOperationStatus(allCacheEntriesResultDecoder.status());
        var eob = allCacheEntriesResultDecoder.endOfBatch();

        getCacheEntriesResult.clear();
        getCacheEntriesResult.setStatus(status);

        for (AllCounterCacheEntriesResultDecoder.ItemsDecoder entry : allCacheEntriesResultDecoder.items()) {
            var valLong = entry.counterValue();
            var keyStr = entry.key();
            var resKey = new ReusableString();
            resKey.copyFrom(keyStr);
            var resVal = new ReusableLong();
            resVal.copyFrom(valLong);
            getCacheEntriesResult.getValues().put(resKey, resVal);
        }

        var requestId = allCacheEntriesResultDecoder.requestId();
        getCacheEntriesResult.setRequestId(requestId);

        var cacheID = allCacheEntriesResultDecoder.cacheId();
        getCacheEntriesResult.getCacheId().copyFrom(cacheID);
    }

    @Override
    public void decodeGetCacheEntryResult(DirectBuffer buffer, int offset, GetCacheEntryResult<ReusableString, ReusableString, ReusableLong> getCacheEntryResult) {
        getCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var status = getOperationStatus(getCacheEntryDecoder.status());
        var value = getCacheEntryDecoder.counterValue();
        var cacheID = getCacheEntryDecoder.cacheId();
        var key = getCacheEntryDecoder.key();
        var requestId = getCacheEntryDecoder.requestId();

        getCacheEntryResult.clear();
        var resKey = new ReusableString();
        resKey.copyFrom(key);
        var resVal = new ReusableLong();
        resVal.copyFrom(value);
        getCacheEntryResult.setStatus(status);
        getCacheEntryResult.getCacheId().copyFrom(cacheID);
        getCacheEntryResult.getEntryKey().copyFrom(resKey);
        getCacheEntryResult.getEntryValue().copyFrom(resVal);
        getCacheEntryResult.setRequestId(requestId);
    }

    @Override
    public void decodeAddCacheEntryResult(DirectBuffer buffer, int offset, AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult) {
        addCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var status = getOperationStatus(addCacheEntryDecoder.status());
        var cacheId = addCacheEntryDecoder.cacheId();
        var requestId = addCacheEntryDecoder.requestId();
        var key = addCacheEntryDecoder.counterId();

        addCacheEntryResult.clear();
        addCacheEntryResult.setStatus(status);
        addCacheEntryResult.getCacheId().copyFrom(cacheId);
        var resKey = new ReusableString();
        resKey.copyFrom(key);
        addCacheEntryResult.getEntryKey().copyFrom(resKey);
        addCacheEntryResult.setRequestId(requestId);
    }

    @Override
    public void decodeCacheEntryRemoved(DirectBuffer buffer, int offset, RemoveCacheEntryResult<ReusableString, ReusableString> removeCacheEntryResult) {
        cacheEntryRemovedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var counterValue = cacheEntryRemovedDecoder.counterValue();
        var status = getOperationStatus(cacheEntryRemovedDecoder.status());
        var cacheId = cacheEntryRemovedDecoder.cacheId();
        var requestId = cacheEntryRemovedDecoder.requestId();
        var key = cacheEntryRemovedDecoder.counterId();

        removeCacheEntryResult.clear();
        removeCacheEntryResult.setStatus(status);
        removeCacheEntryResult.getCacheId().copyFrom(cacheId);
        var resKey = new ReusableString();
        resKey.copyFrom(key);
        removeCacheEntryResult.getKey().copyFrom(resKey);
        removeCacheEntryResult.setRequestId(requestId);
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
    public void decodeCacheSubscribeResult(DirectBuffer buffer, int offset, CacheSubscriptionResult<ReusableString, ReusableString, ReusableLong> cacheSubscriptionResult) {
        cacheSubscriptionResult.clear();

        if (cacheSubscriptionResult.getEntries() != null) {
            cacheSubscriptionResult.getEntries().clear();
        }

        cacheSubscriptionResponseDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var status = getOperationStatus(cacheSubscriptionResponseDecoder.status());
        var isEob = cacheSubscriptionResponseDecoder.isEob() == BooleanType.T;
        cacheSubscriptionResult.setEob(isEob);

        for (var item : cacheSubscriptionResponseDecoder.items()) {
            var val = new ReusableLong();
            val.copyFrom(item.counterValue());
            var key = new ReusableString();
            key.copyFrom(item.counterId());
            cacheSubscriptionResult.getEntries().put(key, val);
            item.cacheId();
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
        cacheUnsubscribeResult.setRequestId(requestId);
        cacheUnsubscribeResult.setStatus(status);
    }

    private final CounterCacheEntryUpdateDecoder cacheEntryUpdateDecoder = new CounterCacheEntryUpdateDecoder();

    @Override
    public void decodeCacheEntryUpdated(DirectBuffer buffer, int offset, CacheEntryUpdateResult<ReusableString, ReusableString, ReusableLong> cacheEntryUpdateResult) {
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
    public void decodeBulkCacheOpsResult(DirectBuffer buffer, int offset, BulkCacheOpsResult<ReusableString, ReusableString, ReusableLong> bulkCacheOpsResult) {

    }

    @Override
    public void decodeAllCacheStatsResult(DirectBuffer buffer, int offset, CacheStatsResult<ReusableString> cacheStatsResult) {
        allCacheStatsResultDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var status = getOperationStatus(allCacheStatsResultDecoder.status());
        cacheStatsResult.clear();
        cacheStatsResult.setOperationStatus(status);

        for (AllCounterCacheStatsResultDecoder.StatsDecoder item : allCacheStatsResultDecoder.stats()) {
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


    private com.bhf.aeroncache.models.results.CacheOperationStatus getOperationStatus(com.bhf.aeroncache.messages.OperationStatus status) {
        if (status == null) {
            return com.bhf.aeroncache.models.results.CacheOperationStatus.ERROR;
        }
        switch(status){
            case CACHE_EXISTS -> { return CacheOperationStatus.CACHE_EXISTS; }
            case DUPLICATE_SUBSCRIPTION -> { return CacheOperationStatus.DUPLICATE_SUBSCRIPTION; }
            case SUCCESS -> { return com.bhf.aeroncache.models.results.CacheOperationStatus.SUCCESS; }
            case UNKNOWN_KEY -> { return CacheOperationStatus.UNKNOWN_KEY; }
            case UNKNOWN_CACHE -> { return CacheOperationStatus.UNKNOWN_CACHE; }
            case UNKNOWN_SUBSCRIPTION -> { return CacheOperationStatus.UNKNOWN_SUBSCRIPTION; }
            case NULL_VAL -> { return CacheOperationStatus.NULL_VAL; }
            default -> { return com.bhf.aeroncache.models.results.CacheOperationStatus.ERROR; }
        }
    }

}
