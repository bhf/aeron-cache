package com.bhf.aeroncache.models;

/**
 * Interface for reusable objects.
 *
 * @param <T> The type of the Reusable.
 */
public interface Reusable<T> {

    /**
     * Clear the state of the instance.
     */
    void clear();

    /**
     * Copy from the source object into this object.
     * @param source The source from which we are copying state.
     */
    void copyFrom(T source);
}
