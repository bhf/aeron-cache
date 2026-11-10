package com.bhf.aeroncache.services.cache.snapshot;

import com.bhf.aeroncache.http.responses.AeronCountersResponse;
import com.bhf.aeroncache.http.responses.RecordingPurgeResponse;
import com.bhf.aeroncache.http.responses.SnapshotInfoResponse;
import com.bhf.aeroncache.http.snapshot.SnapshotPurgeCheckFactory;
import lombok.extern.log4j.Log4j2;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Default {@link SnapshotPurgeCheckFactory}, discovered via {@link java.util.ServiceLoader}, that builds
 * the map-cache implementations of {@link SnapshotPurgeCheck}.
 */
@Log4j2
public final class DefaultSnapshotPurgeCheckFactory implements SnapshotPurgeCheckFactory {

    @Override
    public SnapshotPurgeCheck create(
            final SnapshotCheckStrategy strategy,
            final Duration pollInterval,
            final Duration snapshotInterval,
            final long stateChangeThreshold,
            final long archiveGrowthBytes,
            final Supplier<AeronCountersResponse> countersSupplier,
            final Supplier<SnapshotInfoResponse> snapshotInfoSupplier,
            final Supplier<RecordingPurgeResponse> lastPurgeSupplier) {

        final SnapshotPurgeCheck check = switch (strategy) {
            case CACHE_STATE_CHANGE -> new CacheStateChangeSnapshotPurgeCheck(
                    pollInterval,
                    stateChangeThreshold,
                    countersSupplier);
            case RECORDING_GROWTH -> new RecordingGrowthSnapshotPurgeCheck(
                    pollInterval,
                    archiveGrowthBytes,
                    snapshotInfoSupplier,
                    lastPurgeSupplier);
            case ALWAYS -> new AlwaysSnapshotPurgeCheck(snapshotInterval);
        };

        log.info("Scheduled snapshot + purge gated by {} check '{}' polling every {}",
                strategy, check.name(), check.getScheduleRate());
        return check;
    }
}
