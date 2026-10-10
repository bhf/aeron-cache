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

    /**
     * Shared memory for the purge flow's node + HTTP interface. The purge test deliberately pushes
     * more than one 128MB archive segment of log through the cluster so a whole segment can be
     * reclaimed; the default 512MB /dev/shm is exhausted by that burst (128MB term buffers) and the
     * media driver wedges before the snapshot can persist, so the purge flow needs a larger /dev/shm.
     */
    private static final long PURGE_SHM_BYTES = 2L * 1024 * 1024 * 1024;

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
    // Purge (disk reclamation)
    // ---------------------------------------------------------------------------------------------

    /** Measurements and outcome of a purge run against a live cluster. */
    public record PurgeOutcome(
            long archiveBytesBefore,
            long archiveBytesAfter,
            JSONObject purgeResponse,
            boolean fixtureStillServed) {

        public boolean purgeSucceeded() {
            return purgeResponse.optBoolean("success", false);
        }

        public long reclaimedBytes() {
            return purgeResponse.optLong("reclaimedBytes", 0);
        }

        public long purgedToPosition() {
            return purgeResponse.optLong("purgedToPosition", -1);
        }
    }

    /**
     * Seed the fixture, grow the cluster log across several snapshot rounds, then purge old log
     * segments down to the retained snapshot floor and confirm the cluster still serves the fixture.
     *
     * <p>The sidecar is co-located with {@code node0} sharing its network and IPC namespace (so the
     * in-process Archive client can reach node0's archive over its IPC local control channel), and
     * node0 additionally publishes the sidecar's {@code 7080} port so the purge endpoints are
     * reachable from the host. Reclamation is only observable once the retained floor sits beyond a
     * full archive segment, so {@code churnItemsPerRound} / {@code churnItemBytes} must push more
     * than one segment of log before the oldest retained snapshot - tune them to the image's archive
     * segment length if a strict reclamation assertion is required.
     *
     * @param snapshotRounds     number of snapshot rounds (must exceed {@code retentionCount} for any
     *                           history to be purgeable).
     * @param retentionCount     snapshots to retain (sets {@code SNAPSHOT_RETENTION_COUNT}).
     * @param churnItemsPerRound large items written to the log before each snapshot.
     * @param churnItemBytes     size of each churn item value, in bytes.
     * @return the purge measurements and outcome.
     */
    public static PurgeOutcome purgeReclaimsDisk(
            int snapshotRounds, int retentionCount, int churnItemsPerRound, int churnItemBytes)
            throws IOException, InterruptedException {
        String hostPath = HOST_ROOT + "/purge-" + UUID.randomUUID();
        new File(hostPath).mkdirs();

        Network network = Network.newNetwork();
        GenericContainer<?> node = clusterNode(network, hostPath)
                .withSharedMemorySize(PURGE_SHM_BYTES)
                .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withIpcMode("shareable"));
        // Publish the sidecar's 7080 (the sidecar shares node0's network namespace) so the host can
        // reach the snapshot-info / purge endpoints directly.
        node.addExposedPort(7080);

        try {
            node.start();
            String aeronDir = discoverAeronDir(node);
            System.out.println("Discovered node0 aeron dir: " + aeronDir);

            try (GenericContainer<?> clustertools = purgeSidecar(node, retentionCount, aeronDir);
                 GenericContainer<?> http = httpInterface(network).withSharedMemorySize(PURGE_SHM_BYTES)) {
                clustertools.start();
                http.start();

                String httpBase = "http://" + http.getHost() + ":" + http.getMappedPort(7070);
                SnapshotHttp cache = new SnapshotHttp(httpBase);
                cache.awaitReady(Duration.ofMinutes(2));
                cache.seed();

                String toolsBase = "http://" + node.getHost() + ":" + node.getMappedPort(7080);
                ClusterToolsHttp tools = new ClusterToolsHttp(toolsBase);
                tools.awaitReady(Duration.ofMinutes(1));

                Path recordingLog = Path.of(hostPath, CLUSTER_DIR_IN_DATA, "recording.log");
                cache.createCache("purge-churn");
                String churnValue = "x".repeat(churnItemBytes);
                for (int round = 0; round < snapshotRounds; round++) {
                    for (int i = 0; i < churnItemsPerRound; i++) {
                        cache.putItem("purge-churn", "r" + round + "-k" + i, churnValue);
                    }
                    long entriesBefore = recordingLogEntries(recordingLog);
                    cache.triggerSnapshot();
                    awaitSnapshotPersisted(recordingLog, entriesBefore, Duration.ofMinutes(2));
                }

                Path archiveDir = Path.of(hostPath, "node0", "archive");
                long bytesBefore = directorySizeOf(archiveDir);
                JSONObject purgeResponse = tools.purge();
                long bytesAfter = directorySizeOf(archiveDir);

                boolean fixtureStillServed;
                try {
                    cache.verifyFixture(SnapshotFixture.VERSION);
                    fixtureStillServed = true;
                } catch (AssertionError | RuntimeException e) {
                    fixtureStillServed = false;
                }

                return new PurgeOutcome(bytesBefore, bytesAfter, purgeResponse, fixtureStillServed);
            }
        } finally {
            node.stop();
            network.close();
            deleteRecursively(Path.of(hostPath));
        }
    }

    /**
     * The clustertools sidecar wired for purge: it shares node0's network + IPC namespace (so the
     * Archive client reaches node0's archive IPC control channel and sees its aeron dir under
     * {@code /dev/shm}), mounts the same data dir, and is told the archive dir, aeron dir and
     * retention count.
     */
    private static GenericContainer<?> purgeSidecar(GenericContainer<?> node, int retentionCount, String aeronDir) {
        String ref = "container:" + node.getContainerId();
        GenericContainer<?> sidecar = new GenericContainer<>(
                TestContainersEnvironmentFactory.getImageName("aeroncache-http-clustertools"))
                .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig()
                        .withNetworkMode(ref)
                        .withIpcMode(ref))
                .withFileSystemBind(nodeHostPath(node), DATA_MOUNT, BindMode.READ_WRITE)
                .withEnv("JAVA_TOOL_OPTIONS", JAVA_TOOL_OPTIONS)
                .withEnv("CLUSTER_FOLDER", DATA_MOUNT + "/" + CLUSTER_DIR_IN_DATA)
                .withEnv("ARCHIVE_DIR", DATA_MOUNT + "/node0/archive")
                .withEnv("SNAPSHOT_RETENTION_COUNT", Integer.toString(retentionCount))
                .waitingFor(Wait.forLogMessage(".*Listening on.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(1)));
        if (aeronDir != null) {
            sidecar.withEnv("AERON_DIR", aeronDir);
        }
        return sidecar;
    }

    /**
     * Find node0's aeron media driver directory by locating the driver's {@code cnc.dat} under
     * {@code /dev/shm} - independent of the derived dir name (user / node-id / {@code -driver}
     * suffix). Polls briefly because the embedded driver creates the file during node startup.
     *
     * @return the aeron directory (parent of {@code cnc.dat}), or null if not found.
     */
    private static String discoverAeronDir(GenericContainer<?> node) {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < deadline) {
            try {
                var result = node.execInContainer("sh", "-c",
                        "f=$(find /dev/shm -maxdepth 2 -name cnc.dat 2>/dev/null | head -1); "
                                + "[ -n \"$f\" ] && dirname \"$f\"");
                String out = result.getStdout() == null ? "" : result.getStdout().trim();
                if (!out.isEmpty()) {
                    return out;
                }
            } catch (IOException e) {
                // retry until the deadline
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private static long directorySizeOf(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return 0L;
        }
        // The archive is live (segments are created, renamed and purged while we measure), so a plain
        // Files.walk would throw NoSuchFileException when a file vanishes mid-walk. Skip those instead.
        long[] total = {0L};
        Files.walkFileTree(dir, new java.nio.file.SimpleFileVisitor<>() {
            @Override
            public java.nio.file.FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) {
                if (attrs.isRegularFile()) {
                    total[0] += attrs.size();
                }
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult visitFileFailed(Path file, IOException exc) {
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult postVisitDirectory(Path d, IOException exc) {
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
        return total[0];
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
