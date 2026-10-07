package com.bhf.aeroncache.clustertools.process;

import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs {@code io.aeron.cluster.ClusterTool} as a separate JVM process against a cluster directory.
 *
 * <p>Extracted so both the HTTP request handler and the scheduled snapshot task can trigger cluster
 * tool commands the same way. The spawned process inherits this JVM's classpath and system
 * properties so it resolves the cluster the same way the node does.
 */
@Log4j2
public class ClusterToolProcessRunner {

    private static final String CLUSTER_TOOL_MAIN = "io.aeron.cluster.ClusterTool";

    /**
     * Run a ClusterTool command, blocking until the spawned process exits.
     *
     * @param clusterFolder the cluster data directory to operate on.
     * @param command       the ClusterTool command (e.g. {@code snapshot}, {@code shutdown}).
     * @return the process exit code.
     * @throws Exception if the process could not be started or was interrupted while waiting.
     */
    public int run(final String clusterFolder, final String command) throws Exception {
        final var javaHome = System.getProperty("java.home");
        final var javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        final var classpath = System.getProperty("java.class.path");

        final List<String> args = new ArrayList<>();
        args.add(javaBin);
        args.add("--add-opens");
        args.add("java.base/jdk.internal.misc=ALL-UNNAMED");
        System.getProperties().forEach((key, value) -> args.add("-D" + key + "=" + value));
        args.add("-cp");
        args.add(classpath);
        args.add(CLUSTER_TOOL_MAIN);
        args.add(clusterFolder);
        args.add(command);

        final ProcessBuilder pb = new ProcessBuilder(args);
        pb.inheritIO();
        final var process = pb.start();
        final int exitCode = process.waitFor();
        log.info("Aeron Cache cluster tools command {} exited with code: {}", command, exitCode);
        return exitCode;
    }
}
