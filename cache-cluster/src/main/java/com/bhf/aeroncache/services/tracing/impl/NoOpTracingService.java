package com.bhf.aeroncache.services.tracing.impl;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.requests.*;
import com.bhf.aeroncache.services.tracing.CacheTracingService;

/**
 * A No Operation tracing service.
 */
public class NoOpTracingService implements CacheTracingService {
    @Override
    public void startHandleDeleteCache(DeleteCacheRequestDetails requestDetails) {

    }

    @Override
    public void endHandleDeleteCache(DeleteCacheRequestDetails requestDetails) {

    }

    @Override
    public void startHandleClearCache(ClearCacheRequestDetails requestDetails) {

    }

    @Override
    public void endHandleClearCache(ClearCacheRequestDetails requestDetails) {

    }

    @Override
    public void startRemoveCacheEntry(RemoveCacheEntryRequestDetails requestDetails) {

    }

    @Override
    public void endRemoveCacheEntry(RemoveCacheEntryRequestDetails requestDetails) {

    }

    @Override
    public void startAddCacheEntry(AddCacheEntryRequestDetails requestDetails) {

    }

    @Override
    public void endAddCacheEntry(AddCacheEntryRequestDetails requestDetails) {

    }

    @Override
    public void startGetCacheEntry(GetCacheEntryRequestDetails requestDetails) {

    }

    @Override
    public void endGetCacheEntry(GetCacheEntryRequestDetails requestDetails) {

    }

    @Override
    public void startGetAllCacheEntries(GetAllCacheEntriesRequestDetails requestDetails) {

    }

    @Override
    public void endGetAllCacheEntries(GetAllCacheEntriesRequestDetails requestDetails) {

    }

    @Override
    public void startCreateCacheRequest(CreateCacheRequestDetails requestDetails) {

    }

    @Override
    public void endCreateCacheRequest(CreateCacheRequestDetails requestDetails) {

    }

    @Override
    public void startGetAllStatsRequest(GetCacheStatsRequestDetails requestDetails) {

    }

    @Override
    public void endGetAllStatsRequest(GetCacheStatsRequestDetails requestDetails) {

    }
}
