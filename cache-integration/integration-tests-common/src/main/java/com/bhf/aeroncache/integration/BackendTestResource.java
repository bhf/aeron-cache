package com.bhf.aeroncache.integration;

import com.bhf.aeroncache.application.ClusterLauncher;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.extension.ExtensionContext;

@RequiredArgsConstructor
public class BackendTestResource implements ExtensionContext.Store.CloseableResource {

    @Getter
    private final int httpPort;

    @Getter
    private final String baseHttpUri;


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
