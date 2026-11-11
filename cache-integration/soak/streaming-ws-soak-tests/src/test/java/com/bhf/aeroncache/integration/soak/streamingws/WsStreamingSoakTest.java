package com.bhf.aeroncache.integration.soak.streamingws;

import com.bhf.aeroncache.application.ClusterLauncher;
import com.bhf.aeroncache.gateway.application.GatewayApplication;
import com.bhf.aeroncache.gateway.client.GatewayClient;
import com.bhf.aeroncache.integration.soak.common.AbstractGatewaySoakTest;
import com.bhf.aeroncache.integration.soak.common.HttpNearCacheAdmin;
import com.bhf.aeroncache.integration.soak.common.SoakRecordingListener;
import com.bhf.aeroncache.integration.soak.common.SoakRun;
import com.bhf.aeroncache.integration.soak.common.StreamingSoakConfig;
import com.bhf.aeroncache.integration.soak.common.StreamingSoakWorkload;
import com.bhf.aeroncache.ws.application.WebsocketApplication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;

/**
 * Streaming-WebSocket soak test: cycles the full subscription coverage matrix over the one-directional streaming
 * WS routes ({@code /api/ws/v1/...}) against a real in-process 3-node RAFT cluster. Mutations are applied over
 * the binary Aeron {@link GatewayClient} (via {@link AbstractGatewaySoakTest}); the subscription under test is
 * observed over actual websockets. The matrix engine is the shared {@link StreamingSoakWorkload}; this class
 * only stands up the backend (cluster + SBE gateway for mutations + WS server for subscriptions) and wires the
 * WS subscriber factory.
 *
 * <p>Manually triggered via the {@code Soak Streaming WS} workflow; excluded from the normal push/PR CI.
 */
@DisplayName("Streaming WebSocket soak (embedded clustered)")
class WsStreamingSoakTest extends AbstractGatewaySoakTest {

    private static final Logger log = LogManager.getLogger(WsStreamingSoakTest.class);

    private static final int CLUSTER_NODES = 3;
    // A one-directional WS subscriber receives each counter update exactly once (observed): the double-delivery
    // the bidi soak models is specific to the bidi handler reusing the counter result as its own broadcast.
    private static final boolean COUNTER_STREAMS_TWICE = false;

    private int wsPort;
    private WsSoakStreamSubscriberFactory factory;

    @Override
    protected void startBackend() throws Exception {
        ClusterLauncher.launchTestCluster(CLUSTER_NODES, "soak_stream_ws");
        GatewayApplication.start(0); // SBE gateway: the mutation path for the GatewayClient.
        wsPort = WebsocketApplication.startWebsocketInterface(0); // WS server: the subscription path under test.
        log.info("Streaming WS server listening on port {}", wsPort);
    }

    @Override
    protected SoakRun createRun(GatewayClient client, SoakRecordingListener listener) {
        factory = new WsSoakStreamSubscriberFactory(wsPort);
        // Priming and the synthetic-patch metric live on the WS server (the edge that serves the subscription
        // and holds the warm copy); mutations still flow over the SBE GatewayClient through the cluster.
        var nearAdmin = new HttpNearCacheAdmin("http://localhost:" + wsPort, "aeroncache_near_patches_total", true);
        return new StreamingSoakWorkload(client, listener, factory, nearAdmin,
                StreamingSoakConfig.fromSystemProperties(), COUNTER_STREAMS_TWICE, "soak-ws-strm-");
    }

    @Override
    protected void stopClient() {
        if (factory != null) {
            factory.close();
        }
        super.stopClient();
    }
}
