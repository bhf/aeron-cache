package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Flyweight
public class CacheStatsResult<I extends Reusable> implements Reusable<CacheStatsResult<I>> {

    private CacheOperationStatus operationStatus = CacheOperationStatus.NONE;
    private RequestId requestId = new RequestId();
    private final List<CacheStats<I>> stats = new ArrayList<>();

    public String getRequestId(){
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    @Override
    public void clear() {
        stats.clear();
        requestId.clear();
        operationStatus = CacheOperationStatus.NONE;
    }

    @Override
    public void copyFrom(CacheStatsResult<I> source) {
        this.stats.addAll(source.getStats());
        this.requestId.copyFrom(source.requestId);
        this.operationStatus = source.operationStatus;
    }

    @Override
    public void copyFrom(Reusable<CacheStatsResult<I>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public CacheStatsResult<I> value() {
        return this;
    }
}
