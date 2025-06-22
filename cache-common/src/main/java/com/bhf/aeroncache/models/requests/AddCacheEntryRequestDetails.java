package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Decoded version of a request to add a cache entry.
 *
 * @param <I> The type the caches are indexed on.
 * @param <K> The type the entries of the caches are indexed on.
 * @param <V> The type of the values held in individual caches.
 */
@Getter
@Setter
@RequiredArgsConstructor
@Flyweight
public class AddCacheEntryRequestDetails<I extends Reusable, K extends Reusable, V extends Reusable> implements Reusable<AddCacheEntryRequestDetails<I, K, V>> {

    final I cacheId;
    final K key;
    final V value;
    final RequestId requestId = new RequestId();

    public String getRequestId() {
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    public void setRequestIdLength(int rawBytesLength) {
        this.requestId.setRawBytesLength(rawBytesLength);
    }

    public byte[] getRequestIdRawBytes() {
        return this.requestId.getRawBytes();
    }

    public int getRequestIdLength() {
        return this.requestId.getRawBytesLength();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        cacheId.clear();
        key.clear();
        value.clear();
        this.requestId.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(AddCacheEntryRequestDetails<I, K, V> source) {
        this.cacheId.copyFrom(source.getCacheId());
        this.key.copyFrom(source.getKey());
        this.value.copyFrom(source.value);
        this.requestId.copyFrom(source.requestId);
    }

    @Override
    public void copyFrom(Reusable<AddCacheEntryRequestDetails<I, K, V>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public AddCacheEntryRequestDetails<I, K, V> value() {
        return this;
    }
}
