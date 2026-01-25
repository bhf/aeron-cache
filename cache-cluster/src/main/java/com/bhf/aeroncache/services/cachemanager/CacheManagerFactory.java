package com.bhf.aeroncache.services.cachemanager;

import com.bhf.aeroncache.models.Reusable;

public interface CacheManagerFactory<I extends Reusable, K extends Reusable, V extends Reusable> {

    CacheManager<I, K, V> getCacheManager();
}
