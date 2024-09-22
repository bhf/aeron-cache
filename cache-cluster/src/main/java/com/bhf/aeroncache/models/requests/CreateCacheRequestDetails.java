package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCacheRequestDetails<I> implements Reusable<CreateCacheRequestDetails<I>> {

    I cacheId;
    @Override
    public void clear() {
        cacheId=null;
    }

    @Override
    public void copyFrom(CreateCacheRequestDetails<I> source) {
        this.cacheId=source.getCacheId();
    }
}
