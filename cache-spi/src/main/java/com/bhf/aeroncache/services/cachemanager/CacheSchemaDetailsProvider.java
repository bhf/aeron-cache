package com.bhf.aeroncache.services.cachemanager;

public interface CacheSchemaDetailsProvider {

    int getCreateCacheId();

    int getAddCacheEntryId();

    int getGetCacheEntryId();

    int getRemoveCacheEntryId();

    int getClearCacheId();

    int getDeleteCacheId();

    int getGetAllCacheEntriesId();

    int getGetCacheStatsId();

    int getCacheSubscriptionRequestId();

    int getCacheUnsubscribeRequestId();

    int getBulkCacheOpsRequestId();

    int getCreateCounterCacheId();

    int getAddCounterCacheEntryId();

    int getGetCounterCacheEntryId();

    int getRemoveCounterCacheEntryId();

    int getClearCounterCacheId();

    int getDeleteCounterCacheId();

    int getGetAllCounterCacheEntriesId();

    int getCounterCacheSubscriptionRequestId();

    int getCounterCacheUnsubscribeRequestId();
}
