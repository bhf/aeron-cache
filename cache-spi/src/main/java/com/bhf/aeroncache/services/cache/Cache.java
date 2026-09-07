package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;
import io.aeron.ExclusivePublication;
import org.agrona.DirectBuffer;

import java.util.Map;

public interface Cache<I extends Reusable, K extends Reusable, V extends Reusable> {

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
     * Patch the value of an existing entry. The current value and the supplied
     * patch are both treated as JSON, and the patch is merged into the current
     * value.
     *
     * @param key   The key of the entry to patch.
     * @param patch The JSON patch to merge into the current value.
     * @return The result of patching the value, including a reference to the updated value.
     */
    PatchValueResult<I, K, V> patchValue(K key, V patch);

    /**
     * Clear all entries from this cache.
     *
     * @return The result of clearing all entries.
     */
    ClearCacheResult<I> clearEntries();

    /**
     * Get all the entries.
     * @return A {@link Map} of all entries in this cache.
     */
    Map<K, V> getAllEntries();

    /**
     * Get stats about this cache.
     * @return A {@link CacheStats} reusable.
     */
    CacheStats<I> getCacheStats();

    /**
     * Take a snapshot of this cache.
     * @param snapshotPublication
     * @param cacheId
     */
    void takeSnapshot(ExclusivePublication snapshotPublication, I cacheId);

    /**
     * Load a snapshot into this cache.
     * @param buffer
     * @param offset
     */
    void loadSnapshot(DirectBuffer buffer, int offset);
}
