package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

/**
 * The result of a request to make an addition to a cache.
 *
 * @param <K> The type of the key added.
 */
@Getter
@Setter
public class AddCacheEntryResult<K> implements Reusable<AddCacheEntryResult<K>> {

    boolean entryAdded;
    K entryKey;

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        entryAdded=false;
        entryKey=null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(AddCacheEntryResult<K> source) {
        this.entryAdded = source.entryAdded;
        this.entryKey=source.entryKey;
    }
}
