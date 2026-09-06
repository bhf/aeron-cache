package com.bhf.aeroncache.application.ephemeral;

import lombok.extern.log4j.Log4j2;
import org.agrona.DeadlineTimerWheel;
import org.agrona.collections.Long2LongHashMap;

import java.util.concurrent.TimeUnit;

/**
 * A timer service for the ephemeral (non-clustered, no snapshotting) cache
 * backed by an Agrona {@link DeadlineTimerWheel}.
 * <p>
 * This mirrors the behaviour of Aeron cluster's {@code WheelTimerService},
 * allowing the stubbed {@link io.aeron.cluster.service.Cluster} used by the
 * {@link EphemeralCacheApplication} to support scheduling and cancelling
 * timers (e.g. for TTL based cache entry removal) without a consensus
 * module.
 * <p>
 * Timers are keyed by the caller supplied {@code correlationId}. Because the
 * {@link DeadlineTimerWheel} allocates its own opaque {@code timerId} per
 * scheduled timer, this service maintains a bidirectional mapping between
 * correlation ids and wheel timer ids so that timers can be cancelled and
 * rescheduled by correlation id.
 * <p>
 * This class is not thread safe. All interaction (schedule, cancel and poll)
 * is expected to happen on the single agent thread that drives the cache.
 */
@Log4j2
public class EphemeralTimerService implements DeadlineTimerWheel.TimerHandler {

    /**
     * Callback invoked when a scheduled timer fires.
     */
    @FunctionalInterface
    public interface ExpiryHandler {
        /**
         * @param correlationId of the timer that expired.
         * @param now           the current time (in the wheel's time unit) at which the timer expired.
         */
        void onTimerEvent(long correlationId, long now);
    }

    private static final int POLL_LIMIT = 20;
    private static final long NULL_VALUE = Long.MAX_VALUE;

    private final ExpiryHandler expiryHandler;
    private final DeadlineTimerWheel timerWheel;
    private final Long2LongHashMap timerIdByCorrelationId = new Long2LongHashMap(NULL_VALUE);
    private final Long2LongHashMap correlationIdByTimerId = new Long2LongHashMap(NULL_VALUE);

    /**
     * Create a timer service with sensible defaults for TTL handling: a
     * millisecond resolution wheel starting at the current wall clock time.
     *
     * @param expiryHandler invoked when a timer fires.
     */
    public EphemeralTimerService(ExpiryHandler expiryHandler) {
        this(expiryHandler, TimeUnit.MILLISECONDS, System.currentTimeMillis(), 16, 256);
    }

    /**
     * @param expiryHandler invoked when a timer fires.
     * @param timeUnit      the time unit of the wheel and of scheduled deadlines.
     * @param startTime     the start time of the wheel in {@code timeUnit}.
     * @param tickResolution the resolution of a single tick of the wheel in {@code timeUnit}.
     * @param ticksPerWheel  the number of ticks per wheel rotation, must be a power of two.
     */
    public EphemeralTimerService(ExpiryHandler expiryHandler, TimeUnit timeUnit, long startTime,
                                 long tickResolution, int ticksPerWheel) {
        this.expiryHandler = expiryHandler;
        this.timerWheel = new DeadlineTimerWheel(timeUnit, startTime, tickResolution, ticksPerWheel);
    }

    /**
     * Schedule a timer to fire at the given deadline for the supplied correlation id.
     * Any existing timer for the same correlation id is cancelled first so that
     * rescheduling replaces the previous timer.
     *
     * @param correlationId identifying the timer.
     * @param deadline      the time (in the wheel's time unit) at which the timer should fire.
     * @return {@code true} always; the signature matches {@link io.aeron.cluster.service.Cluster#scheduleTimer(long, long)}.
     */
    public boolean scheduleTimer(long correlationId, long deadline) {
        cancelTimer(correlationId);

        final long timerId = timerWheel.scheduleTimer(deadline);
        timerIdByCorrelationId.put(correlationId, timerId);
        correlationIdByTimerId.put(timerId, correlationId);

        return true;
    }

    /**
     * Cancel a previously scheduled timer by correlation id.
     *
     * @param correlationId identifying the timer to cancel.
     * @return {@code true} if a timer was found and cancelled, {@code false} otherwise.
     */
    public boolean cancelTimer(long correlationId) {
        final long timerId = timerIdByCorrelationId.remove(correlationId);
        if (NULL_VALUE == timerId) {
            return false;
        }

        timerWheel.cancelTimer(timerId);
        correlationIdByTimerId.remove(timerId);

        return true;
    }

    /**
     * Poll the timer wheel, firing the expiry handler for any timers that have
     * elapsed at or before {@code now}.
     *
     * @param now the current time in the wheel's time unit.
     * @return the number of timers that expired.
     */
    public int poll(long now) {
        int expired = 0;
        do {
            expired += timerWheel.poll(now, this, POLL_LIMIT - expired);
        } while (expired < POLL_LIMIT && timerWheel.currentTickTime() < now);

        return expired;
    }

    @Override
    public boolean onTimerExpiry(TimeUnit timeUnit, long now, long timerId) {
        final long correlationId = correlationIdByTimerId.remove(timerId);
        if (NULL_VALUE != correlationId) {
            timerIdByCorrelationId.remove(correlationId);
            expiryHandler.onTimerEvent(correlationId, now);
        }

        return true;
    }
}
