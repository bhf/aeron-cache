package com.bhf.aeroncache.clustertools.schedule;

import com.bhf.aeroncache.clustertools.config.ClusterToolsConfig;
import com.bhf.aeroncache.clustertools.schedule.check.AlwaysSnapshotPurgeCheck;
import com.bhf.aeroncache.clustertools.schedule.check.CacheStateChangeSnapshotPurgeCheck;
import com.bhf.aeroncache.clustertools.schedule.check.RecordingGrowthSnapshotPurgeCheck;
import com.bhf.aeroncache.clustertools.schedule.check.SnapshotPurgeCheck;
import com.bhf.aeroncache.http.responses.AeronCountersResponse;
import com.bhf.aeroncache.http.responses.RecordingPurgeResponse;
import com.bhf.aeroncache.http.responses.SnapshotInfoResponse;
import lombok.extern.log4j.Log4j2;

import java.util.function.Supplier;

/**
 * Builds the configured {@link SnapshotPurgeCheck} from {@link ClusterToolsConfig}, so the gating
 * strategy for the scheduled snapshot + purge can be selected via environment
 * ({@code SNAPSHOT_CHECK_STRATEGY}) rather than code. The data-reading strategies are wired with the
 * supplied readers so this factory stays free of IO.
 */
@Log4j2
public final class SnapshotPurgeCheckFactory {

    private SnapshotPurgeCheckFactory() {
    }

    /**
     * Create the check selected by {@link ClusterToolsConfig#getSnapshotCheckStrategy()}.
     *
     * @param config               the resolved cluster tools configuration.
     * @param countersSupplier     reads the media driver counters, for the cache-state-change strategy.
     * @param snapshotInfoSupplier reads snapshot/archive info, for the recording-growth strategy.
     * @param lastPurgeSupplier    supplies the last purge result, for the recording-growth baseline.
     * @return the configured check.
     */
    public static SnapshotPurgeCheck create(
            final ClusterToolsConfig config,
            final Supplier<AeronCountersResponse> countersSupplier,
            final Supplier<SnapshotInfoResponse> snapshotInfoSupplier,
            final Supplier<RecordingPurgeResponse> lastPurgeSupplier) {

        final SnapshotPurgeCheck check = switch (config.getSnapshotCheckStrategy()) {
            case CACHE_STATE_CHANGE -> new CacheStateChangeSnapshotPurgeCheck(
                    config.getSnapshotCheckPollInterval(),
                    config.getSnapshotCheckStateChangeThreshold(),
                    countersSupplier);
            case RECORDING_GROWTH -> new RecordingGrowthSnapshotPurgeCheck(
                    config.getSnapshotCheckPollInterval(),
                    config.getSnapshotCheckArchiveGrowthBytes(),
                    snapshotInfoSupplier,
                    lastPurgeSupplier);
            case ALWAYS -> new AlwaysSnapshotPurgeCheck(config.getSnapshotInterval());
        };

        log.info("Scheduled snapshot + purge gated by {} check '{}' polling every {}",
                config.getSnapshotCheckStrategy(), check.name(), check.getScheduleRate());
        return check;
    }
}
