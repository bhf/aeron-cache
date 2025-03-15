package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

/**
 * The result of a request to make an addition to a cache.
 *
 * @param <I> The type of the cache ID.
 * @param <K> The type of the key used in the cache.
 */
@Getter
@Setter
public class AddCacheEntryResult<I, K> implements Reusable<AddCacheEntryResult<I, K>> {

    I cacheID;
    boolean entryAdded;
    K entryKey;
    final RequestId requestId = new RequestId();

    public String getRequestId(){
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        entryAdded = false;
        entryKey = null;
        cacheID = null;
        requestId.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(AddCacheEntryResult<I, K> source) {
        this.entryAdded = source.entryAdded;
        this.entryKey = source.entryKey;
        this.cacheID = source.cacheID;
        this.requestId.copyFrom(source.requestId);
    }
}
