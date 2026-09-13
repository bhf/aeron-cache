package com.bhf.aeroncache.http.config;

import com.bhf.aeroncache.utils.IdleStrategyConfig;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

import java.util.function.Supplier;

/**
 * All {@link org.agrona.concurrent.IdleStrategy} implementations used for the {@link com.bhf.aeroncache.http.application.NearCacheApplication}.
 * <p>
 * Each strategy is configurable via a role specific environment variable, falling back to the
 * application wide {@link IdleStrategyConfig#IDLE_STRATEGY_ENV} variable and finally to a
 * {@link BackoffIdleStrategy}. See {@link IdleStrategyConfig} for the accepted values.
 */
public class HttpNearCacheIdleStrategies {

    public static final Supplier<IdleStrategy> blockingPublisherIdleStrategy =
            IdleStrategyConfig.supplier(BackoffIdleStrategy::new, "IDLE_STRATEGY_BLOCKING_PUBLISHER", IdleStrategyConfig.IDLE_STRATEGY_ENV);
    public static final Supplier<IdleStrategy> clusterClientAgentIdleStrategy =
            IdleStrategyConfig.supplier(BackoffIdleStrategy::new, "IDLE_STRATEGY_CLUSTER_CLIENT_AGENT", IdleStrategyConfig.IDLE_STRATEGY_ENV);
    public static final Supplier<IdleStrategy> clusterMessagePublisherIdleStrategy =
            IdleStrategyConfig.supplier(BackoffIdleStrategy::new, "IDLE_STRATEGY_CLUSTER_MESSAGE_PUBLISHER", IdleStrategyConfig.IDLE_STRATEGY_ENV);
    public static final Supplier<IdleStrategy> agentRunnerIdleStrategy =
            IdleStrategyConfig.supplier(BackoffIdleStrategy::new, "IDLE_STRATEGY_AGENT_RUNNER", IdleStrategyConfig.IDLE_STRATEGY_ENV);
    public static final Supplier<IdleStrategy> unclusteredAgentIdleStrategy =
            IdleStrategyConfig.supplier(BackoffIdleStrategy::new, "IDLE_STRATEGY_UNCLUSTERED", IdleStrategyConfig.IDLE_STRATEGY_ENV);

}
