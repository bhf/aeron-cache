package com.bhf.aeroncache.services.cache.snapshot;

import com.bhf.aeroncache.models.counters.AeronCountersResponse;
import com.bhf.aeroncache.models.snapshot.RecordingPurgeResponse;
import com.bhf.aeroncache.models.snapshot.SnapshotInfoResponse;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * SPI for building the configured {@link SnapshotPurgeCheck} that gates the scheduled snapshot + purge.
 *
 * <p>The implementation is discovered with {@link java.util.ServiceLoader}, so the sidecar that runs the
 * scheduler compiles only against this interface and the {@link SnapshotPurgeCheck} SPI; the concrete
 * checks live in whichever cache implementation module is on the runtime classpath. This mirrors the
 * other SPI backends in the project (for example {@code CacheManagerFactory}).
 *
 * <p>The data-reading strategies are wired from the counter and snapshot DTOs in
 * {@link com.bhf.aeroncache.models.counters} and {@link com.bhf.aeroncache.models.snapshot}; the
 * checks themselves stay free of any IO dependency.
 */
public interface SnapshotPurgeCheckFactory {

    /**
     * Build the check selected by {@code strategy}, wiring the data-reading strategies with the supplied
     * readers so this factory stays free of IO.
     *
     * @param strategy             which gating strategy to build.
     * @param pollInterval         how often a data-reading check polls; passed as its schedule rate.
     * @param snapshotInterval     the plain snapshot interval; the {@code ALWAYS} strategy's schedule rate.
     * @param stateChangeThreshold minimum state-changing operations before the cache-state-change strategy
     *                             proceeds.
     * @param archiveGrowthBytes   minimum archive growth before the recording-growth strategy proceeds.
     * @param countersSupplier     reads the media driver counters, for the cache-state-change strategy.
     * @param snapshotInfoSupplier reads snapshot/archive info, for the recording-growth strategy.
     * @param lastPurgeSupplier    supplies the last purge result, for the recording-growth baseline.
     * @return the configured check.
     */
    SnapshotPurgeCheck create(
            SnapshotCheckStrategy strategy,
            Duration pollInterval,
            Duration snapshotInterval,
            long stateChangeThreshold,
            long archiveGrowthBytes,
            Supplier<AeronCountersResponse> countersSupplier,
            Supplier<SnapshotInfoResponse> snapshotInfoSupplier,
            Supplier<RecordingPurgeResponse> lastPurgeSupplier);
}
