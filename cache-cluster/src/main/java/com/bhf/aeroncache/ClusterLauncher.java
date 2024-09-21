package com.bhf.aeroncache;

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
                ClusterNodeApplication.main(new String[]{String.valueOf(finalI)});
            });
        }
    }
}
