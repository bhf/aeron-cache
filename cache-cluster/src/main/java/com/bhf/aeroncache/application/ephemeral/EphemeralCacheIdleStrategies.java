package com.bhf.aeroncache.application.ephemeral;

import com.bhf.aeroncache.utils.IdleStrategyConfig;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

import java.util.function.Supplier;

/**
 * {@link org.agrona.concurrent.IdleStrategy} implementations used for the ephemeral cache application.
 * <p>
 * Configurable via a role specific environment variable, falling back to the application wide
 * {@link IdleStrategyConfig#IDLE_STRATEGY_ENV} variable and finally to a {@link BackoffIdleStrategy}.
 * See {@link IdleStrategyConfig} for the accepted values.
 */
public class EphemeralCacheIdleStrategies {

    public static final Supplier<IdleStrategy> unclusteredAgentIdleStrategy =
            IdleStrategyConfig.supplier(BackoffIdleStrategy::new, "IDLE_STRATEGY_UNCLUSTERED", IdleStrategyConfig.IDLE_STRATEGY_ENV);
}
