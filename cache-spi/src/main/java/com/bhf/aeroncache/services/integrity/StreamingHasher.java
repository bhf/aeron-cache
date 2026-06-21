package com.bhf.aeroncache.services.integrity;

/**
 * Interface for stream based hashing - useful for data integrity checks.
 *
 * @param <CT> Canonical type we're creating the hash on.
 */
public interface StreamingHasher<CT> {

    /**
     * Add the item to the calculation of the hash.
     * @param item The item to add.
     *
     * @return Fluent reference.
     */
    StreamingHasher<CT> addToHash(CT item);

    StreamingHasher<CT> addToHash(long item);

    /**
     * Reset the state of the hasher.
     */
    void reset();

    /**
     * Get the hash as a long.
     *
     * @return Long representation of the hash so far.
     */
    long getHash();
}
