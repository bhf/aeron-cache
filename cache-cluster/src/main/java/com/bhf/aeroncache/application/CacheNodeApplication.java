package com.bhf.aeroncache.application;

import com.bhf.aeroncache.application.ephemeral.EphemeralCacheApplication;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.cluster.SBEDecodingCacheClusterService;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import com.bhf.aeroncache.services.tracing.impl.NoOpTracingService;
import com.bhf.aeroncache.services.tracing.impl.OtelTracingService;
import com.bhf.aeroncache.utils.DNSUtils;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.CommonContext;
import io.aeron.archive.Archive;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.archive.client.AeronArchive;
import io.aeron.cluster.ConsensusModule;
import io.aeron.cluster.service.ClusteredServiceContainer;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.MinMulticastFlowControlSupplier;
import io.aeron.driver.ThreadingMode;
import lombok.extern.log4j.Log4j2;
import org.agrona.ErrorHandler;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.NoOpLock;
import org.agrona.concurrent.ShutdownSignalBarrier;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import static java.lang.Integer.parseInt;

/**
 * Launch a single node that runs the
 * {@link SBEDecodingCacheClusterService}.
 */
@Log4j2
public class CacheNodeApplication {
    private static ErrorHandler errorHandler(final String context) {
        return
                (Throwable throwable) ->
                {
                    System.err.println(context);
                    throwable.printStackTrace(System.err);
                };
    }

    private static final int PORT_BASE = 9000;
    private static final int PORTS_PER_NODE = 20;
    private static final int ARCHIVE_CONTROL_PORT_OFFSET = 1;
    static final int CLIENT_FACING_PORT_OFFSET = 2;
    private static final int MEMBER_FACING_PORT_OFFSET = 3;
    private static final int LOG_PORT_OFFSET = 4;
    private static final int TRANSFER_PORT_OFFSET = 5;
    private static final int LOG_CONTROL_PORT_OFFSET = 6;
    private static final int TERM_LENGTH = 64 * 1024;
    private static final boolean USE_BUSY_SPIN_IDLE_FOR_CLUSTER_SERVICE = false;
    private static final long RESTART_ATTEMPT_INTERVAL = 10000;

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
        var baseDirectory = System.getenv("CACHE_DATA_DIR");
        if (baseDirectory == null || baseDirectory.isEmpty()) {
            baseDirectory = System.getProperty("user.dir");
        }
        System.out.println("Base directory for data: " + baseDirectory);
        startAeronCacheApplication(args, baseDirectory);
    }

    public static void startAeronCacheApplication(String[] args, String baseDirectory) {
        var cacheMode = System.getenv("CACHE_MODE");
        final boolean CLUSTERED_MODE = cacheMode == null || cacheMode.toUpperCase().equals("RAFT");

        if (!CLUSTERED_MODE) {
            System.out.println("Starting Aeron Cache server in non-clustered mode");
            EphemeralCacheApplication.main(new String[]{});
        } else {
            System.out.println("Starting Aeron Cache server node in clustered mode");
            var clusterNode = System.getenv("CLUSTER_NODE");

            if (clusterNode == null || clusterNode.trim().isEmpty()) {
                clusterNode = System.getenv("POD_NAME");
                if (clusterNode != null && clusterNode.contains("-")) {
                    clusterNode = clusterNode.substring(clusterNode.lastIndexOf("-") + 1);
                }
            }

            if (clusterNode == null || clusterNode.trim().isEmpty()) {
                if (args != null && args.length > 0) {
                    clusterNode = args[0];
                }
            }

            int nodeId = Integer.parseInt(clusterNode);
            try {
                startClusteredMode(nodeId, baseDirectory);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Will try to restart clustered cache");
                try {
                    Thread.sleep(RESTART_ATTEMPT_INTERVAL);
                } catch (InterruptedException ex) {
                }
                startAeronCacheApplication(args, baseDirectory);
            }
        }
    }

    private static void startClusteredMode(int nodeId_, String baseDirectory) {
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

        if (hostnames == null) {
            nodeId = nodeId_;
            hostnames = new String[]{"localhost"};
        }

        final String hostname = hostnames[nodeId];
        System.out.println("This node's hostname:" + hostname);
        final File baseDir = new File(baseDirectory, "node" + nodeId);
        final String aeronDirName = CommonContext.getAeronDirectoryName() + "-" + nodeId + "-driver";
        System.out.println("user.dir=" + baseDir.getAbsolutePath());
        System.out.println("AeronDirName=" + aeronDirName);

        final AeronArchive.Context replicationArchiveContext = new AeronArchive.Context()
                .controlResponseChannel("aeron:udp?endpoint=" + hostname + ":0|alias=AeronCache-Archive" +
                        "-ControlResponse-" + nodeId);

        final Archive.Context archiveContext = new Archive.Context()
                .aeronDirectoryName(aeronDirName)
                .archiveDir(new File(baseDir, "archive"))
                .controlChannel(udpChannel(nodeId, hostname, ARCHIVE_CONTROL_PORT_OFFSET))
                .archiveClientContext(replicationArchiveContext)
                .localControlChannel("aeron:ipc?term-length=64k|alias=AeronCache-Archive-LocalControl")
                .recordingEventsEnabled(false)
                .threadingMode(ArchiveThreadingMode.SHARED)
                .replicationChannel("aeron:udp?endpoint=" + hostname + ":0|alias=AeronCache-Archive-Replication-" + nodeId);

        final AeronArchive.Context aeronArchiveContext = new AeronArchive.Context()
                .lock(NoOpLock.INSTANCE)
                .controlRequestChannel(archiveContext.localControlChannel())
                .controlResponseChannel(archiveContext.localControlChannel())
                .aeronDirectoryName(aeronDirName);

        final ConsensusModule.Context consensusModuleContext = new ConsensusModule.Context()
                .errorHandler(errorHandler("Consensus Module"))
                .aeronDirectoryName(aeronDirName)
                .clusterMemberId(nodeId)
                .clusterMembers(clusterMembers(Arrays.asList(hostnames)))
                .clusterDir(new File(baseDir, "cluster"))
                .ingressChannel("aeron:udp?term-length=64k|alias=AeronCache-Concensus-Ingress-" + nodeId)
                .replicationChannel(logReplicationChannel(hostname))
                .archiveContext(aeronArchiveContext.clone());

        final var cacheManagerFactory = getCacheManagerFactory();
        final var cacheService = getSbeDecodingCacheClusterService(nodeId, cacheManagerFactory);

        final ClusteredServiceContainer.Context clusteredServiceContext =
                new ClusteredServiceContainer.Context()
                        .aeronDirectoryName(aeronDirName)
                        .archiveContext(aeronArchiveContext.clone())
                        .clusterDir(new File(baseDir, "cluster"))
                        .clusteredService(cacheService)
                        .errorHandler(errorHandler("Clustered Service"));

        if (USE_BUSY_SPIN_IDLE_FOR_CLUSTER_SERVICE) {
            clusteredServiceContext.idleStrategySupplier(BusySpinIdleStrategy::new);
        }

        System.out.println("Awaiting DNS Resolution");
        final List<String> hostAddresses = List.of(hostnames);

        for (int i = 0; i < hostAddresses.size(); i++) {
            DNSUtils.awaitDnsResolution(hostAddresses, i);
        }
        
        boolean useExternalMediaDriver = Boolean.parseBoolean(System.getenv().getOrDefault("LAUNCH_EMBEDDED", "false"));

        System.out.println("Launching cluster node now...");

        try (final ShutdownSignalBarrier barrier = new ShutdownSignalBarrier()) {
            final var mediaDriverContext = new MediaDriver.Context()
                    .aeronDirectoryName(aeronDirName)
                    .threadingMode(ThreadingMode.SHARED)
                    .termBufferSparseFile(true)
                    .multicastFlowControlSupplier(new MinMulticastFlowControlSupplier())
                    .terminationHook(barrier::signal)
                    .errorHandler(CacheNodeApplication.errorHandler("Media Driver"));

            try (var mediaDriver = useExternalMediaDriver ? null : MediaDriver.launch(mediaDriverContext);
                 var archive = Archive.launch(archiveContext);
                 var concensusModule = ConsensusModule.launch(consensusModuleContext);
                 var serviceContainer = ClusteredServiceContainer.launch(clusteredServiceContext)) {

                System.out.println("Started Cluster Node: "+nodeId+" on " + hostname);
                barrier.await();
                System.out.println("Exiting cluster node: "+nodeId);
            }
        }
        catch (Exception e){
            throw e;
        }
    }

    private static SBEDecodingCacheClusterService<Reusable<?>, Reusable<?>, Reusable<?>> getSbeDecodingCacheClusterService(
            int nodeId, CacheManagerFactory<Reusable<?>, Reusable<?>, Reusable<?>> cacheManagerFactory) {
        return new SBEDecodingCacheClusterService<>(String.valueOf(nodeId), getTracingService(nodeId), cacheManagerFactory);
    }

    private static CacheManagerFactory getCacheManagerFactory() {
        ServiceLoader<CacheManagerFactory> service = ServiceLoader.load(CacheManagerFactory.class);
        Optional<CacheManagerFactory> first = service.findFirst();

        if (first.isPresent()) {
            return first.get();
        } else {
            throw new IllegalStateException("No CacheManagerFactory found.");
        }
    }

    private static CacheTracingService getTracingService(int nodeId) {
        String tracingServiceName = System.getenv("OTEL_SERVICE_NAME");
        String jaegerExporter = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");

        if (tracingServiceName != null) {
            System.out.println("Aeron Cache tracing is enabled as OTEL Service:" + tracingServiceName);
            return new OtelTracingService(jaegerExporter, tracingServiceName, String.valueOf(nodeId));
        }

        return new NoOpTracingService();
    }

}
