package com.bhf.aeroncache.integration.soak;

import com.bhf.aeroncache.integration.soak.common.SoakProps;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration for a core-cache soak run, resolved from {@code -Psoak.*} system properties (forwarded by
 * the module build) with sensible defaults. The resolved values - crucially the seed - are recorded in the
 * report so any failure is reproducible by re-running with the same {@code -Psoak.seed}.
 *
 * <p>The run is purely time-bounded: it drives a weighted, randomised workload until {@link #durationSeconds}
 * of wall-clock has elapsed.
 */
final class SoakConfig {

    /** Total wall-clock duration to drive load for. */
    final long durationSeconds;
    /** PRNG seed; every random choice derives from it, so a run is reproducible given the seed. */
    final long seed;
    /** Number of key-value caches the workload spreads load across. */
    final int kvCacheCount;
    /** Number of counter caches the workload spreads load across. */
    final int counterCacheCount;
    /** Size of the key space per cache; a small space forces overwrites, removes and collisions. */
    final int keySpace;
    /** Size in bytes of randomly generated values. */
    final int valueSizeBytes;
    /** Run a full oracle reconciliation sweep every N operations. */
    final int verifyEvery;
    /** Per-operation response timeout; a breach means the cluster stalled - a genuine soak finding. */
    final int opTimeoutSeconds;

    private SoakConfig(long durationSeconds, long seed, int kvCacheCount, int counterCacheCount,
                       int keySpace, int valueSizeBytes, int verifyEvery, int opTimeoutSeconds) {
        this.durationSeconds = durationSeconds;
        this.seed = seed;
        this.kvCacheCount = kvCacheCount;
        this.counterCacheCount = counterCacheCount;
        this.keySpace = keySpace;
        this.valueSizeBytes = valueSizeBytes;
        this.verifyEvery = verifyEvery;
        this.opTimeoutSeconds = opTimeoutSeconds;
    }

    static SoakConfig fromSystemProperties() {
        return new SoakConfig(
                SoakProps.longProp("soak.durationSeconds", 1800L),
                SoakProps.seed("soak.seed"),
                SoakProps.intProp("soak.kvCacheCount", 4),
                SoakProps.intProp("soak.counterCacheCount", 4),
                SoakProps.intProp("soak.keySpace", 500),
                SoakProps.intProp("soak.valueSizeBytes", 160),
                SoakProps.intProp("soak.verifyEvery", 2000),
                SoakProps.intProp("soak.opTimeoutSeconds", 30));
    }

    /** Ordered config fields for the run report. */
    Map<String, Object> toConfigMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("kvCacheCount", kvCacheCount);
        map.put("counterCacheCount", counterCacheCount);
        map.put("keySpace", keySpace);
        map.put("valueSizeBytes", valueSizeBytes);
        map.put("verifyEvery", verifyEvery);
        map.put("opTimeoutSeconds", opTimeoutSeconds);
        return map;
    }

    @Override
    public String toString() {
        return "SoakConfig{durationSeconds=" + durationSeconds
                + ", seed=" + seed
                + ", kvCacheCount=" + kvCacheCount
                + ", counterCacheCount=" + counterCacheCount
                + ", keySpace=" + keySpace
                + ", valueSizeBytes=" + valueSizeBytes
                + ", verifyEvery=" + verifyEvery
                + ", opTimeoutSeconds=" + opTimeoutSeconds + '}';
    }
}
