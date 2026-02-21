package com.bhf.aeroncache.ws.config;

import com.bhf.aeroncache.ws.application.WebsocketApplication;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.YieldingIdleStrategy;

import java.util.function.Supplier;

/**
 * All {@link org.agrona.concurrent.IdleStrategy} implementations used
 * for the {@link WebsocketApplication}.
 */
public class WsIdleStrategies {

    public static final Supplier<IdleStrategy> clusterClientAgentIdleStrategy = BackoffIdleStrategy::new;
    public static final Supplier<IdleStrategy> clusterMessagePublisherIdleStrategy = BusySpinIdleStrategy::new;
    public static final Supplier<IdleStrategy> agentRunnerIdleStrategy = YieldingIdleStrategy::new;
    public static final Supplier<IdleStrategy> unclusteredIdleStrategy = BackoffIdleStrategy::new;

}
