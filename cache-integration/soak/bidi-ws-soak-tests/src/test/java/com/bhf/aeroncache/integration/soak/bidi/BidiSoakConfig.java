package com.bhf.aeroncache.integration.soak.bidi;

import com.bhf.aeroncache.integration.soak.common.SoakProps;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration for a bidi-websocket soak run, resolved from {@code -Psoak.*} system properties with
 * defaults. The run is split into two time-bounded phases driven over the one bidi protocol:
 * <ol>
 *   <li>a <b>command/oracle</b> phase ({@code commandDurationSeconds}) that mirrors the core-cache soak over
 *       the bidi command surface, reconciling against a {@code SoakOracle}; and</li>
 *   <li>a <b>streaming</b> phase ({@code streamingDurationSeconds}) that cycles the subscription coverage
 *       matrix over the bidi subscribe/stream path.</li>
 * </ol>
 * The resolved values - crucially the seed - are recorded in the report so any failure is reproducible.
 */
final class BidiSoakConfig {

    // ---- phases
    /** Wall-clock duration of the command/oracle phase. */
    final long commandDurationSeconds;
    /** Wall-clock duration of the streaming-coverage phase. */
    final long streamingDurationSeconds;
    /** PRNG seed; every random choice derives from it, so a run is reproducible given the seed. */
    final long seed;

    // ---- command phase
    final int kvCacheCount;
    final int counterCacheCount;
    final int keySpace;
    /** Target key length in bytes (both phases); {@code 0} leaves keys at their natural length. */
    final int keySizeBytes;
    /** Target value length in bytes (both phases); {@code 0} leaves values at their natural form. */
    final int valueSizeBytes;
    final int verifyEvery;

    // ---- streaming phase
    final int mutationsPerRound;
    final int wholeCacheKeys;
    final boolean includeCounters;
    /** Whether to include the warm near-cache synthetic-patch-generation variants in the streaming matrix. */
    final boolean includeSynthetic;

    // ---- shared
    /**
     * Whether the WS server runs with its near cache enabled. On by default (the synthetic-patch streaming
     * rounds need it). Turning it off makes the command/oracle phase read through to the cluster instead of a
     * non-read-through near cache - needed for large-value runs, where slower priming otherwise races the
     * command phase's first read. When off, {@link #includeSynthetic} is forced off (synthetic rounds require
     * the near cache). Applied to the WS server via the {@code NEAR_CACHE_ENABLED} system property.
     */
    final boolean nearCacheEnabled;
    /** Per-operation / per-verification timeout; a breach means the cluster (or the socket) stalled. */
    final int opTimeoutSeconds;

    private BidiSoakConfig(long commandDurationSeconds, long streamingDurationSeconds, long seed,
                          int kvCacheCount, int counterCacheCount, int keySpace, int keySizeBytes,
                          int valueSizeBytes, int verifyEvery, int mutationsPerRound, int wholeCacheKeys,
                          boolean includeCounters, boolean includeSynthetic, boolean nearCacheEnabled,
                          int opTimeoutSeconds) {
        this.commandDurationSeconds = commandDurationSeconds;
        this.streamingDurationSeconds = streamingDurationSeconds;
        this.seed = seed;
        this.kvCacheCount = kvCacheCount;
        this.counterCacheCount = counterCacheCount;
        this.keySpace = keySpace;
        this.keySizeBytes = keySizeBytes;
        this.valueSizeBytes = valueSizeBytes;
        this.verifyEvery = verifyEvery;
        this.mutationsPerRound = mutationsPerRound;
        this.wholeCacheKeys = wholeCacheKeys;
        this.includeCounters = includeCounters;
        this.includeSynthetic = includeSynthetic;
        this.nearCacheEnabled = nearCacheEnabled;
        this.opTimeoutSeconds = opTimeoutSeconds;
    }

    static BidiSoakConfig fromSystemProperties() {
        var nearCacheEnabled = SoakProps.boolProp("soak.nearCacheEnabled", true);
        // Synthetic-patch (warm) rounds are served from the near cache, so they are impossible with it off.
        var includeSynthetic = SoakProps.boolProp("soak.includeSynthetic", true) && nearCacheEnabled;
        return new BidiSoakConfig(
                SoakProps.longProp("soak.commandDurationSeconds", 900L),
                SoakProps.longProp("soak.streamingDurationSeconds", 900L),
                SoakProps.seed("soak.seed"),
                SoakProps.intProp("soak.kvCacheCount", 4),
                SoakProps.intProp("soak.counterCacheCount", 4),
                SoakProps.intProp("soak.keySpace", 500),
                SoakProps.intProp("soak.keySizeBytes", 0),
                // 0 = each phase's natural default (command: opaque bytes at COMMAND_DEFAULT_VALUE_SIZE_BYTES;
                // streaming: single-field {"v":N}). A positive value sizes both phases to it.
                SoakProps.intProp("soak.valueSizeBytes", 0),
                SoakProps.intProp("soak.verifyEvery", 2000),
                SoakProps.intProp("soak.mutationsPerRound", 8),
                SoakProps.intProp("soak.wholeCacheKeys", 5),
                SoakProps.boolProp("soak.includeCounters", true),
                includeSynthetic,
                nearCacheEnabled,
                SoakProps.intProp("soak.opTimeoutSeconds", 30));
    }

    /** The opaque value size the command phase uses when {@link #valueSizeBytes} is left at its 0 default. */
    static final int COMMAND_DEFAULT_VALUE_SIZE_BYTES = 160;

    /** Effective command-phase value size: the configured size, or the opaque default when unset. */
    int commandValueSizeBytes() {
        return valueSizeBytes > 0 ? valueSizeBytes : COMMAND_DEFAULT_VALUE_SIZE_BYTES;
    }

    /** Total wall-clock duration across both phases (recorded in the report). */
    long totalDurationSeconds() {
        return commandDurationSeconds + streamingDurationSeconds;
    }

    Map<String, Object> toConfigMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("commandDurationSeconds", commandDurationSeconds);
        map.put("streamingDurationSeconds", streamingDurationSeconds);
        map.put("kvCacheCount", kvCacheCount);
        map.put("counterCacheCount", counterCacheCount);
        map.put("keySpace", keySpace);
        map.put("keySizeBytes", keySizeBytes);
        map.put("valueSizeBytes", valueSizeBytes);
        map.put("verifyEvery", verifyEvery);
        map.put("mutationsPerRound", mutationsPerRound);
        map.put("wholeCacheKeys", wholeCacheKeys);
        map.put("includeCounters", includeCounters);
        map.put("includeSynthetic", includeSynthetic);
        map.put("nearCacheEnabled", nearCacheEnabled);
        map.put("opTimeoutSeconds", opTimeoutSeconds);
        return map;
    }

    @Override
    public String toString() {
        return "BidiSoakConfig{commandDurationSeconds=" + commandDurationSeconds
                + ", streamingDurationSeconds=" + streamingDurationSeconds
                + ", seed=" + seed
                + ", kvCacheCount=" + kvCacheCount
                + ", counterCacheCount=" + counterCacheCount
                + ", keySpace=" + keySpace
                + ", keySizeBytes=" + keySizeBytes
                + ", valueSizeBytes=" + valueSizeBytes
                + ", verifyEvery=" + verifyEvery
                + ", mutationsPerRound=" + mutationsPerRound
                + ", wholeCacheKeys=" + wholeCacheKeys
                + ", includeCounters=" + includeCounters
                + ", includeSynthetic=" + includeSynthetic
                + ", nearCacheEnabled=" + nearCacheEnabled
                + ", opTimeoutSeconds=" + opTimeoutSeconds + '}';
    }
}
