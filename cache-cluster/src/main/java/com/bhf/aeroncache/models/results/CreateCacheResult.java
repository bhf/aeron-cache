package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

/**
 * The result of a request to create a cache. Uses a flyweight pattern.
 */
@Getter
@Setter
public class CreateCacheResult implements Reusable<CreateCacheResult> {

    long timeCreated;

    @Override
    public void clear() {
        timeCreated=0;
    }

    @Override
    public void copyFrom(CreateCacheResult source) {
        this.timeCreated=source.getTimeCreated();
    }
}
