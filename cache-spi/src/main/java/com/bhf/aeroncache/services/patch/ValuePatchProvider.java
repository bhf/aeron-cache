package com.bhf.aeroncache.services.patch;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.PatchValueResult;

public interface ValuePatchProvider<I extends Reusable, K extends Reusable, V extends Reusable> {

    <V extends Reusable> boolean applyPatch(V patch, V existingValue);

    void produceMergePatch(K key, V previousValue, V newValue, PatchValueResult<I, K, V> mergePatchOut);
}
