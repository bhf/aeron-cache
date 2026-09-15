package com.bhf.aeroncache.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
public class PendingRemove<I extends Reusable, K extends Reusable> {
    final long timerCorrelationId;
    final I cacheToRemoveOn;
    final K keyToRemove;
    final long deadline;
}
