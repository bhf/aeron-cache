package com.bhf.aeroncache.services.cache.snapshot;

/**
 * Record framing for a cache manager's portion of a cluster snapshot.
 *
 * <p>Each message offered to the snapshot publication is a single self-typed record whose first
 * field is a {@link #TYPE_LENGTH}-byte type discriminator. A cache manager writes, in order:
 * <pre>
 *   CACHE_BEGIN(cacheId, stats)          // once per cache, recreates it (even when empty)
 *   CACHE_ENTRY(key, value, hash) ...    // one per entry, loaded into the current cache
 *   ... (repeated per cache) ...
 *   MANAGER_END                          // marks the end of THIS manager's records
 * </pre>
 *
 * <p>The trailing {@link #MANAGER_END} lets each manager load exactly its own records and stop,
 * rather than draining the whole snapshot image - which matters because several managers (regular
 * caches, then counter caches) share one snapshot stream.
 *
 * <p>Entries are framed with the implicit-current-cache convention: they carry no cacheId and load
 * into the cache named by the most recent {@link #CACHE_BEGIN}. This is safe because a single
 * snapshot publication delivers messages in order and each cache is written in full before the next.
 */
public final class SnapshotRecords {

    /** Bytes used by the leading record-type discriminator. */
    public static final int TYPE_LENGTH = Integer.BYTES;

    /** Cache header: cacheId followed by its stats. Recreates the cache and sets the current cache. */
    public static final int CACHE_BEGIN = 1;

    /** A single cache entry (key, value, hash) for the current cache. */
    public static final int CACHE_ENTRY = 2;

    /** Sentinel marking the end of a manager's records. */
    public static final int MANAGER_END = 3;

    private SnapshotRecords() {
    }
}
