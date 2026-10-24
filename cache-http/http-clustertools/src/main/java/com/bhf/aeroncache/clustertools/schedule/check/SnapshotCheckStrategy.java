package com.bhf.aeroncache.clustertools.schedule.check;

/**
 * Selects which {@link SnapshotPurgeCheck} gates the scheduled snapshot + purge. Chosen via the
 * {@code SNAPSHOT_CHECK_STRATEGY} environment variable; only the scheduled path is affected, the
 * on-demand HTTP endpoint always runs regardless.
 */
public enum SnapshotCheckStrategy {

    /**
     * Always take the scheduled snapshot + purge on a fixed interval - the original behaviour.
     * Backed by {@link AlwaysSnapshotPurgeCheck}.
     */
    ALWAYS,

    /**
     * Snapshot only once enough cache state-changing operations have accrued, read from the custom
     * request counters in the media driver. Backed by {@link CacheStateChangeSnapshotPurgeCheck}.
     */
    CACHE_STATE_CHANGE,

    /**
     * Snapshot only once the archive has grown enough since the last purge. Backed by
     * {@link RecordingGrowthSnapshotPurgeCheck}.
     */
    RECORDING_GROWTH
}
