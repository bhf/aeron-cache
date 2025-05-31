package com.bhf.aeroncache.application;

import com.bhf.aeroncache.services.cluster.AeronCacheListener;
import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;
import com.bhf.aeroncache.services.cluster.impl.ObservingClusterRequestPublisher;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;

import java.util.*;

import static com.bhf.aeroncache.application.ClusterNodeApplication.calculatePort;

/**
 * A basic request response perf test.
 */
public class BasicPerfTest {

    static long lastSent = 0;
    static int count = 0;
    static List<Long> samples = new ArrayList<>();

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
                .onAddCacheEntry(c -> {
                    long now = System.currentTimeMillis();
                    long dur = now - lastSent;
                    count++;
                    samples.add(dur);
                });
    }

    static int c = 0;
    /**
     * Send messages to the cache cluster.
     *
     * @param client       The cluster client egress listener.
     * @param cluster      The Aeron Cluster.
     * @param publisher    The publisher to use.
     * @param cacheId      The ID of the cache we're testing against.
     * @param payloadValue
     */
    private static void sendMessagesToCache(AeronCacheListener client, AeronCluster cluster, ObservingClusterRequestPublisher publisher, int cacheId, String payloadValue) {
        var ts = System.currentTimeMillis();
        var requestId = String.valueOf(c++);
        lastSent = ts;
        publisher.addCacheEntryBlocking(cluster, requestId, cacheId, "key1", payloadValue);
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

        Map<Integer, StringBuilder> payloadSizeToDistro = new TreeMap<>();

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

            var cacheId = 808;
            System.out.println("Sending request to create cache " + cacheId);
            observingPublisher.sendCreateCacheBlocking(aeronCluster, UUID.randomUUID().toString(), cacheId);

            var totalToSend = 10_000;
            var payloadSizes = new Integer[]{5};
            //var payloadSizes = new Integer[]{5, 10, 25, 50, 100, 200, 400, 1000};
            //var payloadSizes = new Integer[]{2000, 5000, 10000};
            //var payloadSizes = new Integer[]{20000, 50000, 100000};

            for (int payloadSize : payloadSizes) {
                var payloadValue = getPayloadValue(payloadSize);
                System.out.println("Using payload value "+payloadValue);

                while (count < totalToSend) {
                    sendMessagesToCache(client, aeronCluster, observingPublisher, cacheId, payloadValue);
                }

                double tally = 0;
                for (Long time : samples) {
                    tally += time;
                }

                Collections.sort(samples);
                var mean = tally / (double) samples.size();

                StringBuilder sb = new StringBuilder();

                for (int i = 0; i <= 10; i++) {
                    var index = i > 0 ? (i * 1000) - 1 : 0;
                    var pctileValue = samples.get(index);
                    sb.append(pctileValue).append(",");
                }

                sb.append(mean);
                payloadSizeToDistro.put(payloadSize, sb);

                lastSent=0;
                count=0;
                samples.clear();
            }
        }
        System.out.println("FINISHED TEST");
        outputByPayloadSize(payloadSizeToDistro);
    }

    private static void outputByPayloadSize(Map<Integer, StringBuilder> payloadSizeToDistro) {
        payloadSizeToDistro.forEach((payloadSize, distro) -> System.out.println(payloadSize + "," + distro.toString()));
    }

    private static String getPayloadValue(int payloadSize) {
        return "$".repeat(Math.max(0, payloadSize));
    }
}
