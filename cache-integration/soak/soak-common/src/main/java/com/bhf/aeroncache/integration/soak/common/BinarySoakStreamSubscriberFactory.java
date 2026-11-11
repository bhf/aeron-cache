package com.bhf.aeroncache.integration.soak.common;

import com.bhf.aeroncache.gateway.client.GatewayClient;

/**
 * Opens {@link BinarySoakStreamSubscriber}s over the binary Aeron transport, so the binary streaming soak drives
 * the same shared {@link StreamingSoakWorkload} matrix engine as the URL transports (WS/SSE). Unlike those, the
 * subscription is observed on the very {@link GatewayClient}/{@link SoakRecordingListener} the mutations flow
 * over - there is no separate connection - so one factory instance serves the whole run.
 */
public final class BinarySoakStreamSubscriberFactory implements SoakStreamSubscriberFactory {

    private final GatewayClient client;
    private final SoakRecordingListener listener;

    public BinarySoakStreamSubscriberFactory(GatewayClient client, SoakRecordingListener listener) {
        this.client = client;
        this.listener = listener;
    }

    @Override
    public SoakStreamSubscriber open(SubscriptionSpec spec) {
        return new BinarySoakStreamSubscriber(client, listener, spec);
    }
}
