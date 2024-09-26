package com.bhf.aeroncache.application;

import java.util.concurrent.Executors;

/**
 * Launch a 3 node cluster.
 */
public class ClusterLauncher {
    public static void main(String[] args) {
        int clusterNodes = 3;
        var pool = Executors.newFixedThreadPool(clusterNodes);

        for (var i = 0; i < clusterNodes; i++) {
            int finalI = i;
            pool.execute(() -> {
                System.out.println("Launching cluster with node Id: " + finalI);
                ClusterNodeApplication.main(new String[]{String.valueOf(finalI)});
            });
        }
    }

}
