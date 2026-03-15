package com.bhf.aeroncache.integration.utils;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A factory for creating different combinations of Aeron Cache environments.
 */
public class TestContainersEnvironmentFactory {

    public static final long SHM_SIZE_MBS = 512L;

    /**
     * Get containers for an AeronCache Cluster.
     *
     * @param nodes
     * @param network
     * @return
     */
    public static List<GenericContainer<?>> getClusteredCacheContainers(int nodes, Network network) {

        List<GenericContainer<?>> containers = new ArrayList<>();

        String clusterAddresses = getClusterAddresses(nodes);

        for (int i = 0; i < nodes; i++) {

            String name = "node" + i;
            String hostPath = "/tmp/aeron-cache/" + name + "-" + UUID.randomUUID();
            new File(hostPath).mkdirs();

            GenericContainer<?> container =
                    new GenericContainer<>("aeroncache-cluster:latest")
                            .withNetwork(network)
                            .withNetworkAliases(name)
                            .withCreateContainerCmdModifier(cmd -> cmd.withHostName(name))
                            .withFileSystemBind(hostPath, "/tmp/data", BindMode.READ_WRITE)
                            .withSharedMemorySize(SHM_SIZE_MBS * 1024L * 1024L)
                            .withEnv("JAVA_TOOL_OPTIONS", "-Daeron.debug.timeout=60s")
                            .withEnv("CLUSTER_ADDRESSES", clusterAddresses)
                            .withEnv("CLUSTER_NODE", String.valueOf(i))
                            .withEnv("CLUSTER_PORT_BASE", "9000")
                            .withEnv("OTEL_SERVICE_NAME", "aeron-cache-cluster-node")
                            .withEnv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://jaeger:4317")
                            .withEnv("CACHE_MODE", "RAFT")
                            .withEnv("CACHE_DATA_DIR", "/tmp/data");

            containers.add(container);
        }

        return containers;
    }

    /**
     * Get a container for a single HTTP interface configured for using cluster mode.
     *
     * @param nodes
     * @param network
     * @return
     */
    public static GenericContainer<?> getClusteredHTTPContainer(int nodes, Network network) {
        String clusterAddresses = getClusterAddresses(nodes);

        return new GenericContainer<>("aeroncache-http-javalin:latest")
                .withNetwork(network)
                .withNetworkAliases("cache-http-client")
                .withSharedMemorySize(SHM_SIZE_MBS * 1024L * 1024L)
                .withExposedPorts(7070)
                .withEnv("JAVA_TOOL_OPTIONS", "-Daeron.debug.timeout=60s")
                .withEnv("CLUSTER_ADDRESSES", clusterAddresses)
                .withEnv("EGRESS_IP", "172.16.202.5")
                .withEnv("OTEL_SERVICE_NAME", "aeron-cache-http")
                .withEnv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://jaeger:4318")
                .withEnv("OTEL_JAVAAGENT_LOGGING", "none")
                .withEnv("CACHE_MODE", "RAFT")
                .waitingFor(Wait.forHttp("/readiness"));
    }

    /**
     * Get a container for a single WS interface configured for using cluster mode.
     *
     * @param nodes
     * @param network
     * @return
     */
    public static GenericContainer<?> getClusteredWSContainer(int nodes, Network network) {
        String clusterAddresses = getClusterAddresses(nodes);

        return new GenericContainer<>("aeroncache-ws-javalin:latest")
                .withNetwork(network)
                .withNetworkAliases("cache-ws-client")
                .withSharedMemorySize(SHM_SIZE_MBS * 1024L * 1024L)
                .withExposedPorts(7071)
                .withEnv("JAVA_TOOL_OPTIONS", "-Daeron.debug.timeout=60s")
                .withEnv("CLUSTER_ADDRESSES", clusterAddresses)
                .withEnv("EGRESS_IP", "172.16.202.5")
                .withEnv("OTEL_SERVICE_NAME", "aeron-cache-ws")
                .withEnv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://jaeger:4318")
                .withEnv("OTEL_JAVAAGENT_LOGGING", "none")
                .withEnv("CACHE_MODE", "RAFT")
                .waitingFor(Wait.forHttp("/readiness"));
    }

    /**
     * Get a container for a single SSE interface configured for using cluster mode.
     *
     * @param nodes
     * @param network
     * @return
     */
    public static GenericContainer<?> getClusteredSSEContainer(int nodes, Network network) {
        String clusterAddresses = getClusterAddresses(nodes);

        return new GenericContainer<>("aeroncache-sse-jooby:latest")
                .withNetwork(network)
                .withNetworkAliases("cache-sse-client")
                .withSharedMemorySize(SHM_SIZE_MBS * 1024L * 1024L)
                .withExposedPorts(7072)
                .withEnv("JAVA_TOOL_OPTIONS", "-Daeron.debug.timeout=60s")
                .withEnv("CLUSTER_ADDRESSES", clusterAddresses)
                .withEnv("EGRESS_IP", "172.16.202.5")
                .withEnv("OTEL_SERVICE_NAME", "aeron-cache-ws")
                .withEnv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://jaeger:4318")
                .withEnv("OTEL_JAVAAGENT_LOGGING", "none")
                .withEnv("CACHE_MODE", "RAFT")
                .withEnv("REQUEST_PUB_HOST", "node0")
                .waitingFor(Wait.forHttp("/readiness"));
    }

    /**
     * Get a container for a single AeronCache node.
     *
     * @param network
     * @return
     */
    public static GenericContainer<?> getSingleNodeCacheContainer(Network network) {
        String name = "node0";
        String hostPath = "/tmp/aeron-cache/" + name + "-" + UUID.randomUUID();
        new File(hostPath).mkdirs();

        return new GenericContainer<>("aeroncache-cluster:latest")
                .withNetwork(network)
                .withNetworkAliases(name)
                .withCreateContainerCmdModifier(cmd -> cmd.withHostName(name))
                .withFileSystemBind(hostPath, "/tmp/data", BindMode.READ_WRITE)
                .withSharedMemorySize(SHM_SIZE_MBS * 1024L * 1024L)
                .withEnv("JAVA_TOOL_OPTIONS", "-Daeron.debug.timeout=60s")
                .withEnv("CLUSTER_NODE", "0")
                .withEnv("OTEL_SERVICE_NAME", "aeron-cache-cluster-node")
                .withEnv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://jaeger:4317")
                .withEnv("CACHE_MODE", "SINGLE")
                .withEnv("CACHE_DATA_DIR", "/tmp/data")
                .withEnv("HTTP_RESPONSE_PUB_HOST", "cache-http-client")
                .withEnv("WS_RESPONSE_PUB_HOST", "cache-ws-client")
                .withEnv("SSE_RESPONSE_PUB_HOST", "cache-sse-client");
    }

    /**
     * Get a container for a single HTTP interface configured for single node mode.
     *
     * @param network
     * @return
     */
    public static GenericContainer<?> getSingleNodeHTTPContainer(Network network) {
        return new GenericContainer<>("aeroncache-http-javalin:latest")
                .withNetwork(network)
                .withNetworkAliases("cache-http-client")
                .withSharedMemorySize(SHM_SIZE_MBS * 1024L * 1024L)
                .withExposedPorts(7070)
                .withEnv("JAVA_TOOL_OPTIONS", "-Daeron.debug.timeout=60s")
                .withEnv("OTEL_SERVICE_NAME", "aeron-cache-http")
                .withEnv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://jaeger:4318")
                .withEnv("OTEL_JAVAAGENT_LOGGING", "none")
                .withEnv("CLUSTER_ADDRESSES", "node0")
                .withEnv("CACHE_MODE", "SINGLE")
                .withEnv("REQUEST_PUB_HOST", "node0")
                .waitingFor(Wait.forHttp("/readiness"));
    }

    /**
     * Get a container for a single WS interface configured for single node mode.
     *
     * @param network
     * @return
     */
    public static GenericContainer<?> getSingleNodeWSContainer(Network network) {
        return new GenericContainer<>("aeroncache-ws-javalin:latest")
                .withNetwork(network)
                .withNetworkAliases("cache-ws-client")
                .withSharedMemorySize(SHM_SIZE_MBS * 1024L * 1024L)
                .withExposedPorts(7071)
                .withEnv("JAVA_TOOL_OPTIONS", "-Daeron.debug.timeout=60s")
                .withEnv("OTEL_SERVICE_NAME", "aeron-cache-ws")
                .withEnv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://jaeger:4318")
                .withEnv("OTEL_JAVAAGENT_LOGGING", "none")
                .withEnv("CLUSTER_ADDRESSES", "node0")
                .withEnv("CACHE_MODE", "SINGLE")
                .withEnv("REQUEST_PUB_HOST", "node0")
                .waitingFor(Wait.forHttp("/readiness"));
    }

    /**
     * Get a container for a single SSE interface configured for single node mode.
     *
     * @param network
     * @return
     */
    public static GenericContainer<?> getSingleNodeSSEContainer(Network network) {
        return new GenericContainer<>("aeroncache-sse-jooby:latest")
                .withNetwork(network)
                .withNetworkAliases("cache-sse-client")
                .withSharedMemorySize(SHM_SIZE_MBS * 1024L * 1024L)
                .withExposedPorts(7072)
                .withEnv("JAVA_TOOL_OPTIONS", "-Daeron.debug.timeout=60s")
                .withEnv("OTEL_SERVICE_NAME", "aeron-cache-sse")
                .withEnv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://jaeger:4318")
                .withEnv("OTEL_JAVAAGENT_LOGGING", "none")
                .withEnv("CLUSTER_ADDRESSES", "node0")
                .withEnv("CACHE_MODE", "SINGLE")
                .withEnv("REQUEST_PUB_HOST", "node0")
                .waitingFor(Wait.forHttp("/readiness/"));
    }

    /**
     * Build the AeronCache cluster addresses based on the number of nodes.
     *
     * @param nodes
     * @return
     */
    private static @NotNull String getClusterAddresses(int nodes) {
        return IntStream.range(0, nodes)
                .mapToObj(i -> "node" + i)
                .collect(Collectors.joining(","));
    }

}
