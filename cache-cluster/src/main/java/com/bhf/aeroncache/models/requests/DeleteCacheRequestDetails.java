package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteCacheRequestDetails<I> implements Reusable<DeleteCacheRequestDetails<I>> {

    I cacheId;
    @Override
    public void clear() {
        cacheId=null;
    }

    @Override
    public void copyFrom(DeleteCacheRequestDetails<I> source) {
        this.cacheId=source.cacheId;
    }
}
