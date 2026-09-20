package com.bhf.aeroncache.pool;

import com.bhf.aeroncache.models.Reusable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * A {@link ReusableObjectPool} intended for single threaded use only. No synchronisation is
 * performed, so an instance must not be shared across threads.
 *
 * <p>The pool is pre-populated with {@code initialSize} instances. When {@link #acquire()} is
 * called on an empty pool a new instance is created on demand via the supplied factory.
 *
 * <p>The {@code recycleNewObjects} flag controls what happens to instances created on demand once
 * they are released:
 * <ul>
 *     <li>When {@code true}, every released instance is added back to the pool, allowing the pool
 *     to grow beyond its initial size.</li>
 *     <li>When {@code false}, a released instance is only retained while the pool holds fewer than
 *     {@code initialSize} instances; any extra instances created under load are discarded on
 *     release and left for garbage collection.</li>
 * </ul>
 *
 * @param <T> The concrete {@link Reusable} type held by this pool.
 */
public class DequeReusableObjectPool<T extends Reusable> implements ReusableObjectPool<T> {

    private final Supplier<? extends T> factory;
    private final Deque<T> pool;
    private final int initialSize;
    private final boolean recycleNewObjects;

    /**
     * Create a new pool.
     *
     * @param factory           Factory used to create new instances, both to pre-populate the pool
     *                          and to satisfy acquisitions when the pool is empty.
     * @param initialSize       The number of instances to pre-create; must not be negative.
     * @param recycleNewObjects Whether instances created on demand (while the pool is empty) should
     *                          be added back to the pool when released, growing it beyond
     *                          {@code initialSize}.
     */
    public DequeReusableObjectPool(final Supplier<? extends T> factory,
                                   final int initialSize,
                                   final boolean recycleNewObjects) {
        if (factory == null) {
            throw new IllegalArgumentException("factory must not be null");
        }
        if (initialSize < 0) {
            throw new IllegalArgumentException("initialSize must not be negative");
        }
        this.factory = factory;
        this.initialSize = initialSize;
        this.recycleNewObjects = recycleNewObjects;
        this.pool = new ArrayDeque<>(Math.max(initialSize, 1));
        for (int i = 0; i < initialSize; i++) {
            pool.addLast(factory.get());
        }
    }

    @Override
    public T acquire() {
        final T instance = pool.pollLast();
        if (instance == null) {
            return factory.get();
        }
        return instance;
    }

    @Override
    public void release(final T instance) {
        if (instance == null) {
            return;
        }
        instance.clear();
        if (recycleNewObjects || pool.size() < initialSize) {
            pool.addLast(instance);
        }
    }

    @Override
    public int available() {
        return pool.size();
    }
}
