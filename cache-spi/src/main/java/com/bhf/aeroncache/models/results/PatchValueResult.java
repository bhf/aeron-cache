package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * The result of a request to patch the value of a cache entry.
 *
 * @param <I> The type of the cache ID.
 * @param <K> The type of the key of the entry that has been patched.
 * @param <V> The type of the value of the entry that has been patched.
 */
@Getter
@Setter
@RequiredArgsConstructor
@Flyweight
public class PatchValueResult<I extends Reusable, K extends Reusable, V extends Reusable> implements Reusable<PatchValueResult<I, K, V>> {

    final I cacheId;
    final K entryKey;
    final V entryValue;
    final RequestId requestId = new RequestId();
    CacheOperationStatus status = CacheOperationStatus.NONE;

    public String getRequestId() {
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    @Override
    public void clear() {
        cacheId.clear();
        entryKey.clear();
        entryValue.clear();
        requestId.clear();
        status = CacheOperationStatus.NONE;
    }

    @Override
    public void copyFrom(PatchValueResult<I, K, V> source) {
        this.cacheId.copyFrom(source.cacheId);
        this.entryKey.copyFrom(source.entryKey);
        this.entryValue.copyFrom(source.entryValue);
        this.requestId.copyFrom(source.requestId);
        this.status = source.status;
    }

    @Override
    public void copyFrom(Reusable<PatchValueResult<I, K, V>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public PatchValueResult<I, K, V> value() {
        return this;
    }
}
