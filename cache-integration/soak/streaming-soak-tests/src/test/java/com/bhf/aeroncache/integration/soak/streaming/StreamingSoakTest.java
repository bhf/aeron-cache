package com.bhf.aeroncache.integration.soak.streaming;

import com.bhf.aeroncache.application.ClusterLauncher;
import com.bhf.aeroncache.gateway.application.GatewayApplication;
import com.bhf.aeroncache.gateway.client.GatewayClient;
import com.bhf.aeroncache.integration.soak.common.AbstractGatewaySoakTest;
import com.bhf.aeroncache.integration.soak.common.HttpNearCacheAdmin;
import com.bhf.aeroncache.integration.soak.common.SoakRecordingListener;
import com.bhf.aeroncache.integration.soak.common.SoakRun;
import org.junit.jupiter.api.DisplayName;

/**
 * Streaming soak test: a long-running, single-writer driver that verifies the gateway's
 * subscription/streaming behaviour across the full matrix of subscription variations (cache kind, mode,
 * scope, hydration, cardinality) against a real in-process 3-node RAFT cluster fronted by the real
 * {@link GatewayApplication}, driven over Aeron by a real {@link GatewayClient}. The lifecycle lives in
 * {@link AbstractGatewaySoakTest}; the coverage matrix and expectation model live in
 * {@link StreamingSoakWorkload}; this class only stands up the clustered backend and wires the workload.
 *
 * <p>Manually triggered via the {@code Soak Streaming} workflow; excluded from the normal push/PR CI.
 */
@DisplayName("Streaming soak (embedded clustered)")
class StreamingSoakTest extends AbstractGatewaySoakTest {

    private static final int CLUSTER_NODES = 3;

    private int gatewayHttpPort;

    @Override
    protected void startBackend() throws Exception {
        ClusterLauncher.launchTestCluster(CLUSTER_NODES, "soak_stream");
        // Capture the gateway's HTTP port (ephemeral): the near-cache prime/list endpoints are served there, so
        // the warm synthetic-patch rounds can prime a cache before subscribing. (The gateway's synthetic-patch
        // counters are Aeron driver counters, not on its Micrometer /prometheus, so those rounds verify the
        // synthetic stream events rather than the counter - see HttpNearCacheAdmin patchMetricsExposed=false.)
        gatewayHttpPort = GatewayApplication.start(0);
    }

    @Override
    protected SoakRun createRun(GatewayClient client, SoakRecordingListener listener) {
        var nearAdmin = new HttpNearCacheAdmin("http://localhost:" + gatewayHttpPort, "aeron_gateway_near_patches_total", false);
        return new StreamingSoakWorkload(client, listener, nearAdmin, StreamingSoakConfig.fromSystemProperties());
    }
}
