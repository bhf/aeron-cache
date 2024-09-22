package com.bhf.aeroncache.services.cachemanager;

import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.services.cache.Cache;
import io.aeron.ExclusivePublication;
import io.aeron.Image;

public interface CacheManager<I, K,V> {
    CreateCacheResult createCache(I cacheId);

    Cache<K,V> getCache(I cacheId);

    void takeSnapshot(ExclusivePublication snapshotPublication);

    void loadSnapshot(Image snapshotImage);

    ClearCacheResult clearCache(I cacheId);
}
