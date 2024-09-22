package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddCacheEntryRequestDetails<I,K,V> implements Reusable<AddCacheEntryRequestDetails<I,K,V>> {

    I cacheId;
    K key;
    V value;

    @Override
    public void clear() {
        cacheId=null;
        key=null;
        value=null;
    }

    @Override
    public void copyFrom(AddCacheEntryRequestDetails<I, K, V> source) {
        this.cacheId=source.getCacheId();
        this.key=source.getKey();
        this.value=source.value;
    }

}
