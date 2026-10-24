package com.bhf.aeroncache.clustertools.schedule.check;

import java.time.Duration;

/**
 * The default {@link SnapshotPurgeCheck}: always proceed, on a fixed interval. Wiring this preserves
 * the original behaviour of the scheduler - a snapshot + purge every {@code SNAPSHOT_INTERVAL} with no
 * gating - so it is the safe default when no smarter strategy is selected.
 *
 * <p>Because it does no work beyond returning {@code proceed}, its schedule rate <em>is</em> the
 * snapshot interval: there is no point polling more often than it takes a snapshot.
 */
public final class AlwaysSnapshotPurgeCheck implements SnapshotPurgeCheck {

    private final Duration scheduleRate;

    /**
     * @param scheduleRate how often the scheduled snapshot + purge runs; typically the configured
     *                     {@code SNAPSHOT_INTERVAL}.
     */
    public AlwaysSnapshotPurgeCheck(final Duration scheduleRate) {
        this.scheduleRate = scheduleRate;
    }

    @Override
    public SnapshotPurgeDecision evaluate() {
        return SnapshotPurgeDecision.proceed("always take scheduled snapshot + purge");
    }

    @Override
    public Duration getScheduleRate() {
        return scheduleRate;
    }
}
