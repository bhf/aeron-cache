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

}
