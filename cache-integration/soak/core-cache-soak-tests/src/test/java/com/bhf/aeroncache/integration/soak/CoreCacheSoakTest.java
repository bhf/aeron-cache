package com.bhf.aeroncache.integration.soak;

import com.bhf.aeroncache.application.ClusterLauncher;
import com.bhf.aeroncache.gateway.application.GatewayApplication;
import org.junit.jupiter.api.DisplayName;

/**
 * Core cache soak test: a long-running, single-writer, randomised workload of key-value and counter
 * operations against a real in-process 3-node RAFT cluster fronted by the real {@link GatewayApplication},
 * driven over Aeron by a real {@link com.bhf.aeroncache.gateway.client.GatewayClient}. The entire workload
 * and correctness oracle live in {@link AbstractSoakTest}/{@link SoakWorkload}; this class only stands up
 * the clustered backend.
 *
 * <p>Manually triggered via the {@code Soak Core Cache} workflow; excluded from the normal push/PR CI.
 */
@DisplayName("Core cache soak (embedded clustered)")
class CoreCacheSoakTest extends AbstractSoakTest {

    private static final int CLUSTER_NODES = 3;

    @Override
    protected void startBackend() throws Exception {
        // Start the in-process RAFT cluster (each node launches its own media driver).
        ClusterLauncher.launchTestCluster(CLUSTER_NODES, "soak_core");

        // Start the real gateway: it launches its own embedded media driver, connects to the cluster and
        // stands up the ingress on the endpoints configured in test.env.
        GatewayApplication.start(0);
    }
}
