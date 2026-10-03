package com.bhf.aeroncache.integration.soak;

import java.util.Random;

/**
 * Configuration for a soak run, resolved from {@code -Psoak.*} system properties (forwarded by the
 * module build) with sensible defaults. The resolved values - crucially the seed - are logged at the
 * start of a run so any failure is fully reproducible by re-running with the same {@code -Psoak.seed}.
 *
 * <p>The run is purely time-bounded: it drives a weighted, randomised workload until
 * {@link #durationSeconds} of wall-clock has elapsed.
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
                longProp("soak.durationSeconds", 1800L),
                seedProp("soak.seed"),
                intProp("soak.kvCacheCount", 4),
                intProp("soak.counterCacheCount", 4),
                intProp("soak.keySpace", 500),
                intProp("soak.valueSizeBytes", 160),
                intProp("soak.verifyEvery", 2000),
                intProp("soak.opTimeoutSeconds", 30));
    }

    private static long seedProp(String name) {
        var raw = System.getProperty(name);
        if (raw == null || raw.isBlank()) {
            return new Random().nextLong();
        }
        return Long.parseLong(raw.trim());
    }

    private static long longProp(String name, long dflt) {
        var raw = System.getProperty(name);
        return (raw == null || raw.isBlank()) ? dflt : Long.parseLong(raw.trim());
    }

    private static int intProp(String name, int dflt) {
        var raw = System.getProperty(name);
        return (raw == null || raw.isBlank()) ? dflt : Integer.parseInt(raw.trim());
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
