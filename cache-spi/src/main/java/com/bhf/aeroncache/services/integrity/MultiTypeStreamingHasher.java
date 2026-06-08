package com.bhf.aeroncache.services.integrity;

public interface MultiTypeStreamingHasher<CT1, CT2> {

    /**
     * Add the item to the calculation of the hash.
     * @param item The item to add.
     *
     * @return Fluent reference.
     */
    MultiTypeStreamingHasher<CT1, CT2> addFirstTypeToHash(CT1 item);

    /**
     * Add the item to the calculation of the hash.
     * @param item The item to add.
     *
     * @return Fluent reference.
     */
    MultiTypeStreamingHasher<CT1, CT2> addSecondTypeToHash(CT2 item);

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
