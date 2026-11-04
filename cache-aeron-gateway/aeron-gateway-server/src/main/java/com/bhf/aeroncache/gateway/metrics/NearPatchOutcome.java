package com.bhf.aeroncache.gateway.metrics;

/**
 * The outcome of a single synthetic patch generation for a near-cache PATCH subscriber, used to keep a
 * separate count of each on the local patch-emission path.
 */
public enum NearPatchOutcome {
    /** A merge-patch {@code PATCH_ITEM} delta was computed from the warm copy and emitted. */
    DELTA("delta"),
    /** A first-seen key had no prior value to diff, so a full {@code ADD_ITEM} baseline was emitted instead. */
    BASELINE("baseline"),
    /** The computed delta was empty (no effective change) and the update was dropped. */
    SUPPRESSED("suppressed"),
    /**
     * A valid merge patch could not be built (e.g. the warm or incoming value was not parseable JSON), so a
     * full {@code ADD_ITEM} value was emitted as a fallback rather than dropping the update and leaving the
     * subscriber stale.
     */
    FULL_FALLBACK("full_fallback");

    private final String label;

    NearPatchOutcome(String label) {
        this.label = label;
    }

    /**
     * @return a short, human readable name suitable for use in a metric label.
     */
    public String label() {
        return label;
    }
}
