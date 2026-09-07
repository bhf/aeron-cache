package com.bhf.aeroncache.services.cachemanager;

import com.bhf.aeroncache.messages.*;

public class MapCacheSchemaDetailsProvider implements CacheSchemaDetailsProvider {

    @Override
    public int getCreateCacheId() {
        return CreateCacheEncoder.TEMPLATE_ID;
    }

    @Override
    public int getAddCacheEntryId() {
        return AddCacheEntryEncoder.TEMPLATE_ID;
    }

    @Override
    public int getGetCacheEntryId() {
        return GetCacheEntryEncoder.TEMPLATE_ID;
    }

    @Override
    public int getRemoveCacheEntryId() {
        return RemoveCacheEntryEncoder.TEMPLATE_ID;
    }

    @Override
    public int getPatchCacheEntryId() {
        return PatchCacheEntryEncoder.TEMPLATE_ID;
    }

    @Override
    public int getClearCacheId() {
        return ClearCacheEncoder.TEMPLATE_ID;
    }

    @Override
    public int getDeleteCacheId() {
        return DeleteCacheEncoder.TEMPLATE_ID;
    }

    @Override
    public int getGetAllCacheEntriesId() {
        return GetAllCacheEntriesEncoder.TEMPLATE_ID;
    }

    @Override
    public int getGetCacheStatsId() {
        return GetCacheStatsEncoder.TEMPLATE_ID;
    }

    @Override
    public int getCacheSubscriptionRequestId() {
        return CacheSubscriptionRequestEncoder.TEMPLATE_ID;
    }

    @Override
    public int getCacheUnsubscribeRequestId() {
        return CacheUnsubscribeRequestEncoder.TEMPLATE_ID;
    }

    @Override
    public int getBulkCacheOpsRequestId() {
        return BulkOperationRequestEncoder.TEMPLATE_ID;
    }

    @Override
    public int getCreateCounterCacheId() {
        return CreateCounterCacheEncoder.TEMPLATE_ID;
    }

    @Override
    public int getAddCounterCacheEntryId() {
        return AddCounterRequestEncoder.TEMPLATE_ID;
    }

    @Override
    public int getGetCounterCacheEntryId() {
        return GetCounterCacheEntryEncoder.TEMPLATE_ID;
    }

    @Override
    public int getRemoveCounterCacheEntryId() {
        return RemoveCounterRequestEncoder.TEMPLATE_ID;
    }

    @Override
    public int getClearCounterCacheId() {
        return ClearCounterCacheRequestEncoder.TEMPLATE_ID;
    }

    @Override
    public int getDeleteCounterCacheId() {
        return DeleteCounterCacheEncoder.TEMPLATE_ID;
    }

    @Override
    public int getGetAllCounterCacheEntriesId() {
        return GetAllCounterCacheEntriesEncoder.TEMPLATE_ID;
    }

    @Override
    public int getCounterCacheSubscriptionRequestId() {
        return CounterCacheSubscriptionRequestEncoder.TEMPLATE_ID;
    }

    @Override
    public int getCounterCacheUnsubscribeRequestId() {
        return CounterCacheUnsubscribeRequestEncoder.TEMPLATE_ID;
    }

    @Override
    public int getCounterIncrementRequestId() {
        return IncrementCounterRequestEncoder.TEMPLATE_ID;
    }

    @Override
    public int getCounterDecrementRequestId() {
        return DecrementCounterRequestEncoder.TEMPLATE_ID;
    }

    @Override
    public int getSetCounterRequestId() {
        return SetCounterRequestEncoder.TEMPLATE_ID;
    }

    @Override
    public int getGetCounterStatsId() {
        return GetCounterStatsEncoder.TEMPLATE_ID;
    }
}
