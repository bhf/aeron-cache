package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

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

    /**
     * Write the cache stats to the buffer.
     *
     * @param buffer to be written too.
     * @param offset at which to start writing.
     * @return the next offset at which to write.
     */
    public int encode(MutableDirectBuffer buffer, int offset) {
        int written = 0;
        buffer.putLong(offset + written, addedCount);
        written += 8;
        buffer.putLong(offset + written, removedCount);
        written += 8;
        buffer.putLong(offset + written, clearedCount);
        written += 8;
        buffer.putLong(offset + written, size);
        written += 8;
        return written + offset;
    }

    /**
     * Decode the stats fields from the buffer.
     *
     * @param buffer to be read from.
     * @param offset at which to start reading.
     * @return the next offset at which to read.
     */
    public int decode(DirectBuffer buffer, int offset) {
        this.addedCount = buffer.getLong(offset);
        this.removedCount = buffer.getLong(offset + 8);
        this.clearedCount = buffer.getLong(offset + 16);
        this.size = buffer.getLong(offset + 24);
        return offset + 32;
    }
}
