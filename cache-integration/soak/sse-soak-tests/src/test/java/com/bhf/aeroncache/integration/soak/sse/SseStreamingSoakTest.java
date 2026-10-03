package com.bhf.aeroncache.integration.soak.sse;

import com.bhf.aeroncache.application.ClusterLauncher;
import com.bhf.aeroncache.gateway.application.GatewayApplication;
import com.bhf.aeroncache.gateway.client.GatewayClient;
import com.bhf.aeroncache.integration.soak.common.AbstractGatewaySoakTest;
import com.bhf.aeroncache.integration.soak.common.SoakRecordingListener;
import com.bhf.aeroncache.integration.soak.common.SoakRun;
import com.bhf.aeroncache.integration.soak.common.UrlStreamingSoakConfig;
import com.bhf.aeroncache.integration.soak.common.UrlStreamingSoakWorkload;
import com.bhf.aeroncache.sse.application.SSEApplication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;

/**
 * SSE soak test: cycles the full subscription coverage matrix over the SSE routes ({@code /api/sse/v1/...})
 * against a real in-process 3-node RAFT cluster. Mutations are applied over the binary Aeron
 * {@link GatewayClient} (via {@link AbstractGatewaySoakTest}); the subscription under test is observed over a
 * real {@code EventSource}. The matrix engine is the shared {@link UrlStreamingSoakWorkload}; this class only
 * stands up the backend (cluster + SBE gateway for mutations + SSE server for subscriptions) and wires the SSE
 * subscriber factory.
 *
 * <p>Manually triggered via the {@code Soak SSE} workflow; excluded from the normal push/PR CI.
 */
@DisplayName("SSE soak (embedded clustered)")
class SseStreamingSoakTest extends AbstractGatewaySoakTest {

    private static final Logger log = LogManager.getLogger(SseStreamingSoakTest.class);

    private static final int CLUSTER_NODES = 3;
    // A one-directional SSE subscriber receives each counter update exactly once (as over streaming WS); the
    // double-delivery the bidi soak models is specific to the bidi handler.
    private static final boolean COUNTER_STREAMS_TWICE = false;

    private int ssePort;
    private SseSoakStreamSubscriberFactory factory;

    @Override
    protected void startBackend() throws Exception {
        ClusterLauncher.launchTestCluster(CLUSTER_NODES, "soak_sse");
        GatewayApplication.start(0); // SBE gateway: the mutation path for the GatewayClient.
        ssePort = SSEApplication.startSSEInterface(null, 0); // SSE server: the subscription path under test.
        log.info("SSE server listening on port {}", ssePort);
    }

    @Override
    protected SoakRun createRun(GatewayClient client, SoakRecordingListener listener) {
        factory = new SseSoakStreamSubscriberFactory(ssePort);
        return new UrlStreamingSoakWorkload(client, listener, factory,
                UrlStreamingSoakConfig.fromSystemProperties(), COUNTER_STREAMS_TWICE, "soak-sse-strm-");
    }

    @Override
    protected void stopClient() {
        if (factory != null) {
            factory.close();
        }
        super.stopClient();
    }
}
