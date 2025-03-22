package com.bhf.aeroncache.utils;

import io.aeron.cluster.client.AeronCluster;
import io.aeron.cluster.client.EgressListener;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.ErrorHandler;
import org.agrona.concurrent.status.AtomicCounter;

import java.util.List;

/**
 * Helpers for starting and configuring an {@link AeronCluster}.
 */
public class ClusterUtils {

    private static final int PORT_BASE = 9000;
    private static final int PORTS_PER_NODE = 100;
    static final int CLIENT_FACING_PORT_OFFSET = 2;

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
                    calculatePort(i, CLIENT_FACING_PORT_OFFSET));
            sb.append(',');
        }

        sb.setLength(sb.length() - 1);

        return sb.toString();
    }

    public static int calculatePort(final int nodeId, final int offset) {
        return PORT_BASE + (nodeId * PORTS_PER_NODE) + offset;
    }


    /**
     * Build the connection to the cluster.
     *
     * @return An {@link AeronCluster} instance.
     */
    public static AeronCluster buildClusterConnection(String egressIP, String ingressEndpoints, EgressListener client) {
        System.out.println("Building cluster connection...");
        MediaDriver mediaDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()
                .threadingMode(ThreadingMode.SHARED)
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true));
        return AeronCluster.connect(
                new AeronCluster.Context()
                        .egressListener(client)
                        .egressChannel("aeron:udp?endpoint=" + egressIP + ":0")
                        .aeronDirectoryName(mediaDriver.aeronDirectoryName())
                        .ingressChannel("aeron:udp")
                        .ingressEndpoints(ingressEndpoints));
    }

    public static AtomicCounter getAgentErrorCounter(AeronCluster cluster) {
        return cluster.context().aeron().addCounter(1, "AeronCacheAgent");
    }

    public static ErrorHandler getAgentRunnerErrorHandler(AeronCluster cluster) {
        return cluster.context().errorHandler();
    }
}
