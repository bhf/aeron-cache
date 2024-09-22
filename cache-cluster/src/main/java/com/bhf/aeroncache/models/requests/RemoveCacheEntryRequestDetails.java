package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RemoveCacheEntryRequestDetails<I,K> implements Reusable<RemoveCacheEntryRequestDetails<I,K>> {

    I cacheId;
    K key;

    @Override
    public void clear() {
        cacheId=null;
        key=null;
    }

    @Override
    public void copyFrom(RemoveCacheEntryRequestDetails<I, K> source) {
        this.cacheId= source.getCacheId();
        this.key=source.getKey();
    }
}
