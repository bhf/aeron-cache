package com.bhf.aeroncache.utils;

import io.aeron.cluster.client.AeronCluster;
import io.aeron.cluster.client.EgressListener;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.ErrorHandler;
import org.agrona.SystemUtil;
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
     * System property used to configure the Aeron term buffer length (in bytes). Accepts human
     * readable sizes such as {@code 64k}, {@code 16m} or {@code 128m}. Must be a power of two
     * between 64k and 1g. The maximum Aeron message length is {@code termLength / 8}, so a 128m
     * term buffer is required to carry ~16MB payloads.
     */
    public static final String TERM_LENGTH_PROP = "aeron.cache.term.length";
    /**
     * Environment variable equivalent of {@link #TERM_LENGTH_PROP}.
     */
    public static final String TERM_LENGTH_ENV = "AERON_CACHE_TERM_LENGTH";

    /**
     * Resolve the configured Aeron term buffer length.
     *
     * @param defaultTermLength value to return when neither the {@link #TERM_LENGTH_PROP} system
     *                          property nor the {@link #TERM_LENGTH_ENV} environment variable is set.
     * @return the configured term buffer length in bytes, or {@code defaultTermLength} when unset.
     */
    public static int getConfiguredTermLength(int defaultTermLength) {
        String value = System.getProperty(TERM_LENGTH_PROP);
        if (value == null || value.isBlank()) {
            value = System.getenv(TERM_LENGTH_ENV);
        }
        if (value == null || value.isBlank()) {
            return defaultTermLength;
        }
        return (int) SystemUtil.parseSize(TERM_LENGTH_PROP, value.trim());
    }

    /**
     * Apply the configured term buffer length to a media driver context, leaving Aeron defaults in
     * place when nothing is configured.
     *
     * @param context the media driver context to configure.
     * @return the same context for chaining.
     */
    public static MediaDriver.Context applyConfiguredTermLength(MediaDriver.Context context) {
        int configured = getConfiguredTermLength(-1);
        if (configured > 0) {
            context.publicationTermBufferLength(configured)
                    .ipcTermBufferLength(configured)
                    .termBufferSparseFile(true);
        }
        return context;
    }

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
     * Create the {@link AeronCluster.Context} for connecting to a cluster.
     */
    public static AeronCluster.Context createClusterContext(String egressIP, String ingressEndpoints, EgressListener client, String alias, String aeronDirectory) {
        return new AeronCluster.Context()
                .egressListener(client)
                .egressChannel("aeron:udp?endpoint=" + egressIP + ":0|alias=" + alias + "-ClusterEgress")
                .aeronDirectoryName(aeronDirectory)
                .ingressChannel("aeron:udp")
                .ingressEndpoints(ingressEndpoints);
    }

    /**
     * Build the connection to the cluster.
     *
     * @return An {@link AeronCluster} instance.
     */
    public static AeronCluster buildClusterConnection(String egressIP, String ingressEndpoints, EgressListener client, String alias) {
        System.out.println("Building cluster connection...");
        MediaDriver mediaDriver = launchEmbeddedMediaDriver();
        return buildClusterConnection(egressIP, ingressEndpoints, client, alias, mediaDriver);
    }

    public static MediaDriver launchEmbeddedMediaDriver() {
        return MediaDriver.launchEmbedded(applyConfiguredTermLength(new MediaDriver.Context()
                .threadingMode(ThreadingMode.SHARED)
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true)));
    }

    public static MediaDriver launchEmbeddedMediaDriver(String aeronDirectory) {
        return MediaDriver.launchEmbedded(applyConfiguredTermLength(new MediaDriver.Context().aeronDirectoryName(aeronDirectory)
                .threadingMode(ThreadingMode.SHARED)
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true)));
    }

    public static AeronCluster buildClusterConnection(String egressIP, String ingressEndpoints, EgressListener client, String alias, MediaDriver mediaDriver) {
        return buildClusterConnection(egressIP, ingressEndpoints, client, alias, mediaDriver != null ? mediaDriver.aeronDirectoryName() : null);
    }

    public static AeronCluster buildClusterConnection(String egressIP, String ingressEndpoints, EgressListener client, String alias, String aeronDirectory) {
        return AeronCluster.connect(createClusterContext(egressIP, ingressEndpoints, client, alias, aeronDirectory));
    }

    /**
     * Closes the cluster connection safely.
     *
     * @param cluster the cluster to close.
     */
    public static void close(AeronCluster cluster) {
        if (cluster != null) {
            cluster.close();
        }
    }

    public static AtomicCounter getAgentErrorCounter(AeronCluster cluster, String alias) {
        return cluster.context().aeron().addCounter(1, "AeronCacheAgent-"+alias);
    }

    public static ErrorHandler getAgentRunnerErrorHandler(AeronCluster cluster) {
        return cluster.context().errorHandler();
    }

}
