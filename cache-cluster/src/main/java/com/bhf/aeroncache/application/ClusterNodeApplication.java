package com.bhf.aeroncache.application;

import com.bhf.aeroncache.services.cluster.SBEDecodingCacheClusterService;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.CommonContext;
import io.aeron.archive.Archive;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.archive.client.AeronArchive;
import io.aeron.cluster.ClusteredMediaDriver;
import io.aeron.cluster.ConsensusModule;
import io.aeron.cluster.service.ClusteredServiceContainer;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.MinMulticastFlowControlSupplier;
import io.aeron.driver.ThreadingMode;
import lombok.extern.log4j.Log4j2;
import org.agrona.ErrorHandler;
import org.agrona.concurrent.NoOpLock;
import org.agrona.concurrent.ShutdownSignalBarrier;
import org.agrona.concurrent.SystemEpochClock;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import static java.lang.Integer.parseInt;

/**
 * Launch a single node that runs the
 * {@link SBEDecodingCacheClusterService}.
 */
@Log4j2
public class ClusterNodeApplication {
    private static ErrorHandler errorHandler(final String context) {
        return
                (Throwable throwable) ->
                {
                    System.err.println(context);
                    throwable.printStackTrace(System.err);
                };
    }

    private static final int PORT_BASE = 9000;
    private static final int PORTS_PER_NODE = 100;
    private static final int ARCHIVE_CONTROL_PORT_OFFSET = 1;
    static final int CLIENT_FACING_PORT_OFFSET = 2;
    private static final int MEMBER_FACING_PORT_OFFSET = 3;
    private static final int LOG_PORT_OFFSET = 4;
    private static final int TRANSFER_PORT_OFFSET = 5;
    private static final int LOG_CONTROL_PORT_OFFSET = 6;
    private static final int TERM_LENGTH = 64 * 1024;

    static int calculatePort(final int nodeId, final int offset) {
        return PORT_BASE + (nodeId * PORTS_PER_NODE) + offset;
    }

    private static String udpChannel(final int nodeId, final String hostname, final int portOffset) {
        final int port = calculatePort(nodeId, portOffset);
        return new ChannelUriStringBuilder()
                .media("udp")
                .termLength(TERM_LENGTH)
                .endpoint(hostname + ":" + port)
                .build();
    }


    private static String logControlChannel(final int nodeId, final String hostname, final int portOffset) {
        final int port = calculatePort(nodeId, portOffset);
        return new ChannelUriStringBuilder()
                .media("udp")
                .termLength(TERM_LENGTH)
                .controlMode(CommonContext.MDC_CONTROL_MODE_MANUAL)
                .controlEndpoint(hostname + ":" + port)
                .build();
    }

    private static String logReplicationChannel(final String hostname) {
        return new ChannelUriStringBuilder()
                .media("udp")
                .endpoint(hostname + ":0")
                .build();
    }

    private static String clusterMembers(final List<String> hostnames) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hostnames.size(); i++) {
            sb.append(i);
            sb.append(',').append(hostnames.get(i)).append(':').append(calculatePort(i, CLIENT_FACING_PORT_OFFSET));
            sb.append(',').append(hostnames.get(i)).append(':').append(calculatePort(i, MEMBER_FACING_PORT_OFFSET));
            sb.append(',').append(hostnames.get(i)).append(':').append(calculatePort(i, LOG_PORT_OFFSET));
            sb.append(',').append(hostnames.get(i)).append(':').append(calculatePort(i, TRANSFER_PORT_OFFSET));
            sb.append(',').append(hostnames.get(i)).append(':')
                    .append(calculatePort(i, ARCHIVE_CONTROL_PORT_OFFSET));
            sb.append('|');
        }

        return sb.toString();
    }

    /**
     * Main method for launching the process.
     *
     * @param args passed to the process.
     */
    public static void main(final String[] args) {
        int nodeId = -1;
        String[] hostnames = null;

        System.out.println("Launching AeronCache Cluster Node");

        try {
            var clusterNode = System.getenv("CLUSTER_NODE");
            var allHosts = System.getenv("CLUSTER_ADDRESSES");
            System.out.println("CLUSTER_NODE=" + clusterNode);
            System.out.println("CLUSTER_ADDRESSES=" + allHosts);
            nodeId = parseInt(clusterNode);
            hostnames = allHosts.split(",");
            System.out.println("Using cluster nodeId: " + clusterNode + ", cluster addresses: " + Arrays.toString(hostnames));
        } catch (Exception e) {
        }
        try {
            var allHosts = System.getenv("CLUSTER_ADDRESSES");
            var podName = System.getenv("POD_NAME");
            System.out.println("CLUSTER_ADDRESSES=" + allHosts);
            System.out.println("POD_NAME=" + podName);
            var podSplit = podName.split("-");
            nodeId = parseInt(podSplit[podSplit.length - 1]);
            hostnames = allHosts.split(",");
            System.out.println("Using pod name: " + podName + ", nodeId: " + nodeId + " cluster addresses: " + Arrays.toString(hostnames));
        } catch (Exception e) {
        }

        final String hostname = hostnames[nodeId];
        System.out.println("This node's hostname:" + hostname);
        final File baseDir = new File(System.getProperty("user.dir"), "node" + nodeId);
        final String aeronDirName = CommonContext.getAeronDirectoryName() + "-" + nodeId + "-driver";
        System.out.println("user.dir=" + baseDir.getAbsolutePath());
        System.out.println("AeronDirName=" + aeronDirName);

        final ShutdownSignalBarrier barrier = new ShutdownSignalBarrier();

        final MediaDriver.Context mediaDriverContext = new MediaDriver.Context()
                .aeronDirectoryName(aeronDirName)
                .threadingMode(ThreadingMode.SHARED)
                .termBufferSparseFile(true)
                .multicastFlowControlSupplier(new MinMulticastFlowControlSupplier())
                .terminationHook(barrier::signal)
                .errorHandler(ClusterNodeApplication.errorHandler("Media Driver"));

        final AeronArchive.Context replicationArchiveContext = new AeronArchive.Context()
                .controlResponseChannel("aeron:udp?endpoint=" + hostname + ":0");

        final Archive.Context archiveContext = new Archive.Context()
                .aeronDirectoryName(aeronDirName)
                .archiveDir(new File(baseDir, "archive"))
                .controlChannel(udpChannel(nodeId, hostname, ARCHIVE_CONTROL_PORT_OFFSET))
                .archiveClientContext(replicationArchiveContext)
                .localControlChannel("aeron:ipc?term-length=64k")
                .recordingEventsEnabled(false)
                .threadingMode(ArchiveThreadingMode.SHARED)
                .replicationChannel("aeron:udp?endpoint=" + hostname + ":0");

        final AeronArchive.Context aeronArchiveContext = new AeronArchive.Context()
                .lock(NoOpLock.INSTANCE)
                .controlRequestChannel(archiveContext.localControlChannel())
                .controlResponseChannel(archiveContext.localControlChannel())
                .aeronDirectoryName(aeronDirName);

        final ConsensusModule.Context consensusModuleContext = new ConsensusModule.Context()
                .errorHandler(errorHandler("Consensus Module"))
                .clusterMemberId(nodeId)
                .clusterMembers(clusterMembers(Arrays.asList(hostnames)))
                .clusterDir(new File(baseDir, "cluster"))
                .ingressChannel("aeron:udp?term-length=64k")
                .replicationChannel(logReplicationChannel(hostname))
                .archiveContext(aeronArchiveContext.clone());

        final ClusteredServiceContainer.Context clusteredServiceContext =
                new ClusteredServiceContainer.Context()
                        .aeronDirectoryName(aeronDirName)
                        .archiveContext(aeronArchiveContext.clone())
                        .clusterDir(new File(baseDir, "cluster"))
                        .clusteredService(new SBEDecodingCacheClusterService())
                        .errorHandler(errorHandler("Clustered Service"));

        System.out.println("Awaiting DNS Resolution");
        final List<String> hostAddresses = List.of(hostnames);

        for (int i = 0; i < hostAddresses.size(); i++) {
            awaitDnsResolution(hostAddresses, i);
        }

        System.out.println("Launching cluster node now...");

        try (
                ClusteredMediaDriver clusteredMediaDriver = ClusteredMediaDriver.launch(
                        mediaDriverContext, archiveContext, consensusModuleContext);
                ClusteredServiceContainer container = ClusteredServiceContainer.launch(
                        clusteredServiceContext)) {
            System.out.println("[" + nodeId + "] Started Cluster Node on " + hostname + "...");
            barrier.await();
            System.out.println("[" + nodeId + "] Exiting");
        }
    }

    private static void awaitDnsResolution(final List<String> hostArray, final int nodeId) {
        if (applyDnsDelay()) {
            System.out.println("Waiting 5 seconds for DNS to be registered...");
            quietSleep(5000);
        }

        final long endTime = SystemEpochClock.INSTANCE.time() + 60000;
        final String nodeName = hostArray.get(nodeId);
        java.security.Security.setProperty("networkaddress.cache.ttl", "0");

        boolean resolved = false;
        while (!resolved) {
            if (SystemEpochClock.INSTANCE.time() > endTime) {
                System.out.println("cannot resolve name "+nodeName+", exiting");
                System.exit(-1);
            }

            try {
                var res = InetAddress.getByName(nodeName);
                resolved = true;
                System.out.println("Resolved "+nodeName+" to "+res);
            } catch (final UnknownHostException e) {
                System.out.println("cannot yet resolve name "+nodeName+", retrying in 3 seconds");
                quietSleep(3000);
            }
        }
    }

    /**
     * Sleeps for the given number of milliseconds, ignoring any interrupts.
     *
     * @param millis the number of milliseconds to sleep.
     */
    private static void quietSleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException ex) {
            System.out.println("Interrupted while sleeping");
        }
    }

    /**
     * Apply DNS delay
     *
     * @return true if DNS delay should be applied
     */
    private static boolean applyDnsDelay() {
        final String dnsDelay = System.getenv("DNS_DELAY");
        if (null == dnsDelay || dnsDelay.isEmpty()) {
            return false;
        }
        return Boolean.parseBoolean(dnsDelay);
    }
}
