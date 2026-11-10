package com.bhf.aeroncache.services.patch;

/**
 * The outcome of attempting to compute a merge-patch delta.
 */
public record MergePatchResult(Kind kind, String delta) {

    public enum Kind {
        /** A non-empty delta was produced; emit it as a patch update. */
        DELTA,
        /** The two values were equal; no update need be sent. */
        UNCHANGED,
        /** A delta could not be produced (unparseable or null input); emit the whole value instead. */
        FAILED
    }

    private static final MergePatchResult UNCHANGED = new MergePatchResult(Kind.UNCHANGED, null);
    private static final MergePatchResult FAILED = new MergePatchResult(Kind.FAILED, null);

    public static MergePatchResult delta(String delta) {
        return new MergePatchResult(Kind.DELTA, delta);
    }

    public static MergePatchResult unchanged() {
        return UNCHANGED;
    }

    public static MergePatchResult failed() {
        return FAILED;
    }

    public boolean isDelta() {
        return kind == Kind.DELTA;
    }

    public boolean isFailed() {
        return kind == Kind.FAILED;
    }
}
