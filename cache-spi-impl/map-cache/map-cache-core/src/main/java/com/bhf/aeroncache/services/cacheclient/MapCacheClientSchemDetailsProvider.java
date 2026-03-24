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
}
