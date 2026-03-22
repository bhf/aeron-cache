package com.bhf.aeroncache.services.cacheclient;

import com.bhf.aeroncache.messages.*;

public class MapCacheClientSchemDetailsProvider implements CacheClientSchemDetailsProvider {

    @Override
    public int getCacheCreatedDecoder() {
        return CacheCreatedDecoder.TEMPLATE_ID ;
    }

    @Override
    public int getCacheEntryCreatedDecoder() {
        return CacheEntryCreatedDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCacheEntryResultDecoder() {
        return CacheEntryResultDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCacheClearedDecoder() {
        return CacheClearedDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCacheDeletedDecoder() {
        return CacheDeletedDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCacheEntryRemovedDecoder() {
        return CacheEntryRemovedDecoder.TEMPLATE_ID;
    }

    @Override
    public int getAllCacheEntriesResultDecoder() {
        return AllCacheEntriesResultDecoder.TEMPLATE_ID;
    }

    @Override
    public int getAllCacheStatsResultDecoder() {
        return AllCacheStatsResultDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCacheSubscriptionResponseDecoder() {
        return CacheSubscriptionResponseDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCacheUnsubscribeResponseDecoder() {
        return CacheUnsubscribeResponseDecoder.TEMPLATE_ID;
    }

    @Override
    public int getCacheEntryUpdateDecoder() {
        return CacheEntryUpdateDecoder.TEMPLATE_ID;
    }
}
