package com.bhf.aeroncache.codecs.response;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import org.agrona.MutableDirectBuffer;

public interface CountersCacheResponseEncoder<I extends Reusable, K extends Reusable, V extends Reusable> extends CacheResponseEncoder<I,K,V>{
    int encodeIncrementResult(I cacheId, CreateCacheResult<I> cacheCreationResult, MutableDirectBuffer egressBuffer);
    int encodeDecrementResult(I cacheId, CreateCacheResult<I> cacheCreationResult, MutableDirectBuffer egressBuffer);
    int encodeSetCounterResult(I cacheId, CreateCacheResult<I> cacheCreationResult, MutableDirectBuffer egressBuffer);
}
