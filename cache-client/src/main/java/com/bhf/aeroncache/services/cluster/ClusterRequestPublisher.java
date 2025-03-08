package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.models.results.*;
import io.aeron.cluster.client.AeronCluster;

import java.util.function.Consumer;

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
     * Send a message to create a cache instance, blocking
     * until you get a response. Passes the result to the
     * Consumer.
     *
     * @param cluster  The Aeron Cluster instance to use.
     * @param cacheId  The ID of the cache to create.
     * @param consumer The consumer of the result.
     */
    void sendCreateCacheBlocking(AeronCluster cluster, long cacheId, Consumer<CreateCacheResult<Long>> consumer);

    /**
     * Send a message to add a cache entry in a non-blocking manner.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're adding too.
     * @param key     The key to use.
     * @param value   The value to use.
     */
    void addCacheEntryNonBlocking(AeronCluster cluster, long cacheId, String key, String value);

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
     * Send a message to add a cache entry in a blocking manner.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're adding too.
     * @param key     The key to use.
     * @param value   The value to use.
     * @param c       The consumer that will handle the result.
     */
    void addCacheEntryBlocking(AeronCluster cluster, long cacheId, String key, String value, Consumer<AddCacheEntryResult<Long, String>> c);

    /**
     * Send a message to get a cache entry.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're adding too.
     * @param key     The key to use.
     */
    void getCacheEntryNonBlocking(AeronCluster cluster, long cacheId, String key);

    /**
     * Send a message to get a cache entry in a blocking manner.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're adding too.
     * @param key     The key to use.
     * @param c       The consumer to handle the result.
     */
    void getCacheEntryBlocking(AeronCluster cluster, long cacheId, String key, Consumer<GetCacheEntryResult<Long, String, String>> c);

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
    void clearCacheNonBlocking(AeronCluster cluster, long cacheId);

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
    void deleteCacheNonBlocking(AeronCluster cluster, long cacheId);

    /**
     * Send a message to delete a cache in a blocking manner.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're deleting.
     */
    void deleteCacheBlocking(AeronCluster cluster, long cacheId);

    /**
     * Send a message to delete a cache in a blocking manner.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're deleting.
     */
    void deleteCacheBlocking(AeronCluster cluster, long cacheId, Consumer<DeleteCacheResult<Long>> consumer);

    /**
     * Send a message to remove a cache entry.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're removing an entry from.
     * @param key     The key of the entry we're removing.
     */
    void removeCacheEntryNonBlocking(AeronCluster cluster, long cacheId, String key);

    /**
     * Send a message to remove a cache entry in a blocking manner.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're removing an entry from.
     * @param key     The key of the entry we're removing.
     */
    void removeCacheEntryBlocking(AeronCluster cluster, long cacheId, String key);

    /**
     * Send a message to remove a cache entry in a blocking manner.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're removing an entry from.
     * @param key     The key of the entry we're removing.
     * @param c       The consumer that will handle the result.
     */
    void removeCacheEntryBlocking(AeronCluster cluster, long cacheId, String key, Consumer<RemoveCacheEntryResult<Long, String>> c);
}
