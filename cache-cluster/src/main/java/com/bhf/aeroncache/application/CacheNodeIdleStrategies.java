package com.bhf.aeroncache.application;

import com.bhf.aeroncache.utils.IdleStrategyConfig;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

import java.util.function.Supplier;

/**
 * {@link org.agrona.concurrent.IdleStrategy} implementations used for the clustered cache node
 * ({@link CacheNodeApplication}) - the Aeron consensus module and the clustered service.
 * <p>
 * Each strategy is configurable via a role specific environment variable, falling back to the
 * application wide {@link IdleStrategyConfig#IDLE_STRATEGY_ENV} variable and finally to a
 * {@link BackoffIdleStrategy}. See {@link IdleStrategyConfig} for the accepted values.
 */
public class CacheNodeIdleStrategies {

    public static final Supplier<IdleStrategy> consensusModuleIdleStrategy =
            IdleStrategyConfig.supplier(BackoffIdleStrategy::new, "IDLE_STRATEGY_CONSENSUS_MODULE", IdleStrategyConfig.IDLE_STRATEGY_ENV);
    public static final Supplier<IdleStrategy> clusteredServiceIdleStrategy =
            IdleStrategyConfig.supplier(BackoffIdleStrategy::new, "IDLE_STRATEGY_CLUSTERED_SERVICE", IdleStrategyConfig.IDLE_STRATEGY_ENV);

}
