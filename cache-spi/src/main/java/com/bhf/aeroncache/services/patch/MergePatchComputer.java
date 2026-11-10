package com.bhf.aeroncache.services.patch;

/**
 * Computes an RFC 7386 JSON merge patch describing the change between two whole values.
 */
public interface MergePatchComputer {

    /**
     * Compute the merge-patch delta from {@code previousJson} to {@code newJson}.
     *
     * @param previousJson the value already held, or {@code null}.
     * @param newJson      the new whole value, or {@code null}.
     * @return a {@link MergePatchResult}
     */
    MergePatchResult computeMergePatch(String previousJson, String newJson);
}
