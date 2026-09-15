package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Details of a single pending TTL removal timer: the type of cache it targets,
 * the cache and key it will remove, and the epoch deadline at which it fires.
 *
 * @param <I> The type of the cache ID.
 * @param <K> The type of the key.
 */
@Flyweight
@RequiredArgsConstructor
public class TimerDetails<I extends Reusable, K extends Reusable> implements Reusable<TimerDetails<I, K>> {

    @Getter
    private final I cacheId;
    @Getter
    private final K key;
    public TimerType timerType = TimerType.CACHE;
    public long deadline;

    @Override
    public void clear() {
        timerType = TimerType.CACHE;
        deadline = 0;
        cacheId.clear();
        key.clear();
    }

    @Override
    public void copyFrom(TimerDetails<I, K> source) {
        this.timerType = source.timerType;
        this.deadline = source.deadline;
        this.cacheId.copyFrom(source.cacheId);
        this.key.copyFrom(source.key);
    }

    @Override
    public void copyFrom(Reusable<TimerDetails<I, K>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public TimerDetails<I, K> value() {
        return this;
    }
}
