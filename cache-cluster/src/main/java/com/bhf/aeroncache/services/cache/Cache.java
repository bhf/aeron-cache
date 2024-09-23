package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.models.results.RemoveCacheEntryResult;

public interface Cache<K,V> {

    /**
     * Add an entry to the cache.
     * @param key The key of the entry to add.
     * @param value The value of the entry to add.
     * @return The result of adding the entry.
     */
    AddCacheEntryResult<K> add(K key, V value);

    /**
     * Remove an entry from the cache.
     * @param key The key of the entry to remove.
     * @return The removed entry.
     */
    RemoveCacheEntryResult<K> remove(K key);

    /**
     * Clear all entries from this cache.
     * @return The result of clearing all entries.
     */
    ClearCacheResult clearEntries();
}
