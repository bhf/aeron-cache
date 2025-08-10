package com.bhf.aeroncache.application.unclustered;

import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

import java.util.function.Supplier;

public class SingleNodeIdleStrategies {

    public static final Supplier<IdleStrategy> unclusteredAgentIdleStrategy = BusySpinIdleStrategy::new;
}
