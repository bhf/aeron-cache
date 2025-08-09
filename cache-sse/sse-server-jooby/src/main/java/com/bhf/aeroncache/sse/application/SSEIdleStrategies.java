package com.bhf.aeroncache.sse.application;

import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.YieldingIdleStrategy;

public class SSEIdleStrategies {
    public static final IdleStrategy blockingPublisherIdleStrategy = new BusySpinIdleStrategy();
    public static final IdleStrategy clusterClientAgentIdleStrategy = new BackoffIdleStrategy();
    public static final IdleStrategy clusterMessagePublisherIdleStrategy = new BusySpinIdleStrategy();
    public static final IdleStrategy agentRunnerIdleStrategy = new YieldingIdleStrategy();
    public static final IdleStrategy unclusteredAgentIdleStrategy = new BusySpinIdleStrategy();
}
