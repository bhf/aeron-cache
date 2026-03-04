package com.bhf.aeroncache.integration;

import com.bhf.aeroncache.application.ClusterLauncher;
import com.bhf.aeroncache.http.application.HttpApplication;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.sse.application.SSEApplication;
import com.bhf.aeroncache.ws.application.WebsocketApplication;
import org.junit.jupiter.api.extension.*;

public class BackendTestLauncher implements BeforeAllCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(BackendTestLauncher.class);

    private static final String BACKEND_KEY = "backend";
    private static String functionalityKey;

    @Override
    public void beforeAll(ExtensionContext context) {

        var extensionContextRoot = context.getRoot();
        var extensionContextStore = extensionContextRoot.getStore(NAMESPACE);

        BackendTestConfig config =
                context.getRequiredTestClass()
                        .getAnnotation(BackendTestConfig.class);

        var functionalityKeyBuilder = new StringBuilder(BACKEND_KEY + "_http");

        if (config.wsEnabled()) {
            functionalityKeyBuilder.append("_ws");
        }
        if (config.sseEnabled()) {
            functionalityKeyBuilder.append("_sse");
        }

        functionalityKey = functionalityKeyBuilder.toString();

        extensionContextStore.getOrComputeIfAbsent(functionalityKey, key -> {
            try {
                System.out.println("Starting AeronCache Cluster...");
                ClusterLauncher.launchTestCluster(3, functionalityKey);

                var baseUri = "http://localhost";
                int httpPort = 0;
                int wsPort = 0;
                int ssePort = 0;

                if (config.httpEnabled()) {
                    httpPort = HttpApplication.startHTTPInterface(0);
                }
                if (config.wsEnabled()) {
                    wsPort = WebsocketApplication.startWebsocketInterface(0);
                }
                if (config.sseEnabled()) {
                    ssePort = SSEApplication.startSSEInterface(null, 0);
                }

                return new BackendTestResource(baseUri, httpPort, baseUri, wsPort, baseUri, ssePort);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
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

}
