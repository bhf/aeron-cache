package com.bhf.aeroncache.integration.soak.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration for a URL-based streaming soak run (streaming WebSocket or SSE), resolved from {@code -Psoak.*}
 * system properties with defaults. Time-bounded: rounds cycle the full subscription coverage matrix (see
 * {@link UrlStreamingSoakWorkload}) until {@link #durationSeconds} elapses. Shared by both URL transports -
 * only the subscriber factory differs.
 */
public final class UrlStreamingSoakConfig {

    /** Total wall-clock duration to drive rounds for. */
    public final long durationSeconds;
    /** PRNG seed; drives variant order and all per-round choices, so a run is reproducible given the seed. */
    public final long seed;
    /** Number of live mutations applied per round (after any hydration phase). */
    public final int mutationsPerRound;
    /** Size of the key set used by whole-cache-scope rounds. */
    public final int wholeCacheKeys;
    /** Per-operation / per-verification timeout; a breach means the cluster or a connection stalled. */
    public final int opTimeoutSeconds;
    /** Whether to include counter-cache subscription variants in the coverage matrix. */
    public final boolean includeCounters;

    private UrlStreamingSoakConfig(long durationSeconds, long seed, int mutationsPerRound, int wholeCacheKeys,
                                   int opTimeoutSeconds, boolean includeCounters) {
        this.durationSeconds = durationSeconds;
        this.seed = seed;
        this.mutationsPerRound = mutationsPerRound;
        this.wholeCacheKeys = wholeCacheKeys;
        this.opTimeoutSeconds = opTimeoutSeconds;
        this.includeCounters = includeCounters;
    }

    public static UrlStreamingSoakConfig fromSystemProperties() {
        return new UrlStreamingSoakConfig(
                SoakProps.longProp("soak.durationSeconds", 1800L),
                SoakProps.seed("soak.seed"),
                SoakProps.intProp("soak.mutationsPerRound", 8),
                SoakProps.intProp("soak.wholeCacheKeys", 5),
                SoakProps.intProp("soak.opTimeoutSeconds", 30),
                SoakProps.boolProp("soak.includeCounters", true));
    }

    public Map<String, Object> toConfigMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("mutationsPerRound", mutationsPerRound);
        map.put("wholeCacheKeys", wholeCacheKeys);
        map.put("opTimeoutSeconds", opTimeoutSeconds);
        map.put("includeCounters", includeCounters);
        return map;
    }

    @Override
    public String toString() {
        return "UrlStreamingSoakConfig{durationSeconds=" + durationSeconds
                + ", seed=" + seed
                + ", mutationsPerRound=" + mutationsPerRound
                + ", wholeCacheKeys=" + wholeCacheKeys
                + ", opTimeoutSeconds=" + opTimeoutSeconds
                + ", includeCounters=" + includeCounters + '}';
    }
}
