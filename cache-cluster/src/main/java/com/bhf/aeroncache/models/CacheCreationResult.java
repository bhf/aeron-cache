package com.bhf.aeroncache.models;

import lombok.Getter;
import lombok.Setter;

/**
 * The result of a request to create a cache. Uses a flyweight pattern.
 */
@Getter
@Setter
public class CacheCreationResult implements Reusable<CacheCreationResult>{

    long timeCreated;

    @Override
    public void clear() {
        timeCreated=0;
    }

    @Override
    public void copyFrom(CacheCreationResult source) {
        this.timeCreated=source.getTimeCreated();
    }
}
