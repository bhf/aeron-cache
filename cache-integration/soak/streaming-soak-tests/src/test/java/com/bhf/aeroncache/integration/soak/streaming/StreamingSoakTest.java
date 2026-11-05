package com.bhf.aeroncache.integration.soak.streaming;

import com.bhf.aeroncache.application.ClusterLauncher;
import com.bhf.aeroncache.clustertools.counters.AeronCountersPrometheusFormatter;
import com.bhf.aeroncache.clustertools.counters.AeronCountersReader;
import com.bhf.aeroncache.gateway.application.GatewayApplication;
import com.bhf.aeroncache.gateway.client.GatewayClient;
import com.bhf.aeroncache.integration.soak.common.AbstractGatewaySoakTest;
import com.bhf.aeroncache.integration.soak.common.HttpNearCacheAdmin;
import com.bhf.aeroncache.integration.soak.common.SoakRecordingListener;
import com.bhf.aeroncache.integration.soak.common.SoakRun;
import org.junit.jupiter.api.DisplayName;

import java.util.function.Supplier;

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
    private String gatewayAeronDir;

    @Override
    protected void startBackend() throws Exception {
        ClusterLauncher.launchTestCluster(CLUSTER_NODES, "soak_stream");
        // Capture the gateway's HTTP port (ephemeral): the near-cache prime/list endpoints are served there, so
        // the warm synthetic-patch rounds can prime a cache before subscribing.
        gatewayHttpPort = GatewayApplication.start(0);
        // Capture the gateway's Aeron media driver directory. Its synthetic-patch counters are Aeron driver
        // counters (absent from the gateway's own Micrometer /prometheus), so the warm rounds read them in
        // process straight from this driver's CnC file via the clustertool's reader/formatter - the same code
        // the clustertool sidecar runs in production - which is what lets the counter be asserted here.
        gatewayAeronDir = GatewayApplication.gatewayAeronDirectoryName();
    }

    @Override
    protected SoakRun createRun(GatewayClient client, SoakRecordingListener listener) {
        // Management (prime/list/stop) over the gateway's HTTP port; the per-outcome synthetic-patch counter is
        // read from the gateway media driver's CnC file via the clustertool reader/formatter, so the warm rounds
        // assert aeron_gateway_near_patches_total{domain="cache",outcome} as well as the synthetic stream events.
        Supplier<String> clusterToolsScrape =
                () -> AeronCountersPrometheusFormatter.format(AeronCountersReader.read(gatewayAeronDir));
        var nearAdmin = new HttpNearCacheAdmin("http://localhost:" + gatewayHttpPort,
                "aeron_gateway_near_patches_total", clusterToolsScrape);
        return new StreamingSoakWorkload(client, listener, nearAdmin, StreamingSoakConfig.fromSystemProperties());
    }
}
