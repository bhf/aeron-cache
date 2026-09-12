package com.bhf.aeroncache.integration.gateway;

import com.bhf.aeroncache.application.ClusterLauncher;
import com.bhf.aeroncache.gateway.application.GatewayApplication;
import org.junit.jupiter.api.DisplayName;

/**
 * Full-stack embedded end-to-end test for the Aeron gateway backed by a real in-process RAFT cluster.
 * <p>
 * Mirrors the in-process wiring of {@code cache-monolith}'s {@code Main.java}: an embedded media
 * driver, a real in-process RAFT cluster ({@link ClusterLauncher#launchTestCluster}), and the real
 * {@link GatewayApplication} server edge, driven by a real {@link com.bhf.aeroncache.gateway.client.GatewayClient}.
 * <p>
 * The entire streaming/subscription/CRUD command suite lives in {@link AbstractGatewayEndToEndTest} and
 * runs identically against both this clustered backend and the ephemeral backend
 * ({@link GatewayEphemeralEndToEndTest}); this class only stands up the clustered backend.
 * <p>
 * Endpoints and cluster addresses are supplied via {@code src/test/resources/test.env} (loaded into
 * the test JVM environment by the build). The gateway binds {@code localhost:7075} (request) and
 * {@code localhost:7076} (response control) so the in-process client can reach it deterministically.
 */
@DisplayName("Gateway embedded end-to-end")
class GatewayEndToEndTest extends AbstractGatewayEndToEndTest {

    private static final int CLUSTER_NODES = 3;

    @Override
    protected void startBackend() throws Exception {
        // Start the in-process RAFT cluster (each node launches its own media driver).
        ClusterLauncher.launchTestCluster(CLUSTER_NODES, "gateway_e2e");

        // Start the real gateway: it launches its own embedded media driver and connects to the
        // cluster, standing up the ingress on the endpoints configured in test.env.
        GatewayApplication.start(0);
    }
}
