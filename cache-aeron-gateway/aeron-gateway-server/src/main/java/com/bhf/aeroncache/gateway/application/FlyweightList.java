package com.bhf.aeroncache.gateway.application;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A reusable list of flyweight elements, rebuilt in place on each use to avoid per-use allocation.
 * <p>
 * A backing {@code store} retains element instances up to the high-water mark of any batch seen so far;
 * they are never discarded. Each batch calls {@link #reset()} and then {@link #next()} once per element,
 * which hands back a retained instance (or creates one only when the batch is larger than any before it)
 * and appends it to the {@link #view()} passed on to the encoder. Both the view's backing array and the
 * element instances are reused across batches, so a steady-state batch allocates nothing.
 * <p>
 * Not thread safe: a single instance must only ever be touched by one thread (in this gateway, either the
 * ingress agent thread or the cluster egress thread, never both).
 *
 * @param <T> the flyweight element type.
 */
class FlyweightList<T> {

    private final Supplier<T> factory;
    private final List<T> store = new ArrayList<>();
    private final List<T> view = new ArrayList<>();

    FlyweightList(Supplier<T> factory) {
        this.factory = factory;
    }

    /**
     * Begin a new batch, emptying the view while retaining the pooled element instances.
     */
    void reset() {
        view.clear();
    }

    /**
     * Return the next element for the current batch, reusing a retained instance where possible and
     * appending it to the {@link #view()}. Callers populate the returned instance in place.
     */
    T next() {
        final int index = view.size();
        final T element;
        if (index < store.size()) {
            element = store.get(index);
        } else {
            element = factory.get();
            store.add(element);
        }
        view.add(element);
        return element;
    }

    /**
     * The elements populated since the last {@link #reset()}, in order, for handing to the encoder.
     */
    List<T> view() {
        return view;
    }
}
