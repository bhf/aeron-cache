package com.bhf.aeroncache.services.cacheclient;

import com.bhf.aeroncache.messages.*;

public class MapCacheClientSchemDetailsProvider implements CacheClientSchemDetailsProvider {

    @Override
    public int getCacheCreatedId() {
        return CacheCreatedDecoder.TEMPLATE_ID ;
    }

    @Override
    public int getCacheEntryCreatedId() {
        return CacheEntryCreatedDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCacheEntryResultId() {
        return CacheEntryResultDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCacheClearedId() {
        return CacheClearedDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCacheDeletedId() {
        return CacheDeletedDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCacheEntryRemovedId() {
        return CacheEntryRemovedDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCacheItemRemovalCancelledId() {
        return CacheItemRemovalCancelledDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCacheEntryPatchedId() {
        return CacheEntryPatchedDecoder.TEMPLATE_ID;
    }

    @Override
    public int getAllCacheEntriesResultId() {
        return AllCacheEntriesResultDecoder.TEMPLATE_ID;
    }

    @Override
    public int getAllCacheStatsResultId() {
        return AllCacheStatsResultDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCacheSubscriptionResponseId() {
        return CacheSubscriptionResponseDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCacheUnsubscribeResponseId() {
        return CacheUnsubscribeResponseDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCacheEntryUpdateId() {
        return CacheEntryUpdateDecoder.TEMPLATE_ID;
    }

    @Override
    public int bulkOperationsResponseId() {
        return BulkOperationResponseDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCounterCacheCreatedId() {
        return CreateCounterCacheResponseDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCounterCacheEntryCreatedId() {
        return AddCounterResponseDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCounterCacheEntryResultId() {
        return CounterCacheEntryResultDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCounterCacheClearedId() {
        return ClearCounterCacheResponseDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCounterCacheDeletedId() {
        return DeleteCounterCacheResponseDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCounterCacheEntryRemovedId() {
        return RemoveCounterResponseDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCounterItemRemovalCancelledId() {
        return CounterItemRemovalCancelledDecoder.TEMPLATE_ID;
    }

    @Override
    public int getAllCounterCacheEntriesResultId() {
        return AllCounterCacheEntriesResultDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCounterCacheSubscriptionResponseId() {
        return CounterCacheSubscriptionResponseDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCounterCacheUnsubscribeResponseId() {
        return CounterCacheUnsubscribeResponseDecoder.TEMPLATE_ID;
    }

    @Override
    public int getIncrementCounterResponseId() {
        return IncrementCounterResponseDecoder.TEMPLATE_ID;
    }

    @Override
    public int getDecrementCounterResponseId() {
        return DecrementCounterResponseDecoder.TEMPLATE_ID;
    }

    @Override
    public int getSetCounterResponseId() {
        return SetCounterResponseDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCounterCacheEntryUpdateId() {
        return CounterCacheEntryUpdateDecoder.TEMPLATE_ID;
    }

    @Override
    public int getAllCounterCacheStatsResultId() {
        return AllCounterCacheStatsResultDecoder.TEMPLATE_ID;
    }
}
