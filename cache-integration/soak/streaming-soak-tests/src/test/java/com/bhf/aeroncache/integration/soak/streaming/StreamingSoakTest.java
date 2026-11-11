package com.bhf.aeroncache.integration.soak.streaming;

import com.bhf.aeroncache.application.ClusterLauncher;
import com.bhf.aeroncache.clustertools.counters.AeronCountersPrometheusFormatter;
import com.bhf.aeroncache.clustertools.counters.AeronCountersReader;
import com.bhf.aeroncache.gateway.application.GatewayApplication;
import com.bhf.aeroncache.gateway.client.GatewayClient;
import com.bhf.aeroncache.integration.soak.common.AbstractGatewaySoakTest;
import com.bhf.aeroncache.integration.soak.common.BinarySoakStreamSubscriberFactory;
import com.bhf.aeroncache.integration.soak.common.HttpNearCacheAdmin;
import com.bhf.aeroncache.integration.soak.common.SoakRecordingListener;
import com.bhf.aeroncache.integration.soak.common.SoakRun;
import com.bhf.aeroncache.integration.soak.common.StreamingSoakConfig;
import com.bhf.aeroncache.integration.soak.common.StreamingSoakWorkload;
import org.junit.jupiter.api.DisplayName;

import java.util.function.Supplier;

/**
 * Streaming soak test: a long-running, single-writer driver that verifies the gateway's
 * subscription/streaming behaviour over the binary Aeron transport. The matrix engine is the shared
 * {@link StreamingSoakWorkload}; this class stands up the backend and wires the binary subscriber factory (the
 * subscription is observed on the same {@link com.bhf.aeroncache.gateway.client.GatewayClient} the mutations
 * flow over).
 */
@DisplayName("Streaming soak (embedded clustered)")
class StreamingSoakTest extends AbstractGatewaySoakTest {

    private static final int CLUSTER_NODES = 3;
    // Over the binary Aeron transport the gateway delivers each counter update to a session that both mutates and
    // subscribes TWICE (the cluster reuses the counter result as the subscriber broadcast); the URL transports
    // deliver it once.
    private static final boolean COUNTER_STREAMS_TWICE = true;

    private int gatewayHttpPort;
    private String gatewayAeronDir;

    @Override
    protected void startBackend() throws Exception {
        ClusterLauncher.launchTestCluster(CLUSTER_NODES, "soak_stream");
        gatewayHttpPort = GatewayApplication.start(0);
        gatewayAeronDir = GatewayApplication.gatewayAeronDirectoryName();
    }

    @Override
    protected SoakRun createRun(GatewayClient client, SoakRecordingListener listener) {
        Supplier<String> clusterToolsScrape =
                () -> AeronCountersPrometheusFormatter.format(AeronCountersReader.read(gatewayAeronDir));
        var nearAdmin = new HttpNearCacheAdmin("http://localhost:" + gatewayHttpPort,
                "aeron_gateway_near_patches_total", clusterToolsScrape);
        var factory = new BinarySoakStreamSubscriberFactory(client, listener);
        return new StreamingSoakWorkload(client, listener, factory, nearAdmin,
                StreamingSoakConfig.fromSystemProperties(), COUNTER_STREAMS_TWICE, "soak-strm-");
    }
}
