package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetCacheEntryRequestDetails<I, K> implements Reusable<GetCacheEntryRequestDetails<I,K>> {

    I cacheId;
    K key;

    @Override
    public void clear() {
        cacheId=null;
        key=null;
    }

    @Override
    public void copyFrom(GetCacheEntryRequestDetails<I, K> source) {
        this.cacheId=source.cacheId;
        this.key=source.key;
    }
}
