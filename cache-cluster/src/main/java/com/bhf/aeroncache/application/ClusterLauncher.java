package com.bhf.aeroncache.application;

import io.aeron.cluster.ClusterTool;

import java.util.concurrent.Executors;

/**
 * Launch a 3 node dev cluster.
 */
public class ClusterLauncher {
    public static void main(String[] args) {
        int clusterNodes = 3;
        launchCluster(clusterNodes);
    }

    public static void launchCluster(int clusterNodes) {
        var pool = Executors.newFixedThreadPool(clusterNodes);

        for (var i = 0; i < clusterNodes; i++) {
            int finalI = i;
            pool.execute(() -> {
                System.out.println("Launching cluster with node Id: " + finalI);
                CacheNodeApplication.main(new String[]{String.valueOf(finalI)});
            });
        }
    }

    public static void shutdownCluster() {
        System.out.println("Shutting down now...");
        try {
            var javaHome = System.getProperty("java.home");
            var javaBin = javaHome + java.io.File.separator + "bin" + java.io.File.separator + "java";
            var classpath = System.getProperty("java.class.path");

            ProcessBuilder pb = new ProcessBuilder(
                    javaBin, "-cp", classpath, "io.aeron.cluster.ClusterTool", ".", "shutdown"
            );
            pb.inheritIO();
            var process = pb.start();
            int exitCode = process.waitFor();
            System.out.println("Aeron Cache cluster shutdown process exited with code: " + exitCode);
        } catch (Exception e) {
            System.err.println("Failed to shutdown Aeron Cache cluster");
            e.printStackTrace();
        }
    }

}
