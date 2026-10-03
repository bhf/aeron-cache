package com.bhf.aeroncache.integration.soak.common;

/**
 * A soak workload: the suite-specific driver that {@link AbstractGatewaySoakTest} runs against the live
 * gateway client. {@link #report()} must be available as soon as the run is constructed (before and after
 * {@link #run()}), so the harness can always write the metrics summary - even when {@link #run()} fails.
 */
public interface SoakRun {

    void run();

    SoakReport report();
}
