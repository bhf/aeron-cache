package com.bhf.aeroncache.clustertools.schedule.check;

import com.bhf.aeroncache.http.responses.AeronCounter;
import com.bhf.aeroncache.http.responses.AeronCountersResponse;
import lombok.extern.log4j.Log4j2;

import java.time.Duration;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A {@link SnapshotPurgeCheck} that decides based on how much the cache's state has actually changed,
 * read from the custom request counters the cache registers in the Aeron media driver (the same
 * counters the clustertools {@code /counters} endpoint exposes).
 *
 * <p>The cache registers one monotonic counter per request operation under type id
 * {@link #CACHE_REQUEST_COUNTER_TYPE_ID}, with labels like {@code AeronCache-Requests-AddCacheEntry-node0}.
 * This check sums the counters that represent <em>state-changing</em> operations (adds, removes, clears,
 * patches, counter mutations, ...) and takes a scheduled snapshot + purge only once that sum has advanced
 * by a configured number of operations since the last snapshot it approved. Read-only operations
 * (gets, stats, subscriptions) are excluded, so a busy-but-read-only cache is not snapshotted needlessly.
 *
 * <p>Because reading counters is cheap (the clustertools reader maps the driver's CnC file read-only),
 * this check is designed to poll frequently via a short {@link #getScheduleRate()} and only occasionally
 * decide to proceed.
 *
 * <p><strong>State:</strong> the check remembers the operation total at the last snapshot it approved and
 * measures change against that baseline, so "change since the last snapshot" accumulates across polls and
 * resets when a snapshot is taken. It is stateful and is only safe to call from the scheduler's single
 * timer thread (which is how {@code SnapshotScheduler} uses it). The baseline advances whenever the check
 * approves a run; if that run's snapshot then fails, the next threshold crossing simply re-triggers.
 *
 * <p>If the counters total drops (for example the media driver restarted and its counters reset), the
 * baseline is re-established and that poll skips, rather than reporting a spurious negative delta.
 */
@Log4j2
public final class CacheStateChangeSnapshotPurgeCheck implements SnapshotPurgeCheck {

    /**
     * Type id the cache registers its request counters under in the media driver. Must match
     * {@code AeronCounterTracingService.COUNTER_TYPE_ID} (duplicated here because that class lives in a
     * module this sidecar does not depend on).
     */
    public static final int CACHE_REQUEST_COUNTER_TYPE_ID = 2000;

    private static final String REQUEST_LABEL_PREFIX = "AeronCache-Requests-";

    /**
     * Operation labels (as used in the counter labels) whose counters represent a change to cache state.
     * Mirrors the state-changing entries of {@code AeronCounterTracingService.Operation}; read-only and
     * subscription operations are deliberately omitted.
     */
    private static final Set<String> STATE_CHANGING_OPERATIONS = Set.of(
            "DeleteCache",
            "ClearCache",
            "RemoveCacheEntry",
            "AddCacheEntry",
            "CreateCache",
            "BulkOps",
            "PatchValue",
            "CancelItemRemoval",
            "IncrementCounter",
            "DecrementCounter",
            "SetCounter");

    private static final long UNSET = -1L;

    private final Duration scheduleRate;
    private final long operationThreshold;
    private final Supplier<AeronCountersResponse> countersSupplier;
    private final Predicate<AeronCounter> stateChangeCounterFilter;

    private long lastApprovedTotal = UNSET;

    /**
     * Create a check using the default filter that selects the cache's state-changing request counters.
     *
     * @param scheduleRate       how often to poll; typically much shorter than a plain snapshot interval.
     * @param operationThreshold minimum number of state-changing operations since the last snapshot
     *                           before a snapshot + purge is taken.
     * @param countersSupplier   reads a snapshot of the media driver counters; typically
     *                           {@code aeronCountersReader::read}.
     */
    public CacheStateChangeSnapshotPurgeCheck(
            final Duration scheduleRate,
            final long operationThreshold,
            final Supplier<AeronCountersResponse> countersSupplier) {
        this(scheduleRate, operationThreshold, countersSupplier, defaultStateChangeCounterFilter());
    }

    /**
     * Create a check with a custom filter selecting which counters count towards cache state change - for
     * example to include every type-id {@value #CACHE_REQUEST_COUNTER_TYPE_ID} counter, or to narrow to a
     * single operation.
     *
     * @param scheduleRate             how often to poll.
     * @param operationThreshold       minimum selected-counter growth before proceeding.
     * @param countersSupplier         reads a snapshot of the media driver counters.
     * @param stateChangeCounterFilter selects which counters represent cache state change.
     */
    public CacheStateChangeSnapshotPurgeCheck(
            final Duration scheduleRate,
            final long operationThreshold,
            final Supplier<AeronCountersResponse> countersSupplier,
            final Predicate<AeronCounter> stateChangeCounterFilter) {
        this.scheduleRate = scheduleRate;
        this.operationThreshold = operationThreshold;
        this.countersSupplier = countersSupplier;
        this.stateChangeCounterFilter = stateChangeCounterFilter;
    }

    /**
     * The default filter: the cache's own request counters (type id
     * {@value #CACHE_REQUEST_COUNTER_TYPE_ID}) for operations that change cache state.
     *
     * @return a predicate selecting state-changing cache request counters.
     */
    public static Predicate<AeronCounter> defaultStateChangeCounterFilter() {
        return counter -> counter.typeId() == CACHE_REQUEST_COUNTER_TYPE_ID
                && isStateChangingRequestLabel(counter.label());
    }

    private static boolean isStateChangingRequestLabel(final String label) {
        if (label == null || !label.startsWith(REQUEST_LABEL_PREFIX)) {
            return false;
        }
        return STATE_CHANGING_OPERATIONS.stream()
                .anyMatch(operation -> label.startsWith(REQUEST_LABEL_PREFIX + operation + "-node"));
    }

    @Override
    public SnapshotPurgeDecision evaluate() {
        final AeronCountersResponse counters = countersSupplier.get();
        final long total = counters.counters().stream()
                .filter(stateChangeCounterFilter)
                .mapToLong(AeronCounter::value)
                .sum();

        if (lastApprovedTotal == UNSET) {
            lastApprovedTotal = total;
            return SnapshotPurgeDecision.skip(
                    "establishing baseline at " + total + " cache state-change operations");
        }

        if (total < lastApprovedTotal) {
            final long previous = lastApprovedTotal;
            lastApprovedTotal = total;
            return SnapshotPurgeDecision.skip(String.format(
                    "cache state-change operations dropped from %d to %d (media driver restart?); rebaselined",
                    previous, total));
        }

        final long delta = total - lastApprovedTotal;
        if (delta < operationThreshold) {
            return SnapshotPurgeDecision.skip(String.format(
                    "%d cache state-change operations since last snapshot, below threshold %d",
                    delta, operationThreshold));
        }

        lastApprovedTotal = total;
        return SnapshotPurgeDecision.proceed(String.format(
                "%d cache state-change operations since last snapshot, at or above threshold %d",
                delta, operationThreshold));
    }

    @Override
    public Duration getScheduleRate() {
        return scheduleRate;
    }
}
