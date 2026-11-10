package com.bhf.aeroncache.services.near;

import com.bhf.aeroncache.near.NearCacheManager;
import com.bhf.aeroncache.near.NearCacheManagerFactory;

public class NearCacheManagerFactoryImpl implements NearCacheManagerFactory {

    @Override
    public <V> NearCacheManager<V> createNearCacheManager() {
        return new ConcurrentNearCacheManager<>();
    }
}
