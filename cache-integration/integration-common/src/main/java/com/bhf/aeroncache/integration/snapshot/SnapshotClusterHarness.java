package com.bhf.aeroncache.integration.snapshot;

import com.bhf.aeroncache.integration.utils.TestContainersEnvironmentFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Brings up a single-node clustered cache to either capture a snapshot into a release artifact, or
 * restore a previously captured artifact into a fresh (typically newer) binary.
 *
 * <p>The capture topology mirrors production: a {@code node0} container plus a co-located
 * {@code clustertools} sidecar that shares node0's network and IPC namespace (so the aeron
 * media-driver dir under {@code /dev/shm} is visible) and the same data bind mount. The snapshot is
 * triggered through the product HTTP endpoint {@code POST /api/v1/snapshot}, which routes to the
 * sidecar. Because that endpoint reports an unreliable exit code, the harness confirms the snapshot
 * actually landed by watching the cluster recording log grow.
 *
 * <p>Host bind paths live under {@code /tmp/aeron-cache} because that is the directory the docker
 * host shares with containers (the same one the wider testcontainers harness uses).
 */
public final class SnapshotClusterHarness {

    private static final String HOST_ROOT = "/tmp/aeron-cache";
    private static final String DATA_MOUNT = "/tmp/data";
    private static final String CLUSTER_DIR_IN_DATA = "node0/cluster";
    private static final String JAVA_TOOL_OPTIONS =
            "-Daeron.debug.timeout=60s -Daeron.cache.term.length="
                    + TestContainersEnvironmentFactory.AERON_CACHE_TERM_LENGTH;

    /** Each recording-log entry is a fixed 64 bytes; a fresh log has one (the term log) entry. */
    private static final long RECORDING_LOG_ENTRY_BYTES = 64;

    private SnapshotClusterHarness() {
    }

    // ---------------------------------------------------------------------------------------------
    // Capture
    // ---------------------------------------------------------------------------------------------

    /**
     * Seed the fixture, snapshot it, and write a {@code .tar.gz} artifact (containing {@code node0/}
     * plus {@code manifest.json}) to {@code outputTar}.
     *
     * @param outputTar      destination tarball (parent dirs created)
     * @param artifactVersion version string recorded in the manifest (e.g. the release tag)
     */
    public static void capture(Path outputTar, String artifactVersion) throws IOException, InterruptedException {
        String hostPath = HOST_ROOT + "/capture-" + UUID.randomUUID();
        new File(hostPath).mkdirs();

        Network network = Network.newNetwork();
        // node0 must own a shareable IPC namespace so the clustertools sidecar can join it and see
        // the running node's aeron media-driver dir under /dev/shm.
        GenericContainer<?> node = clusterNode(network, hostPath)
                .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withIpcMode("shareable"));
        try {
            node.start();
            try (GenericContainer<?> clustertools = clustertoolsSidecar(node);
                 GenericContainer<?> http = httpInterface(network)) {
                clustertools.start();
                http.start();

                String httpBase = "http://" + http.getHost() + ":" + http.getMappedPort(7070);
                SnapshotHttp client = new SnapshotHttp(httpBase);
                client.awaitReady(Duration.ofMinutes(2));
                client.seed();

                Path recordingLog = Path.of(hostPath, CLUSTER_DIR_IN_DATA, "recording.log");
                long entriesBefore = recordingLogEntries(recordingLog);

                client.triggerSnapshot();
                awaitSnapshotPersisted(recordingLog, entriesBefore, Duration.ofMinutes(1));
            }
        } finally {
            node.stop();
            network.close();
        }

        writeManifest(Path.of(hostPath, "manifest.json"), artifactVersion);
        tarGz(outputTar, hostPath, "node0", "manifest.json");
        deleteRecursively(Path.of(hostPath));
    }

    // ---------------------------------------------------------------------------------------------
    // Restore
    // ---------------------------------------------------------------------------------------------

    /** A restored cache kept running so a caller can assert against it, then close to tear down. */
    public static final class RestoredCache implements AutoCloseable {
        private final SnapshotHttp client;
        private final int artifactFixtureVersion;
        private final Network network;
        private final GenericContainer<?> node;
        private final GenericContainer<?> http;
        private final String hostPath;

        private RestoredCache(SnapshotHttp client, int artifactFixtureVersion, Network network,
                              GenericContainer<?> node, GenericContainer<?> http, String hostPath) {
            this.client = client;
            this.artifactFixtureVersion = artifactFixtureVersion;
            this.network = network;
            this.node = node;
            this.http = http;
            this.hostPath = hostPath;
        }

        public SnapshotHttp client() {
            return client;
        }

        /** Assert every fixture entry present at the artifact's fixture version recovered correctly. */
        public void verifyFixture() {
            client.verifyFixture(artifactFixtureVersion);
        }

        @Override
        public void close() {
            http.stop();
            node.stop();
            network.close();
            deleteRecursively(Path.of(hostPath));
        }
    }

    /**
     * Extract an artifact and boot a fresh node against it.
     *
     * @param artifactTar      the {@code .tar.gz} produced by {@link #capture}
     * @param seedFromSnapshot when true, rebuild the recording log from the latest snapshot before
     *                         boot so recovery can only come from the snapshot (not log replay) -
     *                         the strongest backward-compatibility signal
     */
    public static RestoredCache restore(Path artifactTar, boolean seedFromSnapshot)
            throws IOException, InterruptedException {
        String hostPath = HOST_ROOT + "/restore-" + UUID.randomUUID();
        new File(hostPath).mkdirs();
        untarGz(artifactTar, hostPath);

        int fixtureVersion = readManifestFixtureVersion(Path.of(hostPath, "manifest.json"));

        if (seedFromSnapshot) {
            seedRecordingLogFromSnapshot(hostPath);
        }

        Network network = Network.newNetwork();
        GenericContainer<?> node = clusterNode(network, hostPath);
        GenericContainer<?> http = httpInterface(network);
        try {
            node.start();
            http.start();

            String httpBase = "http://" + http.getHost() + ":" + http.getMappedPort(7070);
            SnapshotHttp client = new SnapshotHttp(httpBase);
            client.awaitReady(Duration.ofMinutes(2));
            return new RestoredCache(client, fixtureVersion, network, node, http, hostPath);
        } catch (RuntimeException e) {
            http.stop();
            node.stop();
            network.close();
            deleteRecursively(Path.of(hostPath));
            throw e;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Containers
    // ---------------------------------------------------------------------------------------------

    private static GenericContainer<?> clusterNode(Network network, String hostPath) {
        return TestContainersEnvironmentFactory.getSingleNodeClusterContainer(network, hostPath)
                .waitingFor(Wait.forLogMessage(".*Launching AeronCache Cluster Node.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(2)));
    }

    /**
     * The clustertools sidecar. It shares node0's network and IPC namespace (pod emulation) so it
     * is reachable at {@code node0:7080} and can see the running node's aeron dir under
     * {@code /dev/shm}; it mounts the same data dir and is pointed at the absolute cluster folder.
     */
    private static GenericContainer<?> clustertoolsSidecar(GenericContainer<?> node) {
        String ref = "container:" + node.getContainerId();
        return new GenericContainer<>(TestContainersEnvironmentFactory.getImageName("aeroncache-http-clustertools"))
                .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig()
                        .withNetworkMode(ref)
                        .withIpcMode(ref))
                .withFileSystemBind(nodeHostPath(node), DATA_MOUNT, BindMode.READ_WRITE)
                .withEnv("JAVA_TOOL_OPTIONS", JAVA_TOOL_OPTIONS)
                .withEnv("CLUSTER_FOLDER", DATA_MOUNT + "/" + CLUSTER_DIR_IN_DATA)
                .waitingFor(Wait.forLogMessage(".*Listening on.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(1)));
    }

    private static GenericContainer<?> httpInterface(Network network) {
        return TestContainersEnvironmentFactory.getClusteredHTTPContainer(1, network);
    }

    private static String nodeHostPath(GenericContainer<?> node) {
        return node.getBinds().stream()
                .filter(b -> DATA_MOUNT.equals(b.getVolume().getPath()))
                .map(b -> b.getPath())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("node has no " + DATA_MOUNT + " bind"));
    }

    // ---------------------------------------------------------------------------------------------
    // Snapshot verification + ClusterTool
    // ---------------------------------------------------------------------------------------------

    private static long recordingLogEntries(Path recordingLog) throws IOException {
        if (!Files.exists(recordingLog)) {
            return 0;
        }
        return Files.size(recordingLog) / RECORDING_LOG_ENTRY_BYTES;
    }

    private static void awaitSnapshotPersisted(Path recordingLog, long entriesBefore, Duration timeout)
            throws IOException, InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (recordingLogEntries(recordingLog) > entriesBefore) {
                return;
            }
            Thread.sleep(1000);
        }
        throw new IllegalStateException("Snapshot never persisted: recording log at " + recordingLog
                + " did not grow beyond " + entriesBefore + " entries within " + timeout);
    }

    /** Run a one-shot ClusterTool command offline against the mounted cluster dir. */
    private static void runClusterTool(String hostPath, String command) throws IOException, InterruptedException {
        try (GenericContainer<?> tool = new GenericContainer<>(
                TestContainersEnvironmentFactory.getImageName("aeroncache-cluster"))
                .withFileSystemBind(hostPath, DATA_MOUNT, BindMode.READ_WRITE)
                .withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint("java"))
                .withCommand("--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
                        "-cp", "@/app/jib-classpath-file",
                        "io.aeron.cluster.ClusterTool",
                        DATA_MOUNT + "/" + CLUSTER_DIR_IN_DATA, command)
                .withStartupCheckStrategy(new org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy()
                        .withTimeout(Duration.ofMinutes(1)))) {
            tool.start();
        }
    }

    private static void seedRecordingLogFromSnapshot(String hostPath) throws IOException, InterruptedException {
        runClusterTool(hostPath, "seed-recording-log-from-snapshot");
    }

    // ---------------------------------------------------------------------------------------------
    // Manifest + archive helpers
    // ---------------------------------------------------------------------------------------------

    private static void writeManifest(Path manifest, String artifactVersion) throws IOException {
        var json = new JSONObject()
                .put("artifactVersion", artifactVersion)
                .put("fixtureVersion", SnapshotFixture.VERSION)
                .put("writerImageTag", System.getProperty("aeroncache.image.tag", "latest"))
                .put("clusterSize", 1)
                .put("cacheMode", "RAFT")
                .put("createdEpochMs", Instant.now().toEpochMilli())
                .put("caches", new JSONArray(SnapshotFixture.CACHES));
        Files.writeString(manifest, json.toString(2));
    }

    private static int readManifestFixtureVersion(Path manifest) throws IOException {
        if (!Files.exists(manifest)) {
            return SnapshotFixture.VERSION; // legacy artifact without a manifest
        }
        return new JSONObject(Files.readString(manifest)).getInt("fixtureVersion");
    }

    private static void tarGz(Path outputTar, String baseDir, String... entries)
            throws IOException, InterruptedException {
        Files.createDirectories(outputTar.toAbsolutePath().getParent());
        var cmd = new java.util.ArrayList<String>(List.of("tar", "-czf", outputTar.toAbsolutePath().toString(),
                "-C", baseDir));
        cmd.addAll(List.of(entries));
        run(cmd);
    }

    private static void untarGz(Path tar, String destDir) throws IOException, InterruptedException {
        Files.createDirectories(Path.of(destDir));
        run(List.of("tar", "-xzf", tar.toAbsolutePath().toString(), "-C", destDir));
    }

    /** Best-effort recursive delete of a harness host directory; failures are non-fatal. */
    private static void deleteRecursively(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // leftover files are cleaned by the OS temp reaper / CI runner teardown
                }
            });
        } catch (IOException ignored) {
            // nothing else to do - the directory lives under /tmp
        }
    }

    private static void run(List<String> cmd) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(cmd).inheritIO().start();
        int exit = p.waitFor();
        if (exit != 0) {
            throw new IOException("Command failed (" + exit + "): " + String.join(" ", cmd));
        }
    }
}
