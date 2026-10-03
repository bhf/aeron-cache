package com.bhf.aeroncache.integration.soak.bidi;

import com.bhf.aeroncache.application.ClusterLauncher;
import com.bhf.aeroncache.integration.soak.common.AbstractSoakTest;
import com.bhf.aeroncache.integration.soak.common.SoakRun;
import com.bhf.aeroncache.ws.application.WebsocketApplication;
import com.bhf.aeroncache.ws.bidi.messages.WsOp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Bidi-websocket soak test: a long-running, single-writer driver that exercises both halves of the bidi
 * protocol - the command surface (reconciled against a {@code SoakOracle}) and the subscription/streaming
 * matrix - over the real {@link WebsocketApplication} fronting a real in-process 3-node RAFT cluster, driven
 * over actual websockets. The WS server is its own cluster client (no SBE gateway needed). The transport-neutral
 * lifecycle and the always-written metrics summary live in {@link AbstractSoakTest}; the backend and the two
 * bidi connections are stood up here and the workload itself lives in {@link BidiSoakWorkload}.
 *
 * <p>Manually triggered via the {@code Soak Bidi WS} workflow; excluded from the normal push/PR CI.
 */
@DisplayName("Bidi websocket soak (embedded clustered)")
class BidiSoakTest extends AbstractSoakTest {

    private static final Logger log = LogManager.getLogger(BidiSoakTest.class);

    private static final int CLUSTER_NODES = 3;
    private static final String BIDI_PATH = "/api/ws/v1/bidi";
    private static final String WARMUP_CACHE = "__bidi_soak_warmup__";
    private static final long WARMUP_TIMEOUT_SECONDS = 120;
    private static final long PARK_NANOS = 250_000_000L;

    private int wsPort;
    private BidiSoakClient mutator;
    private BidiSoakClient subscriber;

    @Override
    protected void startBackend() throws Exception {
        ClusterLauncher.launchTestCluster(CLUSTER_NODES, "soak_bidi_ws");
        // The WS server connects to the cluster directly as its own client (CLUSTER_ADDRESSES / embedded
        // media driver), so no SBE GatewayApplication is required. Port 0 => an ephemeral bound port.
        wsPort = WebsocketApplication.startWebsocketInterface(0);
        log.info("Bidi websocket server listening on port {}", wsPort);
    }

    @Override
    protected void startClient() {
        var uri = "ws://localhost:" + wsPort + BIDI_PATH;
        mutator = new BidiSoakClient().connect(uri);
        subscriber = new BidiSoakClient().connect(uri);
        warmUp();
    }

    @Override
    protected void stopClient() {
        if (mutator != null) {
            mutator.close();
        }
        if (subscriber != null) {
            subscriber.close();
        }
    }

    @Override
    protected SoakRun createRun() {
        return new BidiSoakWorkload(mutator, subscriber, BidiSoakConfig.fromSystemProperties());
    }

    /**
     * The WS server returns from {@code startWebsocketInterface} once it is listening, but its cluster
     * connection may still be establishing. Probe with a create/delete round-trip until the cluster answers,
     * so the timed workload starts against a ready backend (mirrors the gateway soak's connection warm-up).
     */
    private void warmUp() {
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WARMUP_TIMEOUT_SECONDS);
        while (true) {
            var corr = "warmup-" + System.nanoTime();
            mutator.command(WsOp.CREATE_CACHE, corr, WARMUP_CACHE, null, null, 0, 0);
            var response = pollResponse(corr, 2);
            if (response != null && ("SUCCESS".equals(response.status()) || "CACHE_EXISTS".equals(response.status()))) {
                mutator.command(WsOp.DELETE_CACHE, "warmup-del-" + System.nanoTime(), WARMUP_CACHE, null, null, 0, 0);
                pollResponse(null, 1);
                // Clear anything left from warm-up so the workload starts from a clean slate.
                mutator.commandResponses.clear();
                mutator.errors.clear();
                subscriber.errors.clear();
                log.info("Bidi backend ready after warm-up");
                return;
            }
            if (System.nanoTime() > deadline) {
                throw new IllegalStateException("Bidi backend not ready after " + WARMUP_TIMEOUT_SECONDS
                        + "s (last warm-up status " + (response == null ? "<no response>" : response.status()) + ")");
            }
            LockSupport.parkNanos(PARK_NANOS);
        }
    }

    private BidiSoakClient.CommandResponse pollResponse(String corr, long withinSeconds) {
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(withinSeconds);
        while (System.nanoTime() < deadline) {
            if (corr != null) {
                var r = mutator.commandResponses.remove(corr);
                if (r != null) {
                    return r;
                }
            }
            LockSupport.parkNanos(PARK_NANOS);
        }
        return null;
    }
}
