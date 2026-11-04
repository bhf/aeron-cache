package com.bhf.aeroncache.integration.soak.common;

/**
 * Drives a gateway edge's near-cache management surface during a soak run: priming a cache into the local
 * near cache (so a later patch subscription is served synthetic merge-patch deltas from the warm copy),
 * waiting for the warm copy to populate, stopping near caching, and reading the per-outcome synthetic-patch
 * counter off {@code /prometheus}.
 *
 * <p>The management endpoints ({@code POST}/{@code DELETE}/{@code GET /api/v1/near/cache[s]}) and the
 * {@code /prometheus} scrape are all served on the same HTTP port the subscription transport uses for that
 * edge (the WS/SSE server for the URL soaks, the SBE gateway for the binary streaming soak), so one admin is
 * bound per suite to that port. This is the seam that lets a transport-neutral workload exercise synthetic
 * patch generation without knowing the edge's wire details.
 */
public interface NearCacheAdmin {

    /**
     * Prime (warm) the given regular cache: start the internal near-cache subscription so the local copy is
     * warmed before the patch subscription arrives. Idempotent server-side; throws on a non-200 response.
     */
    void prime(String cacheId);

    /**
     * Block until the named cache is reported warm with exactly {@code expectedItemCount} items (the snapshot
     * populates the warm copy asynchronously after {@link #prime}), or fail once {@code timeoutNanos} elapses.
     */
    void awaitWarm(String cacheId, int expectedItemCount, long timeoutNanos);

    /** Stop near caching the given regular cache (drops the warm copy and its internal subscription). */
    void stopNear(String cacheId);

    /**
     * Whether the synthetic-patch outcome counter is readable over this edge's {@code /prometheus}. The web
     * edges (WS/SSE) expose it as a Micrometer counter; the Aeron gateway records it as an Aeron driver counter
     * that its own {@code /prometheus} (Micrometer) does not carry, so a gateway admin reports {@code false} and
     * the workload exercises the warm path (prime, hydrate, synthetic stream events) without asserting counts.
     */
    boolean supportsPatchMetrics();

    /**
     * The current value of the synthetic-patch counter for the regular-cache domain and the given outcome
     * ({@code delta} / {@code baseline} / {@code suppressed} / {@code full_fallback}), read from
     * {@code /prometheus}. A before/after delta around a round's live mutations proves a synthetic patch of
     * that outcome was generated from the warm copy (rather than taking the cluster path).
     */
    long patchCount(String outcome);
}
