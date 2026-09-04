package com.bhf.aeroncache.services.integrity;

/**
 * A No Op streaming hasher implementation.
 *
 * @param <CT> The canonical type on which we are calculating hashes.
 */
public class NoOpStreamingHasher<CT> implements StreamingHasher<CT>{
    @Override
    public StreamingHasher<CT> addToHash(CT item) {
        return this;
    }

    @Override
    public StreamingHasher<CT> addToHash(long item) {
        return this;
    }

    @Override
    public void reset() {

    }

    @Override
    public long getHash() {
        return 0;
    }
}
