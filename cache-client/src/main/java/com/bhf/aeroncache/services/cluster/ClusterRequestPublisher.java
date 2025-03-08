package com.bhf.aeroncache.services.cluster;

import io.aeron.cluster.client.AeronCluster;

/**
 * Encapsulates requests we want to send to the AeronCache cluster.
 */
public interface ClusterRequestPublisher {
    /**
     * Send a message to create a cache instance.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache to create.
     */
    void sendCreateCache(AeronCluster cluster, long cacheId);

    /**
     * Send a message to create a cache instance, block
     * until you get a response.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache to create.
     */
    void sendCreateCacheBlocking(AeronCluster cluster, long cacheId);

    /**
     * Send a message to add a cache entry in a non-blocking manner.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're adding too.
     * @param key     The key to use.
     * @param value   The value to use.
     */
    void addCacheEntry(AeronCluster cluster, long cacheId, String key, String value);

    /**
     * Send a message to add a cache entry and block
     * until you get the result back.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're adding too.
     * @param key     The key to use.
     * @param value   The value to use.
     */
    void addCacheEntryBlocking(AeronCluster cluster, long cacheId, String key, String value);

    /**
     * Send a message to get a cache entry.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're adding too.
     * @param key     The key to use.
     */
    void getCacheEntry(AeronCluster cluster, long cacheId, String key);

    /**
     * Send a message to get a cache entry synchronously.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're adding too.
     * @param key     The key to use.
     */
    void getCacheEntryBlocking(AeronCluster cluster, long cacheId, String key);

    /**
     * Send a message to clear a cache.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're clearing out.
     */
    void clearCache(AeronCluster cluster, long cacheId);

    /**
     * Send a message to clear a cache. Blocks until it gets a response.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're clearing out.
     */
    void clearCacheBlocking(AeronCluster cluster, long cacheId);

    /**
     * Send a message to delete a cache.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're deleting.
     */
    void deleteCache(AeronCluster cluster, long cacheId);

    /**
     * Send a message to delete a cache in a blocking manner.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're deleting.
     */
    void deleteCacheBlocking(AeronCluster cluster, long cacheId);

    /**
     * Send a message to remove a cache entry.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're removing an entry from.
     * @param key     The key of the entry we're removing.
     */
    void removeCacheEntry(AeronCluster cluster, long cacheId, String key);

    /**
     * Send a message to remove a cache entry in a blocking manner.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're removing an entry from.
     * @param key     The key of the entry we're removing.
     */
    void removeCacheEntryBlocking(AeronCluster cluster, long cacheId, String key);

}
