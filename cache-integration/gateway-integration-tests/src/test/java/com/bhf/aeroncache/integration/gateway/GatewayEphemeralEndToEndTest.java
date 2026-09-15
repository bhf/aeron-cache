package com.bhf.aeroncache.integration.gateway;

import com.bhf.aeroncache.application.ephemeral.EphemeralCacheApplication;
import com.bhf.aeroncache.gateway.application.GatewayApplication;
import org.junit.jupiter.api.DisplayName;

/**
 * Full-stack embedded end-to-end test for the Aeron gateway running against an <em>ephemeral</em>
 * (unclustered) cache.
 * <p>
 * Mirrors {@link GatewayEndToEndTest} but replaces the in-process RAFT cluster with a single
 * in-process {@link EphemeralCacheApplication}. The gateway is started in unclustered mode via
 * {@link GatewayApplication#start(int, boolean)} so it connects directly to the ephemeral cache over
 * Aeron response channels ({@code control-mode=response}), dialing the cache's request/response-control
 * endpoints ({@code localhost:8075}/{@code localhost:8076}) via the shared response-channel connector.
 * <p>
 * The shared client harness and the streaming/subscription/CRUD-read suite live in
 * {@link AbstractGatewayEndToEndTest}; this class only stands up the ephemeral backend.
 */
@DisplayName("Gateway embedded end-to-end (ephemeral cache)")
class GatewayEphemeralEndToEndTest extends AbstractGatewayEndToEndTest {

    @Override
    protected void startBackend() throws Exception {
        // Start the ephemeral cache first. It binds a single request subscription and advertises a
        // control-mode=response control endpoint; the gateway (as an unclustered client) dials these
        // via the ResponseChannelCacheConnector defaults (localhost:8075 / localhost:8076). Response
        // publications are created on demand as the gateway connects, so this returns promptly.
        EphemeralCacheApplication.start("0.0.0.0:8075", "localhost:8076", 200, 201, false);

        // Start the real gateway in unclustered mode: it launches its own embedded media driver and
        // connects to the ephemeral cache over the response-channel request/response channels.
        GatewayApplication.start(0, false);
    }
}
