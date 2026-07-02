package com.bhf.aeroncache.codecs.request;

import org.agrona.MutableDirectBuffer;

public interface CountersCacheRequestEncoder<I, K, V> extends CacheRequestEncoder<I,K,V>{

    int encodeIncrementCounterRequest(String requestId, I cacheId, K counterId, long amount, MutableDirectBuffer msgBuffer);
    int encodeDecrementCounterRequest(String requestId, I cacheId, K counterId, long amount, MutableDirectBuffer msgBuffer);
    int encodeSetCounterRequest(String requestId, I cacheId, K counterId, long counterValue, MutableDirectBuffer msgBuffer);
}
