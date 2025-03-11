package com.bhf.aeroncache.application;

import com.bhf.aeroncache.services.cluster.AeronCacheListener;
import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;
import com.bhf.aeroncache.services.cluster.impl.ObservingClusterRequestPublisher;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.bhf.aeroncache.application.ClusterNodeApplication.calculatePort;

/**
 * A sample of how to use the cache client to create a cache, put an entry into it, get the entry,
 * remove the entry, clear the cache and finally delete the cache.
 */
public class SampleClientUsage {

    /**
     * Ingress endpoints generated from a list of hostnames.
     *
     * @param hostnames for the cluster members.
     * @return a formatted string of ingress endpoints for connecting to a cluster.
     */
    public static String ingressEndpoints(final List<String> hostnames) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hostnames.size(); i++) {
            sb.append(i).append('=');
            sb.append(hostnames.get(i)).append(':').append(
                    calculatePort(i, ClusterNodeApplication.CLIENT_FACING_PORT_OFFSET));
            sb.append(',');
        }

        sb.setLength(sb.length() - 1);

        return sb.toString();
    }

    static void addConsumers(AeronCacheListener client, ObservingClusterRequestPublisher observingPublisher) {
        observingPublisher
                .onCreateCache(c -> System.out.println("Cache created with id " + c.getCacheId()))
                .onAddCacheEntry(c -> System.out.println("Cache entry created cache " + c.getCacheID()))
                .onRemoveCacheEntry(c -> System.out.println("Cache entry removed on cache " + c.getCacheId()))
                .onClearCache(c -> System.out.println("Cache cleared on cache " + c.getCacheId()))
                .onDeleteCache(c -> System.out.println("Cache deleted on cache " + c.getCacheId()))
                .onGetCacheEntry(c -> System.out.println("Cache entry GET on cache " + c.getCacheId()));
    }

    /**
     * Send messages to the cache cluster.
     *
     * @param client    The cluster client egress listener.
     * @param cluster   The Aeron Cluster.
     * @param publisher
     */
    private static void sendMessagesToCache(AeronCacheListener client, AeronCluster cluster, ObservingClusterRequestPublisher publisher) {
        var cacheId = System.currentTimeMillis();

        System.out.println("Sending request to create cache " + cacheId);
        publisher.sendCreateCacheBlocking(cluster, UUID.randomUUID().toString(), cacheId);

        System.out.println("Sending request to add cache entry on cache " + cacheId);
        publisher.addCacheEntryBlocking(cluster, UUID.randomUUID().toString(), cacheId, "key1", "{msgType: \"D\"}");

        System.out.println("Sending request to get cache entry on cache " + cacheId);
        publisher.getCacheEntryBlocking(cluster, UUID.randomUUID().toString(), cacheId, "key1");

        System.out.println("Sending request to remove cache entry on cache " + cacheId + " with key: key1");
        publisher.removeCacheEntryBlocking(cluster, UUID.randomUUID().toString(), cacheId, "key1");

        System.out.println("Sending request to get cache entry on cache " + cacheId);
        publisher.getCacheEntryBlocking(cluster, UUID.randomUUID().toString(), cacheId, "key1");

        System.out.println("Sending request to clear cache on cache " + cacheId);
        publisher.clearCacheBlocking(cluster, UUID.randomUUID().toString(), cacheId);

        System.out.println("Sending request to delete cache on cache " + cacheId);
        publisher.deleteCacheBlocking(cluster, UUID.randomUUID().toString(), cacheId);
    }

    public static void main(String[] args) {
        final String[] hostnames = System.getenv("CLUSTER_ADDRESSES").split(",");
        final String egressIP = System.getenv("EGRESS_IP");
        System.out.println("HOSTNAMES: " + Arrays.toString(hostnames));
        System.out.println("EGRESS_IP: " + egressIP);
        final var ingressEndpoints = ingressEndpoints(Arrays.asList(hostnames));

        final var client = new AeronCacheListener();
        var observingPublisher = new ObservingClusterRequestPublisher(new ClusterMessagePublisher());
        client.setCacheResultsCallbacks(observingPublisher);
        addConsumers(client, observingPublisher);

        try (
                MediaDriver mediaDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()
                        .threadingMode(ThreadingMode.SHARED)
                        .dirDeleteOnStart(true)
                        .dirDeleteOnShutdown(true));
                AeronCluster aeronCluster = AeronCluster.connect(
                        new AeronCluster.Context()
                                .egressListener(client)
                                .egressChannel("aeron:udp?endpoint=" + egressIP + ":0")
                                .aeronDirectoryName(mediaDriver.aeronDirectoryName())
                                .ingressChannel("aeron:udp")
                                .ingressEndpoints(ingressEndpoints))) {

            while (true) {
                sendMessagesToCache(client, aeronCluster, observingPublisher);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }
}
