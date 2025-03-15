package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

/**
 * The result of a request to create a cache.
 *
 * @param <I> The type the cache is indexed on.
 */
@Getter
@Setter
public class CreateCacheResult<I> implements Reusable<CreateCacheResult<I>> {

    I cacheId;
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
        cacheId = null;
        requestId.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(CreateCacheResult<I> source) {
        this.cacheId = source.cacheId;
        this.requestId.copyFrom(source.requestId);
    }
}
