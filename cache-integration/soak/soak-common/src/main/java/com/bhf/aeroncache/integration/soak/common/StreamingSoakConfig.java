package com.bhf.aeroncache.integration.soak.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration for a streaming soak run, resolved from {@code -Psoak.*} system properties with defaults, and
 * shared by every streaming soak transport (binary Aeron, streaming WebSocket and SSE) - only the subscriber
 * factory differs. Time-bounded: rounds cycle the full subscription coverage matrix (see
 * {@link StreamingSoakWorkload}) until {@link #durationSeconds} elapses.
 */
public final class StreamingSoakConfig {

    /** Total wall-clock duration to drive rounds for. */
    public final long durationSeconds;
    /** PRNG seed; drives variant order and all per-round choices, so a run is reproducible given the seed. */
    public final long seed;
    /** Number of live mutations applied per round (after any hydration phase). */
    public final int mutationsPerRound;
    /** Size of the key set used by whole-cache-scope rounds. */
    public final int wholeCacheKeys;
    /** Target key length in bytes; {@code 0} leaves keys at their natural length. */
    public final int keySizeBytes;
    /** Target value length in bytes; {@code 0} leaves values as single-field {@code {"v":N}}. */
    public final int valueSizeBytes;
    /** Per-operation / per-verification timeout; a breach means the cluster or a connection stalled. */
    public final int opTimeoutSeconds;
    /** Whether to include counter-cache subscription variants in the coverage matrix. */
    public final boolean includeCounters;
    /** Whether to include the warm near-cache synthetic-patch-generation variants in the coverage matrix. */
    public final boolean includeSynthetic;

    private StreamingSoakConfig(long durationSeconds, long seed, int mutationsPerRound, int wholeCacheKeys,
                               int keySizeBytes, int valueSizeBytes, int opTimeoutSeconds,
                               boolean includeCounters, boolean includeSynthetic) {
        this.durationSeconds = durationSeconds;
        this.seed = seed;
        this.mutationsPerRound = mutationsPerRound;
        this.wholeCacheKeys = wholeCacheKeys;
        this.keySizeBytes = keySizeBytes;
        this.valueSizeBytes = valueSizeBytes;
        this.opTimeoutSeconds = opTimeoutSeconds;
        this.includeCounters = includeCounters;
        this.includeSynthetic = includeSynthetic;
    }

    public static StreamingSoakConfig fromSystemProperties() {
        return new StreamingSoakConfig(
                SoakProps.longProp("soak.durationSeconds", 1800L),
                SoakProps.seed("soak.seed"),
                SoakProps.intProp("soak.mutationsPerRound", 8),
                SoakProps.intProp("soak.wholeCacheKeys", 5),
                SoakProps.intProp("soak.keySizeBytes", 0),
                SoakProps.intProp("soak.valueSizeBytes", 0),
                SoakProps.intProp("soak.opTimeoutSeconds", 30),
                SoakProps.boolProp("soak.includeCounters", true),
                SoakProps.boolProp("soak.includeSynthetic", true));
    }

    public Map<String, Object> toConfigMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("mutationsPerRound", mutationsPerRound);
        map.put("wholeCacheKeys", wholeCacheKeys);
        map.put("keySizeBytes", keySizeBytes);
        map.put("valueSizeBytes", valueSizeBytes);
        map.put("opTimeoutSeconds", opTimeoutSeconds);
        map.put("includeCounters", includeCounters);
        map.put("includeSynthetic", includeSynthetic);
        return map;
    }

    @Override
    public String toString() {
        return "StreamingSoakConfig{durationSeconds=" + durationSeconds
                + ", seed=" + seed
                + ", mutationsPerRound=" + mutationsPerRound
                + ", wholeCacheKeys=" + wholeCacheKeys
                + ", keySizeBytes=" + keySizeBytes
                + ", valueSizeBytes=" + valueSizeBytes
                + ", opTimeoutSeconds=" + opTimeoutSeconds
                + ", includeCounters=" + includeCounters
                + ", includeSynthetic=" + includeSynthetic + '}';
    }
}
