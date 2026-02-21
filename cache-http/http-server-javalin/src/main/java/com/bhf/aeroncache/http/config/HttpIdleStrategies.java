package com.bhf.aeroncache.http.config;

import com.bhf.aeroncache.http.application.HttpApplication;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

import java.util.function.Supplier;

/**
 * All {@link org.agrona.concurrent.IdleStrategy} implementations used for the {@link HttpApplication}.
 */
public class HttpIdleStrategies {

    public static final Supplier<IdleStrategy> blockingPublisherIdleStrategy = BusySpinIdleStrategy::new;
    public static final Supplier<IdleStrategy> clusterClientAgentIdleStrategy = BackoffIdleStrategy::new;
    public static final Supplier<IdleStrategy> clusterMessagePublisherIdleStrategy = BusySpinIdleStrategy::new;
    public static final Supplier<IdleStrategy> agentRunnerIdleStrategy = BackoffIdleStrategy::new;
    public static final Supplier<IdleStrategy> unclusteredIdleStrategy = BackoffIdleStrategy::new;

}

