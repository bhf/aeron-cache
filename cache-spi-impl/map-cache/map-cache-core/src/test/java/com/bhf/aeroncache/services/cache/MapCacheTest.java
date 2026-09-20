package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.services.cache.snapshot.ReusableStringCacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.ReusableStringCacheIdSnapshotCodec;
import com.bhf.aeroncache.services.integrity.NoOpStreamingHasher;
import com.bhf.aeroncache.services.patch.JSONPatchProvider;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;

/**
 * Verifies {@link MapCache} against the shared {@link AbstractMapCacheContractTest cache contract}.
 * {@link MapCache} stores values directly, so the backing map yields them without unwrapping.
 */
class MapCacheTest extends AbstractMapCacheContractTest<MapCache<ReusableString, ReusableString, ReusableString>> {

    @Override
    protected MapCache<ReusableString, ReusableString, ReusableString> createCache() {
        return new MapCache<>(SupplierUtils.stringSupplier, SupplierUtils.stringSupplier,
                SupplierUtils.stringSupplier, SupplierUtils.mapSupplier,
                new ReusableStringCacheIdSnapshotCodec(new NoOpStreamingHasher<>()),
                new ReusableStringCacheEntrySnapshotCodec(new NoOpStreamingHasher<>()), new JSONPatchProvider<>());
    }

    @Override
    protected String storedValue(ReusableString key) {
        var value = cache.cache.get(key);
        return value == null ? null : value.value();
    }
}
