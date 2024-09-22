package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClearCacheRequestDetails<I> implements Reusable<ClearCacheRequestDetails<I>> {
    I cacheId;

    @Override
    public void clear() {
        cacheId =null;
    }


    @Override
    public void copyFrom(ClearCacheRequestDetails<I> source) {
        this.cacheId =source.getCacheId();
    }
}
