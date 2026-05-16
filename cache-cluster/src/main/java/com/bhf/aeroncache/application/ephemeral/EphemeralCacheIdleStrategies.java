package com.bhf.aeroncache.application.ephemeral;

import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

import java.util.function.Supplier;

public class EphemeralCacheIdleStrategies {

    public static final Supplier<IdleStrategy> unclusteredAgentIdleStrategy = BackoffIdleStrategy::new;
}
