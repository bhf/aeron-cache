package com.bhf.aeroncache.gateway.metrics;

import io.aeron.Aeron;
import io.aeron.Counter;
import lombok.extern.log4j.Log4j2;

/**
 * A {@link GatewayMetrics} backed by counters in the Aeron media driver.
 * <p>
 * Registers:
 * <ul>
 *     <li>{@code SessionsConnected} - the number of clients currently connected (a gauge).</li>
 *     <li>{@code SessionsTotal} - the number of client connections since the gateway started.</li>
 *     <li>{@code StreamUpdatesPublished} - streaming subscription updates sent to clients.</li>
 *     <li>One {@code Requests-<type>} counter per {@link GatewayRequestType}.</li>
 * </ul>
 * The session counters are updated from the Aeron conductor thread so use atomic updates. Request counters are
 * written only by the gateway ingress thread, and the update counter only by the cluster egress thread, so each
 * uses an ordered increment.
 */
@Log4j2
public class AeronCounterGatewayMetrics implements GatewayMetrics {
    /**
     * Type id of the counters registered by this class. Values below 1000 are reserved by Aeron.
     */
    public static final int COUNTER_TYPE_ID = 2100;

    private static final String LABEL_PREFIX = "AeronCache-Gateway-";

    private final Counter sessionsConnected;
    private final Counter sessionsTotal;
    private final Counter streamUpdatesPublished;
    private final Counter[] requestCounters;

    /**
     * @param aeron  client connected to the media driver the counters are registered with.
     * @param nodeId identifier for this gateway, included in the counter labels.
     */
    public AeronCounterGatewayMetrics(Aeron aeron, String nodeId) {
        final String suffix = "-" + nodeId;
        sessionsConnected = aeron.addCounter(COUNTER_TYPE_ID, LABEL_PREFIX + "SessionsConnected" + suffix);
        sessionsTotal = aeron.addCounter(COUNTER_TYPE_ID, LABEL_PREFIX + "SessionsTotal" + suffix);
        streamUpdatesPublished = aeron.addCounter(COUNTER_TYPE_ID, LABEL_PREFIX + "StreamUpdatesPublished" + suffix);
        requestCounters = new Counter[GatewayRequestType.values().length];
        for (GatewayRequestType type : GatewayRequestType.values()) {
            requestCounters[type.ordinal()] =
                    aeron.addCounter(COUNTER_TYPE_ID, LABEL_PREFIX + "Requests-" + type.label() + suffix);
        }
        log.info("Registered {} Aeron gateway metrics counters for {}", requestCounters.length + 3, nodeId);
    }

    @Override
    public void onSessionConnected() {
        sessionsConnected.increment();
        sessionsTotal.increment();
    }

    @Override
    public void onSessionDisconnected() {
        sessionsConnected.decrement();
    }

    @Override
    public void onRequest(GatewayRequestType requestType) {
        requestCounters[requestType.ordinal()].incrementOrdered();
    }

    @Override
    public void onStreamUpdatePublished() {
        streamUpdatesPublished.incrementOrdered();
    }
}
