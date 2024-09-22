package com.bhf.aeroncache.services.cachemanager.impl;

import com.bhf.aeroncache.models.CacheCreationResult;
import com.bhf.aeroncache.services.cache.CacheFactory;
import com.bhf.aeroncache.services.cachemanager.CacheManager;

public abstract class AbstractCacheManager<I, K, V> implements CacheManager<I, K, V> {

    CacheCreationResult cacheCreationResult=new CacheCreationResult();
    CacheFactory<K,V> cacheFactory=new CacheFactory<>();
}
