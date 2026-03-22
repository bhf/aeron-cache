package com.bhf.aeroncache.services.cacheclient;

public interface CacheClientSchemDetailsProvider {

    int getCacheCreatedDecoder();
    int getCacheEntryCreatedDecoder();
    int getCacheEntryResultDecoder();
    int getCacheClearedDecoder();
    int getCacheDeletedDecoder();
    int getCacheEntryRemovedDecoder();
    int getAllCacheEntriesResultDecoder();
    int getAllCacheStatsResultDecoder();
    int getCacheSubscriptionResponseDecoder();
    int getCacheUnsubscribeResponseDecoder();
    int getCacheEntryUpdateDecoder();

}
