package com.bhf.aeroncache.near;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent.EventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ConcurrentNearCacheManager PATCH_ITEM apply")
class ConcurrentNearCacheManagerPatchTest {

    private static final String CACHE_ID = "patchCache";
    private static final String KEY = "k1";

    @Test
    @DisplayName("merges a PATCH_ITEM delta onto the existing full value")
    void mergesPatchOntoExistingValue() {
        var manager = new ConcurrentNearCacheManager<Object>();

        // Baseline full value (as a patch subscriber would receive on hydration), then a delta.
        manager.handleCacheUpdate(CACHE_ID, EventType.ADD_ITEM, KEY, "{\"a\":1,\"b\":2}");
        manager.handleCacheUpdate(CACHE_ID, EventType.PATCH_ITEM, KEY, "{\"b\":3,\"c\":4}");

        assertEquals("{\"a\":1,\"b\":3,\"c\":4}", manager.get(CACHE_ID).get(KEY));
    }

    @Test
    @DisplayName("applies successive deltas cumulatively")
    void appliesSuccessiveDeltas() {
        var manager = new ConcurrentNearCacheManager<Object>();

        manager.handleCacheUpdate(CACHE_ID, EventType.ADD_ITEM, KEY, "{\"a\":1}");
        manager.handleCacheUpdate(CACHE_ID, EventType.PATCH_ITEM, KEY, "{\"b\":2}");
        manager.handleCacheUpdate(CACHE_ID, EventType.PATCH_ITEM, KEY, "{\"c\":3}");

        assertEquals("{\"a\":1,\"b\":2,\"c\":3}", manager.get(CACHE_ID).get(KEY));
    }

    @Test
    @DisplayName("treats a PATCH_ITEM as the whole value when there is no prior baseline")
    void treatsPatchAsWholeWhenNoBaseline() {
        var manager = new ConcurrentNearCacheManager<Object>();

        manager.handleCacheUpdate(CACHE_ID, EventType.PATCH_ITEM, KEY, "{\"a\":1}");

        assertEquals("{\"a\":1}", manager.get(CACHE_ID).get(KEY));
    }
}
