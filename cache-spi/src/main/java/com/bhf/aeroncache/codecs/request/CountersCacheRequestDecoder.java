package com.bhf.aeroncache.codecs.request;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.requests.IncrementCounterRequestDetails;
import com.bhf.aeroncache.models.requests.DecrementCounterRequestDetails;
import com.bhf.aeroncache.models.requests.SetCounterRequestDetails;
import org.agrona.DirectBuffer;

public interface CountersCacheRequestDecoder<I extends Reusable, K extends Reusable, V extends Reusable> extends CacheRequestDecoder<I,K, ReusableLong>{

    void decodeIncrementCounterRequest(DirectBuffer buffer, int offset, IncrementCounterRequestDetails<I, K> incrementCounterRequestDetails);
    void decodeDecrementCounterRequest(DirectBuffer buffer, int offset, DecrementCounterRequestDetails<I, K> decrementCounterRequestDetails);
    void decodeSetCounterRequest(DirectBuffer buffer, int offset, SetCounterRequestDetails<I, K> setCounterRequestDetails);
}
