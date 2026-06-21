package com.bhf.aeroncache.services.cachemanager;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.CounterOperationResult;

public interface CountersCacheManager<I extends Reusable, K extends Reusable, V extends Reusable> extends CacheManager<I, K, V> {

    /**
     * Increment a counter by the specified amount.
     *
     * @param cacheId The cacheId/namespace of the counter.
     * @param key     The counter name or key.
     * @param amount  The amount to increment by.
     * @return
     */
    CounterOperationResult<I, K> incrementCounter(I cacheId, K key, long amount);

    /**
     * Decrement a counter by the specified amount.
     *
     * @param cacheId The cacheId/namespace of the counter.
     * @param key     The counter name or key.
     * @param amount  The amount to decrement by.
     * @return
     */
    CounterOperationResult<I, K> decrementCounter(I cacheId, K key, long amount);

    /**
     * Set a counter's value.
     *
     * @param cacheId The cacheId/namespace of the counter.
     * @param key     The counter name or key.
     * @param value  The value the counter should take.
     * @return
     */
    CounterOperationResult<I, K> setCounter(I cacheId, K key, long value);
}
