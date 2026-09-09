package com.bhf.aeroncache.integration.gateway;

import com.bhf.aeroncache.application.ephemeral.EphemeralCacheApplication;
import com.bhf.aeroncache.gateway.application.GatewayApplication;
import com.bhf.aeroncache.utils.DNSUtils;
import org.junit.jupiter.api.DisplayName;

/**
 * Full-stack embedded end-to-end test for the Aeron gateway running against an <em>ephemeral</em>
 * (unclustered) cache.
 * <p>
 * Mirrors {@link GatewayEndToEndTest} but replaces the in-process RAFT cluster with a single
 * in-process {@link EphemeralCacheApplication}. The gateway is started in unclustered mode via
 * {@link GatewayApplication#start(int, boolean)} so it connects directly to the ephemeral cache over
 * the unclustered request/response Aeron channels (the gateway reuses the websocket channel ports
 * {@code 7008}/{@code 7007}).
 * <p>
 * The shared client harness and the streaming/subscription/CRUD-read suite live in
 * {@link AbstractGatewayEndToEndTest}; this class only stands up the ephemeral backend. Both the
 * gateway and the ephemeral cache key their endpoints off {@link DNSUtils#getThisHostName()}, so they
 * always agree on the request/response hosts regardless of the machine they run on.
 */
@DisplayName("Gateway embedded end-to-end (ephemeral cache)")
class GatewayEphemeralEndToEndTest extends AbstractGatewayEndToEndTest {

    @Override
    protected void startBackend() throws Exception {
        // The gateway (response subscription) and the ephemeral cache (request subscriptions/response
        // publications) both key their endpoints off this host, so they agree on any machine.
        var host = DNSUtils.getThisHostName();

        // Start the ephemeral cache first. Its service agent's onStart blocks (on the agent thread)
        // until the gateway's response subscription connects, so this returns promptly.
        EphemeralCacheApplication.start(host, host, host, host, false);

        // Start the real gateway in unclustered mode: it launches its own embedded media driver and
        // connects to the ephemeral cache over the unclustered request/response channels.
        GatewayApplication.start(0, false);
    }
}
