package com.bhf.aeroncache.codecs.response;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.DecrementCounterResult;
import com.bhf.aeroncache.models.results.IncrementCounterResult;
import com.bhf.aeroncache.models.results.SetCounterResult;
import org.agrona.DirectBuffer;

public interface CountersCacheResponseDecoder<I extends Reusable, K extends Reusable, V extends Reusable> extends CacheResponseDecoder<I,K,V>{
    void decodeIncrementCounterResponse(DirectBuffer buffer, int offset, IncrementCounterResult<I, K> result);
    void decodeDecrementCounterResponse(DirectBuffer buffer, int offset, DecrementCounterResult<I, K> result);
    void decodeSetCounterResponse(DirectBuffer buffer, int offset, SetCounterResult<I, K> result);
}
