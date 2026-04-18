package com.bhf.aeroncache.integration.utils;

import com.bhf.aeroncache.integration.BackendTestResource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Basic utility functions for helping in the restarting of containers.
 */
public class ContainerRestartUtils {

    /**
     * Wait for the HTTP interface to be restarted.
     *
     * @param backend
     * @return
     */
    public static MappedHostDetails awaitHTTPInterfaceRestart(BackendTestResource backend) {
        backend.getContainers().httpContainer().start();
        startWithRetry(backend.getContainers().httpContainer());
        backend.getContainers().httpContainer().waitingFor(Wait.forHttp("/readiness"));

        var mappedPort = backend.getContainers().httpContainer().getMappedPort(7070);
        var mappedHost = "http://" + backend.getContainers().httpContainer().getHost();
        return new MappedHostDetails(mappedHost, mappedPort);
    }

    /**
     * Wait for the AeronCache Cluster to be restarted.
     *
     * @param backend
     */
    public static void awaitAeronCacheClusterRestart(BackendTestResource backend) {
        backend.getContainers().clusterContainers().forEach(ContainerRestartUtils::startWithRetry);
        awaitOnFirstRestartAttempt();

        int clusterNodesLaunched = 0;
        while (clusterNodesLaunched != 3) {
            int nodesUp = 0;

            for (var container : backend.getContainers().clusterContainers()) {
                if (container.isRunning()) {
                    nodesUp++;
                }
            }
            if (nodesUp < 3) {
                System.out.println("Only "+nodesUp+" cache nodes up");
                backend.getContainers().clusterContainers().forEach(ContainerRestartUtils::startWithRetry);
            } else {
                System.out.println("All cache nodes started and running");
                clusterNodesLaunched = nodesUp;
            }
        }
    }

    private static void awaitOnFirstRestartAttempt() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void startWithRetry(GenericContainer<?> container) {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                if (!container.isRunning()) {
                    System.out.println("Starting container "+container.getDockerImageName()+", attempt "+i);
                    container.stop();
                    container.start();
                }
                if (container.isRunning()) {
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Couldn't start container "+container.getDockerImageName()+", attempt "+i);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }
}
