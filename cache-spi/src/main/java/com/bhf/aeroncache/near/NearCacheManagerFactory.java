package com.bhf.aeroncache.near;

public interface NearCacheManagerFactory {

    /**
     * Create a new, empty near cache manager holding values of type {@code V}.
     *
     * @param <V> the near cache value type (for example {@code Object}).
     * @return a fresh {@link NearCacheManager}.
     */
    <V> NearCacheManager<V> createNearCacheManager();
}
