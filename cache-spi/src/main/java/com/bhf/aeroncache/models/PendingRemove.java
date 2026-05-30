package com.bhf.aeroncache.models;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class PendingRemove<I extends Reusable, K extends Reusable> {
    final long timerCorrelationId;
    final I cacheToRemoveOn;
    final K keyToRemove;

    public PendingRemove(long timerCorrelationId, I cacheToRemoveOn, K keyToRemove) {
        this.timerCorrelationId = timerCorrelationId;
        this.cacheToRemoveOn = cacheToRemoveOn;
        this.keyToRemove = keyToRemove;
    }
}
