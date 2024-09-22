package com.bhf.aeroncache.services.cachemanager;

import com.bhf.aeroncache.models.CacheCreationResult;
import com.bhf.aeroncache.services.cache.Cache;
import io.aeron.ExclusivePublication;
import io.aeron.Image;

public interface CacheManager<I, K,V> {
    CacheCreationResult createCache(I cacheId);

    Cache<K,V> getCache(I cacheId);

    void takeSnapshot(ExclusivePublication snapshotPublication);

    void loadSnapshot(Image snapshotImage);
}
