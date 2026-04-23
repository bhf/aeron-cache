package com.bhf.aeroncache.integration.utils;

import com.bhf.aeroncache.integration.BackendTestLauncher;
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
        backend.updateHTTPMappings(mappedHost, mappedPort);
        return new MappedHostDetails(mappedHost, mappedPort);
    }

    public static MappedHostDetails awaitWSInterfaceRestart(BackendTestResource backend) {
        backend.getContainers().wsContainer().start();
        startWithRetry(backend.getContainers().wsContainer());
        backend.getContainers().wsContainer().waitingFor(Wait.forHttp("/readiness"));

        var mappedPort = backend.getContainers().wsContainer().getMappedPort(7071);
        var mappedHost = "ws://" + backend.getContainers().wsContainer().getHost();
        backend.updateWSMappings(mappedHost, mappedPort);
        return new MappedHostDetails(mappedHost, mappedPort);
    }

    public static MappedHostDetails awaitSSEInterfaceRestart(BackendTestResource backend) {
        backend.getContainers().sseContainer().start();
        startWithRetry(backend.getContainers().sseContainer());
        backend.getContainers().sseContainer().waitingFor(Wait.forHttp("/readiness"));

        var mappedPort = backend.getContainers().sseContainer().getMappedPort(7072);
        var mappedHost = "http://" + backend.getContainers().sseContainer().getHost();
        backend.updateSSEMappings(mappedHost, mappedPort);
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
        while (clusterNodesLaunched != BackendTestLauncher.NODES) {
            int nodesUp = 0;

            for (var container : backend.getContainers().clusterContainers()) {
                if (container.isRunning()) {
                    nodesUp++;
                }
            }
            if (nodesUp < BackendTestLauncher.NODES) {
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

    public static void stopWebsocketContainer(BackendTestResource backend) {
        while (backend.getContainers().wsContainer().isRunning()) {
            backend.getContainers().wsContainer().stop();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void stopSSEContainer(BackendTestResource backend) {
        while (backend.getContainers().sseContainer().isRunning()) {
            backend.getContainers().sseContainer().stop();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void stopClusterContainers(BackendTestResource backend) {
        var clusterContainers = backend.getContainers().clusterContainers();
        for (GenericContainer<?> clusterContainer : clusterContainers) {
            while (clusterContainer.isRunning()) {
                clusterContainer.stop();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void stopHTTPInterface(BackendTestResource backend) {
        while (backend.getContainers().httpContainer().isRunning()) {
            backend.getContainers().httpContainer().stop();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
