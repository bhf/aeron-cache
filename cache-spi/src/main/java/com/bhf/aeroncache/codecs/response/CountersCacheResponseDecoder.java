package com.bhf.aeroncache.codecs.response;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.requests.CacheSubscriptionRequestDetails;
import org.agrona.DirectBuffer;

public interface CountersCacheResponseDecoder<I extends Reusable, K extends Reusable, V extends Reusable> extends CacheResponseDecoder<I,K,V>{
    void decodeIncrementCounterResponse(DirectBuffer buffer, int offset, CacheSubscriptionRequestDetails<I> cacheSubscribeRequestDetails);
    void decodeDecrementCounterResponse(DirectBuffer buffer, int offset, CacheSubscriptionRequestDetails<I> cacheSubscribeRequestDetails);
    void decodeSetCounterResponse(DirectBuffer buffer, int offset, CacheSubscriptionRequestDetails<I> cacheSubscribeRequestDetails);
}
