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

    /** Bump whenever {@link #ENTRIES} changes so older artifacts remain assertable. */
    public static final int VERSION = 2;

    /** A single seeded cache entry against the standard cache API. */
    public record Entry(String cacheId, String key, String value, int sinceFixtureVersion) {
        public Entry(String cacheId, String key, String value) {
            this(cacheId, key, value, 1);
        }
    }

    public static final String CACHE_A = "regression-cache-a";
    public static final String CACHE_B = "regression-cache-b";

    /**
     * Known caches created before seeding. Kept explicit (rather than derived from entries) so the
     * set of caches is itself part of the asserted contract.
     */
    public static final List<String> CACHES = List.of(CACHE_A, CACHE_B);

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

    /** Entries that existed at or before the given artifact fixture version. */
    public static List<Entry> entriesFor(int artifactFixtureVersion) {
        return ENTRIES.stream()
                .filter(e -> e.sinceFixtureVersion() <= artifactFixtureVersion)
                .toList();
    }

    private SnapshotFixture() {
    }
}
