package com.bhf.aeroncache.integration;

import com.bhf.aeroncache.application.ClusterLauncher;
import com.bhf.aeroncache.http.application.HttpApplication;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class BackendTestLauncher implements BeforeAllCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create("backend");

    @Override
    public void beforeAll(ExtensionContext context) {

        ExtensionContext root = context.getRoot();
        ExtensionContext.Store store = root.getStore(NAMESPACE);

        store.getOrComputeIfAbsent("backend", key -> {
            try {
                System.out.println("Starting backend once...");
                ClusterLauncher.launchCluster(3);

                HttpApplication.main(null);

                return new BackendResource();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    static class BackendResource implements ExtensionContext.Store.CloseableResource {
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
}
