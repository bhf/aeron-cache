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

    /**
     * Copy from the source object into this object.
     * @param source
     */
    void copyFrom(Reusable<T> source);

    /**
     * Get the value.
     * @return The underlying value.
     */
    T value();
}
