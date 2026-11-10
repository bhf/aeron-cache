package com.bhf.aeroncache.services.patch;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.PatchValueResult;

public class JSONPatchProvider<I extends Reusable, K extends Reusable, V extends Reusable> implements ValuePatchProvider<I,K,V>{

    /**
     * Apply a JSON merge patch to an existing value. The patch is applied in-place, modifying the existing value.
     *
     * @param patch
     * @param existingValue
     * @param <V>
     * @return {@code true} if the patch was applied, {@code false} if a valid merge could not be computed (the
     *         existing value is then left untouched).
     */
    @Override
    public <V extends Reusable> boolean applyPatch(V patch, V existingValue){
        return JsonMergePatch.applyMergePatchChecked(existingValue.value().toString(), patch.value().toString())
                .map(mergedJson -> {
                    existingValue.clear();
                    existingValue.copyFrom(mergedJson);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Produce an RFC 7386 JSON merge patch describing the change from a pre-existing value to the newly
     * added value, populating the supplied out-parameter. The outcome is carried on the out-parameter's
     * status:
     * <ul>
     *   <li>{@link CacheOperationStatus#SUCCESS} - a non-empty delta was produced (carried as the entry
     *       value); send it as a patch update.</li>
     *   <li>{@link CacheOperationStatus#UNKNOWN_KEY} - there was no previous value (a first insert), so
     *       there is nothing to diff against. There is no delta, but the caller should still send the whole
     *       new value as an initial baseline, otherwise a patch subscriber that joined before the key
     *       existed would never see the first insert and would hold a stale (empty) view. This mirrors the
     *       near cache's {@code BASELINE} outcome.</li>
     *   <li>{@link CacheOperationStatus#ERROR} - a valid merge patch could not be computed (unparseable
     *       JSON); the caller should fall back to sending the whole value rather than nothing.</li>
     *   <li>{@link CacheOperationStatus#NONE} - the values were equal, so there was no effective change
     *       and no update need be sent.</li>
     * </ul>
     */
    @Override
    public void produceMergePatch(K key, V previousValue, V newValue, PatchValueResult<I, K, V> mergePatchOut) {
        mergePatchOut.clear();
        if (previousValue == null) {
            // No prior value to diff against (a first insert): signal a baseline so the caller sends the
            // whole value as an initial ADD_ITEM rather than nothing. UNKNOWN_KEY reads honestly here - the
            // key held no value before this add.
            mergePatchOut.getEntryKey().copyFrom(key);
            mergePatchOut.setStatus(CacheOperationStatus.UNKNOWN_KEY);
            return;
        }
        mergePatchOut.getEntryKey().copyFrom(key);

        MergePatchResult result =
                JsonMergePatch.computeMergePatch(previousValue.value().toString(), newValue.value().toString());
        switch (result.kind()) {
            case DELTA -> {
                mergePatchOut.getEntryValue().copyFrom(result.delta());
                mergePatchOut.setStatus(CacheOperationStatus.SUCCESS);
            }
            case FAILED -> mergePatchOut.setStatus(CacheOperationStatus.ERROR);
            // UNCHANGED: nothing changed, leave the out-parameter cleared (NONE), no patch update required.
            case UNCHANGED -> { }
        }
    }
}
