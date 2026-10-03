package com.bhf.aeroncache.integration.soak.streaming;

import com.bhf.aeroncache.integration.soak.common.SoakProps;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration for a streaming soak run, resolved from {@code -Psoak.*} system properties with defaults.
 * Time-bounded: rounds are driven until {@link #durationSeconds} elapses, cycling the full subscription
 * coverage matrix (see {@link StreamingSoakWorkload}).
 */
final class StreamingSoakConfig {

    /** Total wall-clock duration to drive rounds for. */
    final long durationSeconds;
    /** PRNG seed; drives variant order and all per-round choices, so a run is reproducible given the seed. */
    final long seed;
    /** Number of live mutations applied per round (after any hydration phase). */
    final int mutationsPerRound;
    /** Number of entries pre-seeded for a hydration round (whole-cache full-mode rounds). */
    final int hydrationEntries;
    /** Size of the key set used by whole-cache-scope rounds. */
    final int wholeCacheKeys;
    /** Per-operation / per-verification timeout; a breach means the cluster stalled - a genuine finding. */
    final int opTimeoutSeconds;
    /**
     * Whether to include counter-cache subscription variants in the coverage matrix (on by default). Note:
     * the gateway delivers each counter update TWICE to a session that both mutates and subscribes to the
     * same counter cache (the cluster reuses the increment/decrement/set result as the subscriber broadcast;
     * see handlePostIncrementCounter). This is a known, accepted behaviour - the workload models the two
     * consecutive events rather than asserting one.
     */
    final boolean includeCounters;

    private StreamingSoakConfig(long durationSeconds, long seed, int mutationsPerRound,
                               int hydrationEntries, int wholeCacheKeys, int opTimeoutSeconds, boolean includeCounters) {
        this.durationSeconds = durationSeconds;
        this.seed = seed;
        this.mutationsPerRound = mutationsPerRound;
        this.hydrationEntries = hydrationEntries;
        this.wholeCacheKeys = wholeCacheKeys;
        this.opTimeoutSeconds = opTimeoutSeconds;
        this.includeCounters = includeCounters;
    }

    static StreamingSoakConfig fromSystemProperties() {
        return new StreamingSoakConfig(
                SoakProps.longProp("soak.durationSeconds", 1800L),
                SoakProps.seed("soak.seed"),
                SoakProps.intProp("soak.mutationsPerRound", 8),
                SoakProps.intProp("soak.hydrationEntries", 4),
                SoakProps.intProp("soak.wholeCacheKeys", 5),
                SoakProps.intProp("soak.opTimeoutSeconds", 30),
                SoakProps.boolProp("soak.includeCounters", true));
    }

    Map<String, Object> toConfigMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("mutationsPerRound", mutationsPerRound);
        map.put("hydrationEntries", hydrationEntries);
        map.put("wholeCacheKeys", wholeCacheKeys);
        map.put("opTimeoutSeconds", opTimeoutSeconds);
        map.put("includeCounters", includeCounters);
        return map;
    }

    @Override
    public String toString() {
        return "StreamingSoakConfig{durationSeconds=" + durationSeconds
                + ", seed=" + seed
                + ", mutationsPerRound=" + mutationsPerRound
                + ", hydrationEntries=" + hydrationEntries
                + ", wholeCacheKeys=" + wholeCacheKeys
                + ", opTimeoutSeconds=" + opTimeoutSeconds + '}';
    }
}
