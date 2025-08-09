package com.bhf.aeroncache.application.unclustered;

import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

public class SingleNodeIdleStrategies {

    public static final IdleStrategy unclusteredAgentIdleStrategy = new BusySpinIdleStrategy();
}
