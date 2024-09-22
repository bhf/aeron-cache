package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

/**
 * The result of a request to make an addition to a cache. Uses a flyweight pattern.
 */
@Getter
@Setter
public class AddCacheEntryResult implements Reusable<AddCacheEntryResult> {

    boolean entryAdded;

    @Override
    public void clear() {

    }

    @Override
    public void copyFrom(AddCacheEntryResult source) {

    }
}
