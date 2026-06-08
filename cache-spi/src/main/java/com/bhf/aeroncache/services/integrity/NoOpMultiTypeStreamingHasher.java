package com.bhf.aeroncache.services.integrity;

public class NoOpMultiTypeStreamingHasher<CT1,CT2> implements MultiTypeStreamingHasher<CT1, CT2>{
    @Override
    public MultiTypeStreamingHasher<CT1, CT2> addFirstTypeToHash(CT1 item) {
        return this;
    }

    @Override
    public MultiTypeStreamingHasher<CT1, CT2> addSecondTypeToHash(CT2 item) {
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
