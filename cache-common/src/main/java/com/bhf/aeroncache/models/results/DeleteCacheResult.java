package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

/**
 * The result of a request to delete a cache.
 *
 * @param <I> The type of the cache ID.
 */
@Getter
@Setter
public class DeleteCacheResult<I> implements Reusable<DeleteCacheResult<I>> {

    I cacheId;
    final RequestId requestId = new RequestId();

    public String getRequestId() {
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    @Override
    public void clear() {
        cacheId = null;
        this.requestId.clear();
    }

    @Override
    public void copyFrom(DeleteCacheResult<I> source) {
        this.cacheId = source.cacheId;
        this.requestId.copyFrom(source.requestId);
    }
}
