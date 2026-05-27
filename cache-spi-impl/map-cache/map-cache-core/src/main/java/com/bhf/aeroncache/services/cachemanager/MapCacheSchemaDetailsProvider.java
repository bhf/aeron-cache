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
}
