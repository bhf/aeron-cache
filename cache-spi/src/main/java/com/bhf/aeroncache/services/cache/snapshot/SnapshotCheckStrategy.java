package com.bhf.aeroncache.services.cache.snapshot;

/**
 * Selects which {@link SnapshotPurgeCheck} gates the scheduled snapshot + purge. Chosen via the
 * {@code SNAPSHOT_CHECK_STRATEGY} environment variable; only the scheduled path is affected, the
 * on-demand HTTP endpoint always runs regardless. The concrete check backing each strategy is
 * supplied by the {@code SnapshotPurgeCheckFactory} SPI implementation on the runtime classpath.
 */
public enum SnapshotCheckStrategy {

    /**
     * Always take the scheduled snapshot + purge on a fixed interval - the original behaviour.
     */
    ALWAYS,

    /**
     * Snapshot only once enough cache state-changing operations have accrued, read from the custom
     * request counters in the media driver.
     */
    CACHE_STATE_CHANGE,

    /**
     * Snapshot only once the archive has grown enough since the last purge.
     */
    RECORDING_GROWTH
}
