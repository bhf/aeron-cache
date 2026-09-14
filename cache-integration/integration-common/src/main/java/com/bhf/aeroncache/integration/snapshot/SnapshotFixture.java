package com.bhf.aeroncache.integration.snapshot;

import java.util.List;

/**
 * The single source of truth for the caches, keys and values that the snapshot backward
 * compatibility harness seeds and later asserts.
 *
 * <p>Both the capture side (which seeds a running cluster and snapshots it into a release
 * artifact) and the restore side (which boots a newer binary against an older artifact and
 * asserts recovery) reference this fixture, so the two can never drift.
 *
 * <p>The {@link #VERSION} is written into the artifact manifest. If entries are ever added, bump
 * the version; the restore side only asserts entries that existed at the artifact's fixture
 * version, so newer harness code can still validate older artifacts.
 */
public final class SnapshotFixture {

    /** Bump whenever {@link #ENTRIES} or {@link #COUNTER_ENTRIES} changes so older artifacts remain assertable. */
    public static final int VERSION = 3;

    /** A single seeded cache entry against the standard cache API. */
    public record Entry(String cacheId, String key, String value, int sinceFixtureVersion) {
        public Entry(String cacheId, String key, String value) {
            this(cacheId, key, value, 1);
        }
    }

    /** A single seeded entry against the counters cache API. */
    public record CounterEntry(String cacheId, String key, long value, int sinceFixtureVersion) {
    }

    public static final String CACHE_A = "regression-cache-a";
    public static final String CACHE_B = "regression-cache-b";

    public static final String COUNTER_CACHE_A = "regression-counters-a";
    public static final String COUNTER_CACHE_B = "regression-counters-b";

    /**
     * Known caches created before seeding. Kept explicit (rather than derived from entries) so the
     * set of caches is itself part of the asserted contract.
     */
    public static final List<String> CACHES = List.of(CACHE_A, CACHE_B);

    /** Known counter caches created before seeding (fixture version 3+). */
    public static final List<String> COUNTER_CACHES = List.of(COUNTER_CACHE_A, COUNTER_CACHE_B);

    /**
     * The seeded entries. Values span serialization edge cases: plain ASCII, a multi-byte unicode
     * string, a moderate (~256 byte) value, and a large (64 KB) value that spans many Aeron snapshot
     * fragments. The large value guards {@code MapCacheManager.loadSnapshot}'s FragmentAssembler: a
     * cache whose serialized snapshot exceeds the MTU must still recover intact.
     */
    public static final List<Entry> ENTRIES = List.of(
            new Entry(CACHE_A, "alpha", "one"),
            new Entry(CACHE_A, "beta", "two"),
            new Entry(CACHE_A, "unicode", "façade-Ω-値"),
            new Entry(CACHE_B, "gamma", "three"),
            new Entry(CACHE_B, "medium", "m".repeat(256)),
            new Entry(CACHE_B, "large", "x".repeat(65536), 2)
    );

    /**
     * Counter cache entries (fixture version 3+). Seeding both regular caches and counter caches and
     * asserting both recover exercises the snapshot's manager boundary: the regular-cache loader must
     * stop at its own end marker and leave the counter-cache records for the counters manager.
     */
    public static final List<CounterEntry> COUNTER_ENTRIES = List.of(
            new CounterEntry(COUNTER_CACHE_A, "hits", 42L, 3),
            new CounterEntry(COUNTER_CACHE_A, "misses", 7L, 3),
            new CounterEntry(COUNTER_CACHE_B, "total", 1_000_000L, 3)
    );

    /** Regular-cache entries that existed at or before the given artifact fixture version. */
    public static List<Entry> entriesFor(int artifactFixtureVersion) {
        return ENTRIES.stream()
                .filter(e -> e.sinceFixtureVersion() <= artifactFixtureVersion)
                .toList();
    }

    /** Counter entries that existed at or before the given artifact fixture version. */
    public static List<CounterEntry> counterEntriesFor(int artifactFixtureVersion) {
        return COUNTER_ENTRIES.stream()
                .filter(e -> e.sinceFixtureVersion() <= artifactFixtureVersion)
                .toList();
    }

    /** Counter caches that existed at or before the given artifact fixture version. */
    public static List<String> counterCachesFor(int artifactFixtureVersion) {
        return artifactFixtureVersion >= 3 ? COUNTER_CACHES : List.of();
    }

    private SnapshotFixture() {
    }
}
