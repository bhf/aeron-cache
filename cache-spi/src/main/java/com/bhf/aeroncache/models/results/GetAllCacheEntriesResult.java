package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@RequiredArgsConstructor
@Flyweight
public class GetAllCacheEntriesResult<I extends Reusable, K extends Reusable, V extends Reusable> implements Reusable<GetAllCacheEntriesResult<I, K, V>> {

    final I cacheId;

    /**
     * Backing map owned by this flyweight, used when entries are populated into it (for example when a
     * client decodes a response). On the server read path {@link #values} is instead pointed straight
     * at the cache's live map to avoid copying every entry.
     */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    final Map<K, V> ownedValues = new HashMap<>();

    /**
     * The entries carried by this result. By default this is the {@link #ownedValues owned} map, but
     * the server-side get-all path assigns the cache's live map here by reference (see
     * {@code MapCacheManager#getAllCacheEntries}) so no per-entry copy is needed. Callers must only
     * read this map when it holds a live reference; it is reset to the owned map on {@link #clear()}.
     */
    Map<K, V> values = ownedValues;
    final RequestId requestId = new RequestId();
    CacheOperationStatus status = CacheOperationStatus.NONE;
    boolean endOfBatch = true;

    public String getRequestId() {
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    @Override
    public void clear() {
        cacheId.clear();
        // Only clear the owned map and drop any live reference; never clear a referenced live cache map.
        ownedValues.clear();
        values = ownedValues;
        requestId.clear();
        status = CacheOperationStatus.NONE;
        endOfBatch = true;
    }

    @Override
    public void copyFrom(GetAllCacheEntriesResult<I, K, V> source) {
        this.cacheId.copyFrom(source.cacheId);
        this.values.putAll(source.values);
        this.requestId.copyFrom(source.requestId);
        this.status = source.status;
        this.endOfBatch = source.endOfBatch;
    }

    @Override
    public void copyFrom(Reusable<GetAllCacheEntriesResult<I, K, V>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public GetAllCacheEntriesResult<I, K, V> value() {
        return this;
    }
}
