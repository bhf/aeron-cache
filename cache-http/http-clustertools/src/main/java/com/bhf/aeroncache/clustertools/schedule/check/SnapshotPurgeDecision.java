package com.bhf.aeroncache.clustertools.schedule.check;

/**
 * The outcome of a {@link SnapshotPurgeCheck} evaluation: whether a scheduled snapshot + purge should
 * go ahead this time, plus a human readable reason for the log.
 *
 * @param proceed whether the scheduled snapshot + purge should run.
 * @param reason  why the check reached this decision; always logged by the scheduler.
 */
public record SnapshotPurgeDecision(boolean proceed, String reason) {

    /**
     * A decision to take the scheduled snapshot + purge.
     *
     * @param reason why the run is worthwhile.
     * @return a proceeding decision.
     */
    public static SnapshotPurgeDecision proceed(final String reason) {
        return new SnapshotPurgeDecision(true, reason);
    }

    /**
     * A decision to skip this scheduled run.
     *
     * @param reason why the run is not worthwhile right now.
     * @return a skipping decision.
     */
    public static SnapshotPurgeDecision skip(final String reason) {
        return new SnapshotPurgeDecision(false, reason);
    }
}
