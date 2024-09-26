package com.bhf.aeroncache.application;

import com.bhf.aeroncache.services.cluster.ClusterClient;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;

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

    private static void addConsumers(ClusterClient client) {
        client.setCreateCacheConsumer(c -> {
            System.out.println("Got consumer callback on cache created with id " + c.getCacheId());
        });

        client.setAddCacheEntryConsumer(c -> {
            System.out.println("Got cache entry created consumer callback on cache " + c.getCacheID() + " on key " + c.getEntryKey());
        });

        client.setRemoveCacheEntryConsumer(c -> {
            System.out.println("Got cache entry removed consumer callback on cache " + c.getCacheId() + " on key " + c.getKey());
        });

        client.setClearCacheConsumer(c -> {
            System.out.println("Got cache cleared consumer callback on cache " + c.getCacheId());
        });

        client.setDeleteCacheConsumer(c -> {
            System.out.println("Got cache deleted consumer callback on cache " + c.getCacheId());
        });
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
        waitForResult(client, cluster, 1000);

        System.out.println("Sending request to add cache entry on cache " + cacheId);
        client.sendAddCacheEntry(cluster, cacheId, "key1", "{msgType: \"D\"}");
        waitForResult(client, cluster, 1000);

        System.out.println("Sending request to clear cache on cache " + cacheId);
        client.sendClearCache(cluster, cacheId);
        waitForResult(client, cluster, 1000);

        System.out.println("Sending request to delete cache on cache " + cacheId);
        client.sendDeleteCache(cluster, cacheId);
        waitForResult(client, cluster, 1000);
    }

    /**
     * Wait for results back from the cluster.
     *
     * @param client  The cluster client egress listener.
     * @param cluster The Aeron Cluster.
     * @param millis  How long to poll the egress for in milliseconds.
     */
    private static void waitForResult(ClusterClient client, AeronCluster cluster, long millis) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() < start + millis) {
            client.getIdleStrategy().idle(cluster.pollEgress());
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
