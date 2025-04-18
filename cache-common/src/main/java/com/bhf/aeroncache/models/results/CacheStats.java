package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Basic stats on individual cache usage.
 */
@Flyweight
@RequiredArgsConstructor
public class CacheStats<I extends Reusable> implements Reusable<CacheStats<I>> {

    @Getter
    private final I cacheId;
    public long addedCount;
    public long removedCount;
    public long clearedCount;
    public long size;

    @Override
    public void clear() {
        addedCount = 0;
        removedCount = 0;
        clearedCount = 0;
        size = 0;
        cacheId.clear();
    }

    @Override
    public void copyFrom(CacheStats<I> source) {
        addedCount = source.addedCount;
        removedCount = source.removedCount;
        clearedCount = source.clearedCount;
        size = source.size;
        cacheId.copyFrom(source.cacheId);
    }

    @Override
    public void copyFrom(Reusable<CacheStats<I>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public CacheStats<I> value() {
        return this;
    }
}
