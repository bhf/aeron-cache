package com.bhf.aeroncache.models.results;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * The result of requesting all pending TTL removal timers. Carries a
 * {@link TimerDetails} per timer, each tagged with its {@link TimerType} so the
 * caller can tell cache timers from counter timers.
 *
 * @param <I> The type of the cache ID.
 * @param <K> The type of the key.
 */
@Getter
@Setter
@Flyweight
public class AllTimersResult<I extends Reusable, K extends Reusable> implements Reusable<AllTimersResult<I, K>> {

    private CacheOperationStatus operationStatus = CacheOperationStatus.NONE;
    private boolean endOfBatch = true;
    private RequestId requestId = new RequestId();
    private final List<TimerDetails<I, K>> timers = new ArrayList<>();

    public String getRequestId(){
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    @Override
    public void clear() {
        timers.clear();
        requestId.clear();
        endOfBatch = true;
        operationStatus = CacheOperationStatus.NONE;
    }

    @Override
    public void copyFrom(AllTimersResult<I, K> source) {
        this.timers.addAll(source.getTimers());
        this.requestId.copyFrom(source.requestId);
        this.endOfBatch = source.endOfBatch;
        this.operationStatus = source.operationStatus;
    }

    @Override
    public void copyFrom(Reusable<AllTimersResult<I, K>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public AllTimersResult<I, K> value() {
        return this;
    }
}
