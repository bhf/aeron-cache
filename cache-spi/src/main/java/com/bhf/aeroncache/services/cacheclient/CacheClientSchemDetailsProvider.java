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

}
