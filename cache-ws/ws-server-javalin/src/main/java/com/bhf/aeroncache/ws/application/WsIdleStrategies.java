package com.bhf.aeroncache.ws.application;

import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.YieldingIdleStrategy;

/**
 * All {@link org.agrona.concurrent.IdleStrategy} implementations used
 * for the {@link WebsocketApplication}.
 */
public class WsIdleStrategies {
    public static final IdleStrategy clusterClientAgentIdleStrategy = new BackoffIdleStrategy();
    public static final IdleStrategy clusterMessagePublisherIdleStrategy = new BusySpinIdleStrategy();
    public static final IdleStrategy agentRunnerIdleStrategy = new YieldingIdleStrategy();
    public static final IdleStrategy unclusteredAgentIdleStrategy = new BusySpinIdleStrategy();
}
