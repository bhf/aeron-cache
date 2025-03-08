package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.models.results.*;
import io.aeron.cluster.client.AeronCluster;

import java.util.function.Consumer;

/**
 * An interface for sending and consuming the results of an AeronCache cluster request via Consumers.
 */
public interface ClusterRequestConsumingPublisher {
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
     * Send a message to get a cache entry in a blocking manner.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're adding too.
     * @param key     The key to use.
     * @param c       The consumer to handle the result.
     */
    void getCacheEntryBlocking(AeronCluster cluster, long cacheId, String key, Consumer<GetCacheEntryResult<Long, String, String>> c);

    /**
     * Send a message to delete a cache in a blocking manner.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're deleting.
     */
    void deleteCacheBlocking(AeronCluster cluster, long cacheId, Consumer<DeleteCacheResult<Long>> consumer);

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
