package com.bhf.aeroncache.application;

import io.aeron.cluster.ClusterTool;

import java.util.concurrent.Executors;

/**
 * Launch a 3 node dev cluster.
 */
public class ClusterLauncher {
    public static void main(String[] args) {
        int clusterNodes = 3;
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
        System.out.println("Shutting down now");
        ClusterTool.main(new String[]{".", "shutdown"});
        System.out.println("Should now be shutdown");
    }

}
