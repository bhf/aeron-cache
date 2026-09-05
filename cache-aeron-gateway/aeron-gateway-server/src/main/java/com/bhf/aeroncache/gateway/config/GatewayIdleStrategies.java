package com.bhf.aeroncache.gateway.config;

import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.YieldingIdleStrategy;

import java.util.function.Supplier;

/**
 * All {@link org.agrona.concurrent.IdleStrategy} implementations used
 * for the {@link com.bhf.aeroncache.gateway.application.GatewayApplication}.
 */
public class GatewayIdleStrategies {

    public static final Supplier<IdleStrategy> clusterClientAgentIdleStrategy = BackoffIdleStrategy::new;
    public static final Supplier<IdleStrategy> clusterMessagePublisherIdleStrategy = BusySpinIdleStrategy::new;
    public static final Supplier<IdleStrategy> agentRunnerIdleStrategy = YieldingIdleStrategy::new;
    public static final Supplier<IdleStrategy> ingressAgentIdleStrategy = BackoffIdleStrategy::new;
    public static final Supplier<IdleStrategy> unclusteredIdleStrategy = BackoffIdleStrategy::new;

}
