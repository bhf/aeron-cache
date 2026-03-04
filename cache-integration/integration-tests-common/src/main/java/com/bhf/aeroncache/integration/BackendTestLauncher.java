package com.bhf.aeroncache.integration;

import com.bhf.aeroncache.application.ClusterLauncher;
import com.bhf.aeroncache.http.application.HttpApplication;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import org.junit.jupiter.api.extension.*;

public class BackendTestLauncher implements BeforeAllCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(BackendTestLauncher.class);

    private static final String BACKEND_KEY = "backend";

    @Override
    public void beforeAll(ExtensionContext context) {

        ExtensionContext root = context.getRoot();
        ExtensionContext.Store store = root.getStore(NAMESPACE);

        BackendTestConfig config =
                context.getRequiredTestClass()
                        .getAnnotation(BackendTestConfig.class);

        store.getOrComputeIfAbsent(BACKEND_KEY, key -> {
            try {
                System.out.println("Starting backend once...");
                ClusterLauncher.launchCluster(3);

                HttpApplication.main(null);

                return new BackendTestResource(7070, "http://localhost");
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
                .get(BACKEND_KEY, BackendTestResource.class);
    }

}
