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

    @Override
    public void close() {
        System.out.println("Shutting down backend...");
        try {
            System.out.println("Shutting down Aeron Cache Cluster");
            ClusterLauncher.shutdownCluster();
        } catch (Throwable e) {
            System.err.println("Error during cluster shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
