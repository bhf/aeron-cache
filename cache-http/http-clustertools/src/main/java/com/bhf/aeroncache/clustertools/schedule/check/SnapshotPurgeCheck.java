package com.bhf.aeroncache.clustertools.schedule.check;

import java.time.Duration;

/**
 * Strategy deciding whether a <em>scheduled</em> snapshot + purge is worth taking.
 *
 * <p>The scheduled snapshot + purge timer consults the single configured check before each run: if
 * {@link #evaluate()} returns a skipping decision the run is dropped, otherwise the snapshot + purge
 * proceeds. This only gates the scheduled path - the on-demand {@code snapshot-and-purge} HTTP
 * endpoint always runs regardless of any check.
 *
 * <p>The polling rate is a property of the implementation ({@link #getScheduleRate()}) rather than a
 * single global setting, so a cheap default check that always proceeds can run rarely, while a check
 * that inspects recording-log or counter growth to decide whether state has changed enough to be
 * worth snapshotting can poll far more frequently - it only triggers the expensive snapshot + purge
 * when it actually decides to.
 *
 * <p>Implementations must be safe to call repeatedly from the scheduler's single timer thread.
 * {@link #evaluate()} may throw; the scheduler treats a thrown exception as a failed run and skips.
 */
public interface SnapshotPurgeCheck {

    /**
     * Decide whether the scheduled snapshot + purge should run now.
     *
     * @return the decision, including a reason for the log.
     */
    SnapshotPurgeDecision evaluate();

    /**
     * How often the scheduler should invoke {@link #evaluate()}. A frequently-polling check that only
     * occasionally decides to proceed keeps snapshots responsive to real change without taking them
     * needlessly; a trivial always-proceed check should return a long interval so it behaves like a
     * plain fixed-rate snapshot timer.
     *
     * @return the polling interval for this check.
     */
    Duration getScheduleRate();

    /**
     * A short name for this check, used in scheduler log lines. Defaults to the simple class name.
     *
     * @return the check's name.
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
