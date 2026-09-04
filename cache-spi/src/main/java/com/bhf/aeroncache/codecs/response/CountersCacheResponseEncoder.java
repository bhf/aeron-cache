package com.bhf.aeroncache.codecs.response;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.DecrementCounterResult;
import com.bhf.aeroncache.models.results.IncrementCounterResult;
import com.bhf.aeroncache.models.results.SetCounterResult;
import org.agrona.MutableDirectBuffer;

public interface CountersCacheResponseEncoder<I extends Reusable, K extends Reusable, V extends Reusable> extends CacheResponseEncoder<I,K,V>{
    int encodeIncrementResult(I cacheId, IncrementCounterResult<I, K> result, MutableDirectBuffer egressBuffer);
    int encodeDecrementResult(I cacheId, DecrementCounterResult<I, K> result, MutableDirectBuffer egressBuffer);
    int encodeSetCounterResult(I cacheId, SetCounterResult<I, K> result, MutableDirectBuffer egressBuffer);
}
