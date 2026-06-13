package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TimerDetailsFlyweight<I extends Reusable, K extends Reusable> {
    I cache;
    K key;
    long correlationId;
}
