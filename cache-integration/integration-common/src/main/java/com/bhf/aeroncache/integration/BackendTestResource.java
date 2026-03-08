package com.bhf.aeroncache.integration;

import com.bhf.aeroncache.application.ClusterLauncher;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.extension.ExtensionContext;

@RequiredArgsConstructor
@Getter
public class BackendTestResource implements ExtensionContext.Store.CloseableResource {

    private final String baseHttpUri;
    private final int httpPort;

    private final String baseWsUri;
    private final int wsPort;

    private final String baseSSEUri;
    private final int ssePort;

    private final String functionalityKey;

    @Override
    public void close() {
        System.out.println("Shutting down backend...");
        try {
            System.out.println("Shutting down Aeron Cache Cluster");

            System.out.println("Shutting down Node0");
            var node0Exit = ClusterLauncher.shutdownCluster(functionalityKey + "_0/node0/cluster/");

            System.out.println("Node 0 exit code: " + node0Exit);

            if (node0Exit != 0) {
                System.out.println("Shutting down Node1");
                var node1Exit = ClusterLauncher.shutdownCluster(functionalityKey + "_1/node1/cluster/");

                System.out.println("Node 1 exit code: " + node1Exit);
                if (node1Exit != 0) {
                    System.out.println("Shutting down Node2");
                    var node2Exit = ClusterLauncher.shutdownCluster(functionalityKey + "_2/node2/cluster/");

                    System.out.println("NODE 2 EXIT CODE: " + node2Exit);
                } else {
                    System.out.println("PRIMARY NODE WAS NODE 1");
                }
            } else {
                System.out.println("PRIMARY NODE WAS NODE 0");
            }

        } catch (Throwable e) {
            System.err.println("Error during cluster shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
