package com.bhf.aeroncache.integration;

import com.bhf.aeroncache.application.ClusterLauncher;
import com.bhf.aeroncache.http.application.HttpApplication;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.BackendTestContainers;
import com.bhf.aeroncache.integration.utils.TestContainersEnvironmentFactory;
import com.bhf.aeroncache.sse.application.SSEApplication;
import com.bhf.aeroncache.ws.application.WebsocketApplication;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.util.ArrayList;
import java.util.List;

public class BackendTestLauncher implements BeforeAllCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(BackendTestLauncher.class);

    private static final String BACKEND_KEY = "backend";
    public static final int NODES = 1;
    private static String functionalityKey;

    @Override
    public void beforeAll(ExtensionContext context) {

        var extensionContextRoot = context.getRoot();
        var extensionContextStore = extensionContextRoot.getStore(NAMESPACE);

        BackendTestConfig config =
                context.getRequiredTestClass()
                        .getAnnotation(BackendTestConfig.class);

        functionalityKey = getFunctionalityKey(config);

        extensionContextStore.getOrComputeIfAbsent(functionalityKey, key -> {
            try {
                return config.useTestContainersEnvironment() ? getTestContainersTestResource(config) 
                        : getEmbeddedBackendTestResource(config);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Create the backend environment using TestContainers.
     *
     * @param config
     * @return
     */
    private BackendTestResource getTestContainersTestResource(BackendTestConfig config) {
        System.out.println("Using TestContainers Environment for "+config);
        Network network = Network.newNetwork();

        if (config.useClusteredMode()) {
            return setupClusteredEnvironment(config, network);
        } else {
            return setupNonClusteredEnvironment(config, network);
        }
    }

    /**
     * Sets up the clustered environment.
     *
     * @param config
     * @param network
     * @return
     */
    private static @NotNull BackendTestResource setupClusteredEnvironment(BackendTestConfig config, Network network) {
        System.out.println("Starting AeronCache Cluster...");
        List<GenericContainer<?>> cacheNodes = TestContainersEnvironmentFactory.getClusteredCacheContainers(NODES, network);
        cacheNodes.forEach(GenericContainer::start);

        var baseHttpUri = "http://localhost";
        var baseHttpNearUri = "http://localhost";
        var baseWsUri = "ws://localhost";
        var baseSseUri = "http://localhost";
        int httpPort = 0;
        int httpNearPort = 0;
        int wsPort = 0;
        int ssePort = 0;

        GenericContainer<?> httpContainer = null;
        GenericContainer<?> httpNearContainer = null;
        GenericContainer<?> wsContainer = null;
        GenericContainer<?> sseContainer = null;

        if (config.httpEnabled()) {
            httpContainer = TestContainersEnvironmentFactory.getClusteredHTTPContainer(NODES, network);
            httpContainer.start();
            baseHttpUri = "http://"+httpContainer.getHost();
            httpPort = httpContainer.getMappedPort(7070);
        }
        if (config.wsEnabled()) {
            wsContainer = TestContainersEnvironmentFactory.getClusteredWSContainer(NODES, network);
            wsContainer.start();
            baseWsUri = "ws://"+wsContainer.getHost();
            wsPort = wsContainer.getMappedPort(7071);
        }
        if (config.sseEnabled()) {
            sseContainer = TestContainersEnvironmentFactory.getClusteredSSEContainer(NODES, network);
            sseContainer.start();
            baseSseUri = "http://"+sseContainer.getHost();
            ssePort = sseContainer.getMappedPort(7072);
        }
        if (config.httpNearCacheEnabled()){
            httpNearContainer = TestContainersEnvironmentFactory.getClusteredHTTPNearContainer(NODES, network);
            httpNearContainer.start();
            baseHttpNearUri = "http://"+httpNearContainer.getHost();
            httpNearPort = httpNearContainer.getMappedPort(7073);
        }

        var backendTestContainers = new BackendTestContainers(cacheNodes, httpContainer, httpNearContainer, wsContainer, sseContainer);
        return new BackendTestResource(baseHttpUri, httpPort, baseWsUri, wsPort, baseSseUri, ssePort, baseHttpNearUri,
                httpNearPort, functionalityKey, true, backendTestContainers);
    }

    private static @NotNull BackendTestResource setupNonClusteredEnvironment(BackendTestConfig config, Network network) {
        System.out.println("Starting AeronCache SingleNode...");
        GenericContainer<?> cacheNode = TestContainersEnvironmentFactory.getSingleNodeCacheContainer(network);
        List<GenericContainer<?>> cacheNodeList = new ArrayList<>();
        cacheNodeList.add(cacheNode);
        cacheNode.start();

        var baseHttpUri = "http://localhost";
        var baseHttpNearUri = "http://localhost";
        var baseWsUri = "ws://localhost";
        var baseSseUri = "http://localhost";
        int httpPort = 0;
        int httpNearPort = 0;
        int wsPort = 0;
        int ssePort = 0;

        GenericContainer<?> httpContainer = null;
        GenericContainer<?> httpNearContainer = null;
        GenericContainer<?> wsContainer = null;
        GenericContainer<?> sseContainer = null;

        if (config.httpEnabled()) {
            httpContainer = TestContainersEnvironmentFactory.getSingleNodeHTTPContainer(network);
            httpContainer.start();
            baseHttpUri = "http://"+httpContainer.getHost();
            httpPort = httpContainer.getMappedPort(7070);
        }
        if (config.wsEnabled()) {
            wsContainer = TestContainersEnvironmentFactory.getSingleNodeWSContainer(network);
            wsContainer.start();
            baseWsUri = "ws://"+wsContainer.getHost();
            wsPort = wsContainer.getMappedPort(7071);
        }
        if (config.sseEnabled()) {
            sseContainer = TestContainersEnvironmentFactory.getSingleNodeSSEContainer(network);
            sseContainer.start();
            baseSseUri = "http://"+sseContainer.getHost();
            ssePort = sseContainer.getMappedPort(7072);
        }
        if (config.httpNearCacheEnabled()){
            httpNearContainer = TestContainersEnvironmentFactory.getClusteredHTTPNearContainer(3, network);
            httpNearContainer.start();
            baseHttpNearUri = "http://"+httpNearContainer.getHost();
            httpNearPort = httpNearContainer.getMappedPort(7073);
        }

        var backendTestContainers = new BackendTestContainers(cacheNodeList, httpContainer, httpNearContainer, wsContainer, sseContainer);
        return new BackendTestResource(baseHttpUri, httpPort, baseWsUri, wsPort, baseSseUri, ssePort, baseHttpNearUri,
                httpNearPort, functionalityKey, true, backendTestContainers);
    }

    /**
     * Create an embedded backend environment.
     *
     * @param config
     * @return
     */
    @NotNull
    private static BackendTestResource getEmbeddedBackendTestResource(BackendTestConfig config) {
        if(config.useClusteredMode()){
            System.out.println("Starting AeronCache Cluster...");
            ClusterLauncher.launchTestCluster(3, functionalityKey);
        }
        else{
            System.out.println("Starting AeronCache Singlenode...");

        }

        var baseHttpUri = "http://localhost";
        var baseWsUri = "ws://localhost";
        int httpPort = 0;
        int wsPort = 0;
        int ssePort = 0;

        if (config.httpEnabled()) {
            httpPort = HttpApplication.startHTTPInterface(0, config.useClusteredMode());
        }
        if (config.wsEnabled()) {
            wsPort = WebsocketApplication.startWebsocketInterface(0);
        }
        if (config.sseEnabled()) {
            ssePort = SSEApplication.startSSEInterface(null, 0);
        }

        return new BackendTestResource(baseHttpUri, httpPort, baseWsUri, wsPort, baseHttpUri, ssePort,
                null, 0, functionalityKey, false, null);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter()
                .getType()
                .equals(BackendTestResource.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return extensionContext.getStore(NAMESPACE)
                .get(functionalityKey, BackendTestResource.class);
    }

    /**
     * Build a functionality key indicating the combination of backend functionality this environment is initialized for.
     *
     * @param config
     * @return
     */
    private static String getFunctionalityKey(BackendTestConfig config) {
        var functionalityKeyBuilder = new StringBuilder(BACKEND_KEY + "_http");

        if (config.wsEnabled()) {
            functionalityKeyBuilder.append("_ws");
        }
        if (config.sseEnabled()) {
            functionalityKeyBuilder.append("_sse");
        }

        return functionalityKeyBuilder.toString();
    }

}
