package com.bhf.aeroncache.sse.config;

import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.YieldingIdleStrategy;

import java.util.function.Supplier;

public class SSEIdleStrategies {

    public static final Supplier<IdleStrategy> clusterClientAgentIdleStrategy = BackoffIdleStrategy::new;
    public static final Supplier<IdleStrategy> clusterMessagePublisherIdleStrategy = BusySpinIdleStrategy::new;
    public static final Supplier<IdleStrategy> agentRunnerIdleStrategy = YieldingIdleStrategy::new;
    public static final Supplier<IdleStrategy> unclusteredIdleStrategy = BackoffIdleStrategy::new;

}
