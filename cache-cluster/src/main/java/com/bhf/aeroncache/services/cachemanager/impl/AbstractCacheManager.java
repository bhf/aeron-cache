package com.bhf.aeroncache.services.cachemanager.impl;

import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.services.cache.CacheFactory;
import com.bhf.aeroncache.services.cachemanager.CacheManager;

public abstract class AbstractCacheManager<I, K, V> implements CacheManager<I, K, V> {

    CreateCacheResult cacheCreationResult=new CreateCacheResult();
    CacheFactory<K,V> cacheFactory=new CacheFactory<>();
}
