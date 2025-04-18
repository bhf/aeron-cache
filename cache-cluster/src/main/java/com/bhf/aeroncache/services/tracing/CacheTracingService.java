package com.bhf.aeroncache.services.tracing;

import com.bhf.aeroncache.models.requests.*;

/**
 * A service for tracing requests made to Aeron Cache.
 */
public interface CacheTracingService {
    void startHandleDeleteCache(DeleteCacheRequestDetails requestDetails);

    void endHandleDeleteCache(DeleteCacheRequestDetails requestDetails);

    void startHandleClearCache(ClearCacheRequestDetails requestDetails);

    void endHandleClearCache(ClearCacheRequestDetails requestDetails);

    void startRemoveCacheEntry(RemoveCacheEntryRequestDetails requestDetails);

    void endRemoveCacheEntry(RemoveCacheEntryRequestDetails requestDetails);

    void startAddCacheEntry(AddCacheEntryRequestDetails requestDetails);

    void endAddCacheEntry(AddCacheEntryRequestDetails requestDetails);

    void startGetCacheEntry(GetCacheEntryRequestDetails requestDetails);

    void endGetCacheEntry(GetCacheEntryRequestDetails requestDetails);

    void startGetAllCacheEntries(GetAllCacheEntriesRequestDetails requestDetails);

    void endGetAllCacheEntries(GetAllCacheEntriesRequestDetails requestDetails);

    void startCreateCacheRequest(CreateCacheRequestDetails requestDetails);

    void endCreateCacheRequest(CreateCacheRequestDetails requestDetails);

    void startGetAllStatsRequest(GetCacheStatsRequestDetails requestDetails);

    void endGetAllStatsRequest(GetCacheStatsRequestDetails requestDetails);
}
