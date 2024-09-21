package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.CacheCreationResult;

public interface CacheManager {
    CacheCreationResult createCache(long cacheId);
}
