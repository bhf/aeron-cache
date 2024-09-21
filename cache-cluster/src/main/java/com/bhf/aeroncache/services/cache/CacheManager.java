package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.AddCacheEntryResult;
import com.bhf.aeroncache.models.CacheCreationResult;

public interface CacheManager {
    CacheCreationResult createCache(long cacheId);

    AddCacheEntryResult addCacheEntry(long cacheId, String key, String value);
}
