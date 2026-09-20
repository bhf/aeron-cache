package com.bhf.aeroncache.pool;

import com.bhf.aeroncache.models.Reusable;

/**
 * A pool of {@link Reusable} instances that lets callers acquire objects for use and release them
 * back once they are done, avoiding repeated allocation and the associated garbage collection.
 *
 * @param <T> The concrete {@link Reusable} type held by this pool.
 */
public interface ReusableObjectPool<T extends Reusable<?>> {

    /**
     * Acquire an instance from the pool. If the pool is empty a new instance is created on demand.
     *
     * @return An instance ready for use.
     */
    T acquire();

    /**
     * Release an instance back to the pool. The instance is {@link Reusable#clear() cleared} before
     * being made available again. Whether the instance is actually retained by the pool depends on
     * the pool's configuration.
     *
     * @param instance The instance to return to the pool.
     */
    void release(T instance);

    /**
     * The number of instances currently available in the pool for acquisition.
     *
     * @return The available instance count.
     */
    int available();
}
