package com.bhf.aeroncache.clustertools.schedule.check;

import com.bhf.aeroncache.http.responses.RecordingPurgeResponse;
import com.bhf.aeroncache.http.responses.SnapshotInfoResponse;
import lombok.extern.log4j.Log4j2;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * An example {@link SnapshotPurgeCheck} that only takes a scheduled snapshot + purge once the cluster
 * log recordings have grown by a configured number of bytes since the last purge - a proxy for "enough
 * new state has accumulated to be worth snapshotting". It is cheap to evaluate (it only reads the
 * recording log and stats the archive directory, via {@link SnapshotInfoResponse}), so it can poll
 * frequently and still avoid taking snapshots when the cluster is idle.
 *
 * <p>The baseline is the archive size recorded right after the last successful purge
 * ({@link RecordingPurgeResponse#archiveDirBytesAfter()}); growth is the current archive size minus
 * that baseline. When no purge has run yet the baseline is zero, so the first poll past the threshold
 * triggers a run and establishes the baseline for subsequent polls.
 *
 * <p>A counter-driven strategy ("how much has cache state changed") follows exactly the same shape:
 * swap the {@link SnapshotInfoResponse} supplier for one reading the application counters and compare a
 * monotonic activity counter's delta against a threshold. This class is the reference for that pattern.
 */
@Log4j2
public final class RecordingGrowthSnapshotPurgeCheck implements SnapshotPurgeCheck {

    private final Duration scheduleRate;
    private final long growthThresholdBytes;
    private final Supplier<SnapshotInfoResponse> snapshotInfoSupplier;
    private final Supplier<RecordingPurgeResponse> lastPurgeSupplier;

    /**
     * @param scheduleRate         how often to poll; typically much shorter than a plain snapshot
     *                             interval, since polling is cheap and most polls will skip.
     * @param growthThresholdBytes minimum archive growth since the last purge before a snapshot +
     *                             purge is taken.
     * @param snapshotInfoSupplier reads the current snapshot info, including the live archive size.
     * @param lastPurgeSupplier    supplies the most recent purge result, or {@code null} if none has
     *                             run yet; used to establish the growth baseline.
     */
    public RecordingGrowthSnapshotPurgeCheck(
            final Duration scheduleRate,
            final long growthThresholdBytes,
            final Supplier<SnapshotInfoResponse> snapshotInfoSupplier,
            final Supplier<RecordingPurgeResponse> lastPurgeSupplier) {
        this.scheduleRate = scheduleRate;
        this.growthThresholdBytes = growthThresholdBytes;
        this.snapshotInfoSupplier = snapshotInfoSupplier;
        this.lastPurgeSupplier = lastPurgeSupplier;
    }

    @Override
    public SnapshotPurgeDecision evaluate() {
        final SnapshotInfoResponse info = snapshotInfoSupplier.get();
        final long currentBytes = info.archiveDirBytes();
        final long baselineBytes = baseline();
        final long growth = currentBytes - baselineBytes;

        if (growth < growthThresholdBytes) {
            return SnapshotPurgeDecision.skip(String.format(
                    "archive grew %d bytes since last purge baseline %d, below threshold %d",
                    growth, baselineBytes, growthThresholdBytes));
        }
        return SnapshotPurgeDecision.proceed(String.format(
                "archive grew %d bytes since last purge baseline %d, at or above threshold %d",
                growth, baselineBytes, growthThresholdBytes));
    }

    /**
     * The archive size to measure growth from: the size after the last successful purge, or zero when
     * no successful purge has run yet.
     */
    private long baseline() {
        final RecordingPurgeResponse lastPurge = lastPurgeSupplier.get();
        if (lastPurge != null && lastPurge.success() && lastPurge.archiveDirBytesAfter() >= 0) {
            return lastPurge.archiveDirBytesAfter();
        }
        return 0L;
    }

    @Override
    public Duration getScheduleRate() {
        return scheduleRate;
    }
}
