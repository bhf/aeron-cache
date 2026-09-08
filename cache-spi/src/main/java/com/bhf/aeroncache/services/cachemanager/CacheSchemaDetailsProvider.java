package com.bhf.aeroncache.services.cachemanager;

public interface CacheSchemaDetailsProvider {

    // Cache TIDs

    int getCreateCacheId();

    int getAddCacheEntryId();

    int getGetCacheEntryId();

    int getRemoveCacheEntryId();

    int getCancelCacheItemRemovalId();

    int getPatchCacheEntryId();

    int getClearCacheId();

    int getDeleteCacheId();

    int getGetAllCacheEntriesId();

    int getGetCacheStatsId();

    int getCacheSubscriptionRequestId();

    int getCacheUnsubscribeRequestId();

    int getBulkCacheOpsRequestId();

    // Counters TIDs

    int getCreateCounterCacheId();

    int getAddCounterCacheEntryId();

    int getGetCounterCacheEntryId();

    int getRemoveCounterCacheEntryId();

    int getCancelCounterItemRemovalId();

    int getClearCounterCacheId();

    int getDeleteCounterCacheId();

    int getGetAllCounterCacheEntriesId();

    int getCounterCacheSubscriptionRequestId();

    int getCounterCacheUnsubscribeRequestId();

    int getCounterIncrementRequestId();

    int getCounterDecrementRequestId();

    int getSetCounterRequestId();

    int getGetCounterStatsId();
}
