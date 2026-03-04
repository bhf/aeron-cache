package com.bhf.aeroncache.application;

import java.util.concurrent.Executors;

/**
 * Launch a 3 node dev cluster.
 */
public class ClusterLauncher {
    public static void main(String[] args) {
        int clusterNodes = 3;
        launchTestCluster(clusterNodes);
    }

    public static void launchTestCluster(int clusterNodes) {
        var pool = Executors.newFixedThreadPool(clusterNodes);

        for (var i = 0; i < clusterNodes; i++) {
            int finalI = i;
            pool.execute(() -> {
                System.out.println("Launching cluster with node Id: " + finalI);
                CacheNodeApplication.main(new String[]{String.valueOf(finalI)});
            });
        }
    }

    public static void launchTestCluster(int clusterNodes, String functionalityKey) {
        var pool = Executors.newFixedThreadPool(clusterNodes);

        for (var i = 0; i < clusterNodes; i++) {
            int finalI = i;
            pool.execute(() -> {
                System.out.println("Launching cluster with node Id: " + finalI);
                var baseDir = functionalityKey+"_"+finalI;
                CacheNodeApplication.startAeronCacheApplication(new String[]{String.valueOf(finalI)}, baseDir);
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
