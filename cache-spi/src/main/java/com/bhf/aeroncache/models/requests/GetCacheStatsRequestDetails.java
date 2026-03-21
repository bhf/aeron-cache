package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Decoded version of a request to get all cache stats.
 *
 */
@Getter
@Setter
@RequiredArgsConstructor
@Flyweight
public class GetCacheStatsRequestDetails implements Reusable<GetCacheStatsRequestDetails> {

    final RequestId requestId = new RequestId();

    public String getRequestId(){
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        this.requestId.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(GetCacheStatsRequestDetails source) {
        this.requestId.copyFrom(source.requestId);
    }

    @Override
    public void copyFrom(Reusable<GetCacheStatsRequestDetails> source) {
        this.copyFrom(source.value());
    }

    @Override
    public GetCacheStatsRequestDetails value() {
        return this;
    }
}
