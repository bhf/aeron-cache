package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.results.GetCacheEntryResult;
import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.models.results.RemoveCacheEntryResult;

public interface Cache<I, K, V> {

    /**
     * Add an entry to the cache.
     *
     * @param key   The key of the entry to add.
     * @param value The value of the entry to add.
     * @return The result of adding the entry.
     */
    AddCacheEntryResult<I, K> add(K key, V value);

    /**
     * Get an entry from the cache.
     * @param key The key of the entry to get.
     * @return The result of the get operation.
     */
    GetCacheEntryResult<I,K,V> get(K key);

    /**
     * Remove an entry from the cache.
     *
     * @param key The key of the entry to remove.
     * @return The removed entry.
     */
    RemoveCacheEntryResult<I, K> remove(K key);

    /**
     * Clear all entries from this cache.
     *
     * @return The result of clearing all entries.
     */
    ClearCacheResult<I> clearEntries();
}
