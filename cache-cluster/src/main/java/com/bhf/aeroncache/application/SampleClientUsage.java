package com.bhf.aeroncache.application;

import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;

import java.util.Arrays;

public class SampleClientUsage {
    public static void main(String[] args) {
        final String[] hostnames = System.getProperty(
                "aeron.cache.hostnames", "localhost,localhost,localhost").split(",");
        final String ingressEndpoints = ClusterClientApplication.ingressEndpoints(Arrays.asList(hostnames));

        final ClusterClient client = new ClusterClient();
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

        while(true){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }


    }

    private static void addConsumers(ClusterClient client) {
        client.setCreateCacheConsumer(c->{
            System.out.println("Got consumer callback on cache created with id "+c.getCacheId());
        });

        client.setAddCacheEntryConsumer(c->{
            System.out.println("Got cache entry created consumer callback on cache "+c.getCacheID()+" on key "+c.getEntryKey());
        });

        client.setRemoveCacheEntryConsumer(c->{
            System.out.println("Got cache entry removed consumer callback on cache "+c.getCacheId()+" on key "+c.getKey());
        });

        client.setClearCacheConsumer(c->{
            System.out.println("Got cache cleared consumer callback on cache "+c.getCacheId());
        });

        client.setDeleteCacheConsumer(c->{
            System.out.println("Got cache deleted consumer callback on cache "+c.getCacheId());
        });
    }

    private static void sendMessagesToCache(ClusterClient client, AeronCluster cluster) {
        var cacheId=System.currentTimeMillis();

        System.out.println("Sending request to create cache "+cacheId);
        client.sendCreateCache(cluster, cacheId);
        waitForResult(client, cluster);

        System.out.println("Sending request to add cache entry on cache "+cacheId);
        client.sendAddCacheEntry(cluster, cacheId, "key1", "{msgType: \"D\"}");
        waitForResult(client, cluster);

        System.out.println("Sending request to clear cache on cache "+cacheId);
        client.sendClearCache(cluster, cacheId);
        waitForResult(client, cluster);

        System.out.println("Sending request to delete cache on cache "+cacheId);
        client.sendDeleteCache(cluster, cacheId);
        waitForResult(client, cluster);
    }

    private static void waitForResult(ClusterClient client, AeronCluster cluster) {
        long start=System.currentTimeMillis();
        while(System.currentTimeMillis()<start+1000){
            client.getIdleStrategy().idle(cluster.pollEgress());
        }
    }
}
