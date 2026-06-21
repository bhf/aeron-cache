package com.bhf.aeroncache.services.cluster;

import lombok.Getter;
import lombok.Setter;

public class TimerCorrelationIdProvider {
    @Setter
    @Getter
    private long timerCorrelationId = 0;

    public long getNextId() {
        return ++timerCorrelationId;
    }
}
