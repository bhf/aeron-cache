package com.bhf.aeroncache.codecs.request;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.requests.CacheSubscriptionRequestDetails;
import org.agrona.DirectBuffer;

public interface CountersCacheRequestDecoder<I extends Reusable, K extends Reusable, V extends Reusable> extends CacheRequestDecoder<I,K, ReusableLong>{

    void decodeIncrementCounterRequest(DirectBuffer buffer, int offset, CacheSubscriptionRequestDetails<I> cacheSubscribeRequestDetails);
    void decodeDecrementCounterRequest(DirectBuffer buffer, int offset, CacheSubscriptionRequestDetails<I> cacheSubscribeRequestDetails);
    void decodeSetCounterRequest(DirectBuffer buffer, int offset, CacheSubscriptionRequestDetails<I> cacheSubscribeRequestDetails);
}
