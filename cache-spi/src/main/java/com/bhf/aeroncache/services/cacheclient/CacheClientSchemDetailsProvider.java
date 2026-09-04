package com.bhf.aeroncache.services.cacheclient;

public interface CacheClientSchemDetailsProvider {

    int getCacheCreatedId();

    int getCacheEntryCreatedId();

    int getCacheEntryResultId();

    int getCacheClearedId();

    int getCacheDeletedId();

    int getCacheEntryRemovedId();

    int getAllCacheEntriesResultId();

    int getAllCacheStatsResultId();

    int getCacheSubscriptionResponseId();

    int getCacheUnsubscribeResponseId();

    int getCacheEntryUpdateId();

    int bulkOperationsResponseId();

    // Counter cache response TIDs

    int getCounterCacheCreatedId();

    int getCounterCacheEntryCreatedId();

    int getCounterCacheEntryResultId();

    int getCounterCacheClearedId();

    int getCounterCacheDeletedId();

    int getCounterCacheEntryRemovedId();

    int getAllCounterCacheEntriesResultId();

    int getCounterCacheSubscriptionResponseId();

    int getCounterCacheUnsubscribeResponseId();

    int getIncrementCounterResponseId();

    int getDecrementCounterResponseId();

    int getSetCounterResponseId();

    int getCounterCacheEntryUpdateId();

    int getAllCounterCacheStatsResultId();
}
