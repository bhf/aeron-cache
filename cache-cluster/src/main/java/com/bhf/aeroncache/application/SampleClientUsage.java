package com.bhf.aeroncache.application;

import com.bhf.aeroncache.services.cluster.ClusterClient;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.concurrent.IdleStrategy;

import java.util.Arrays;
import java.util.List;

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

    static void addConsumers(ClusterClient client) {
        client
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
     * @param client  The cluster client egress listener.
     * @param cluster The Aeron Cluster.
     */
    private static void sendMessagesToCache(ClusterClient client, AeronCluster cluster) {
        var cacheId = System.currentTimeMillis();

        System.out.println("Sending request to create cache " + cacheId);
        client.sendCreateCache(cluster, cacheId);
        waitForResult(client, cluster);

        System.out.println("Sending request to add cache entry on cache " + cacheId);
        client.sendAddCacheEntry(cluster, cacheId, "key1", "{msgType: \"D\"}");
        waitForResult(client, cluster);

        System.out.println("Sending request to get cache entry on cache " + cacheId);
        client.sendGetCacheEntry(cluster, cacheId, "key1");
        waitForResult(client, cluster);

        System.out.println("Sending request to remove cache entry on cache " + cacheId + " with key: key1");
        client.removeCacheEntry(cluster, cacheId, "key1");
        waitForResult(client, cluster);

        System.out.println("Sending request to get cache entry on cache " + cacheId);
        client.sendGetCacheEntry(cluster, cacheId, "key1");
        waitForResult(client, cluster);

        System.out.println("Sending request to clear cache on cache " + cacheId);
        client.sendClearCache(cluster, cacheId);
        waitForResult(client, cluster);

        System.out.println("Sending request to delete cache on cache " + cacheId);
        client.sendDeleteCache(cluster, cacheId);
        waitForResult(client, cluster);
    }

    /**
     * Wait for results back from the cluster.
     *
     * @param client  The cluster client egress listener.
     * @param cluster The Aeron Cluster.
     */
    private static void waitForResult(ClusterClient client, AeronCluster cluster) {
        pollEgressUntilMessage(client.getIdleStrategy(), cluster);
    }

    /**
     * Poll the egress of the cluster.
     *
     * @param cluster The cluster to poll.
     * @return Number of fragments processed.
     */
    static int pollEgress(AeronCluster cluster) {
        return null == cluster ? 0 : cluster.pollEgress();
    }

    /**
     * Keep polling the egress till we get a message.
     *
     * @param idleStrategy The idle strategy to use.
     * @param cluster      The cluster to poll.
     */
    static void pollEgressUntilMessage(IdleStrategy idleStrategy, AeronCluster cluster) {
        idleStrategy.reset();
        while (pollEgress(cluster) <= 0) {
            idleStrategy.idle();
        }
    }

    public static void main(String[] args) {
        final String[] hostnames = System.getProperty(
                "aeron.cache.hostnames", "localhost,localhost,localhost").split(",");
        final var ingressEndpoints = ingressEndpoints(Arrays.asList(hostnames));

        final var client = new ClusterClient();
        addConsumers(client);

        try (
                MediaDriver mediaDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()
                        .threadingMode(ThreadingMode.SHARED)
                        .dirDeleteOnStart(true)
                        .dirDeleteOnShutdown(true));
                AeronCluster aeronCluster = AeronCluster.connect(
                        new AeronCluster.Context()
                                .egressListener(client)
                                .egressChannel("aeron:udp?endpoint=localhost:0")
                                .aeronDirectoryName(mediaDriver.aeronDirectoryName())
                                .ingressChannel("aeron:udp")
                                .ingressEndpoints(ingressEndpoints))) {

            sendMessagesToCache(client, aeronCluster);
        }
    }
}
