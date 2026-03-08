package com.bhf.aeroncache.application;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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

    public static int shutdownCluster(String folder) {
        System.out.println("Shutting down now...");
        return runClusterTool("abort", folder);
    }

    private static int runClusterTool(String command, String folder) {
        try {
            var javaHome = System.getProperty("java.home");
            var javaBin = javaHome + File.separator + "bin" + File.separator + "java";
            var classpath = System.getProperty("java.class.path");

            List<String> args = new ArrayList<>();
            args.add(javaBin);
            args.add("--add-opens");
            args.add("java.base/jdk.internal.misc=ALL-UNNAMED");
            System.getProperties().forEach((key, value) -> args.add("-D" + key + "=" + value));

            args.add("-cp");
            args.add(classpath);
            args.add("io.aeron.cluster.ClusterTool");
            args.add(folder);
            args.add(command);

            ProcessBuilder pb = new ProcessBuilder(args);
            pb.inheritIO();
            var process = pb.start();
            return process.waitFor();
        } catch (Exception e) {
            System.out.println("Error trying to use ClusterTool");
            e.printStackTrace();
        }
        return 0;
    }

}
