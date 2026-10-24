package com.bhf.aeroncache.clustertools.schedule.check;

import com.bhf.aeroncache.http.responses.AeronCounter;
import com.bhf.aeroncache.http.responses.AeronCountersResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.bhf.aeroncache.clustertools.schedule.check.CacheStateChangeSnapshotPurgeCheck.CACHE_REQUEST_COUNTER_TYPE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the state-change-threshold decision logic of {@link CacheStateChangeSnapshotPurgeCheck},
 * feeding it counter snapshots directly rather than scraping a live media driver.
 */
class CacheStateChangeSnapshotPurgeCheckTest {

    private static final Duration RATE = Duration.ofSeconds(30);
    private static final long THRESHOLD = 100L;

    private static AeronCounter counter(final int typeId, final long value, final String label) {
        return new AeronCounter(0, typeId, value, label);
    }

    private static AeronCounter stateChange(final String operation, final long value) {
        return counter(CACHE_REQUEST_COUNTER_TYPE_ID, value, "AeronCache-Requests-" + operation + "-node0");
    }

    private static AeronCountersResponse snapshot(final AeronCounter... counters) {
        return new AeronCountersResponse("dir", 0, 0, counters.length, List.of(counters));
    }

    @Test
    @DisplayName("First evaluation establishes the baseline and skips")
    void firstEvaluationEstablishesBaseline() {
        var check = new CacheStateChangeSnapshotPurgeCheck(
                RATE, THRESHOLD, () -> snapshot(stateChange("AddCacheEntry", 500)));

        var decision = check.evaluate();

        assertFalse(decision.proceed());
        assertTrue(decision.reason().contains("establishing baseline"));
    }

    @Test
    @DisplayName("Skips while change since the baseline is below the threshold")
    void skipsBelowThreshold() {
        var snapshot = new AtomicReference<>(snapshot(stateChange("AddCacheEntry", 500)));
        var check = new CacheStateChangeSnapshotPurgeCheck(RATE, THRESHOLD, snapshot::get);

        check.evaluate(); // baseline at 500

        snapshot.set(snapshot(stateChange("AddCacheEntry", 599)));
        var decision = check.evaluate();

        assertFalse(decision.proceed());
        assertTrue(decision.reason().contains("below threshold"));
    }

    @Test
    @DisplayName("Proceeds once change since the baseline reaches the threshold, then rebaselines")
    void proceedsAtThresholdThenRebaselines() {
        var snapshot = new AtomicReference<>(snapshot(stateChange("AddCacheEntry", 500)));
        var check = new CacheStateChangeSnapshotPurgeCheck(RATE, THRESHOLD, snapshot::get);

        check.evaluate(); // baseline at 500

        snapshot.set(snapshot(stateChange("AddCacheEntry", 600)));
        assertTrue(check.evaluate().proceed());

        // Baseline advanced to 600; no further change means the next poll skips again.
        assertFalse(check.evaluate().proceed());
    }

    @Test
    @DisplayName("Sums across all state-changing operation counters")
    void sumsAcrossStateChangingOperations() {
        var snapshot = new AtomicReference<>(snapshot(
                stateChange("AddCacheEntry", 40),
                stateChange("RemoveCacheEntry", 30),
                stateChange("SetCounter", 20)));
        var check = new CacheStateChangeSnapshotPurgeCheck(RATE, THRESHOLD, snapshot::get);

        check.evaluate(); // baseline at 90

        // +60 add, +40 remove, +10 set == +110 total, clears the threshold of 100.
        snapshot.set(snapshot(
                stateChange("AddCacheEntry", 100),
                stateChange("RemoveCacheEntry", 70),
                stateChange("SetCounter", 30)));
        assertTrue(check.evaluate().proceed());
    }

    @Test
    @DisplayName("Read-only and foreign counters are ignored")
    void ignoresReadOnlyAndForeignCounters() {
        var snapshot = new AtomicReference<>(snapshot(
                stateChange("AddCacheEntry", 10),
                stateChange("GetCacheEntry", 10_000),          // read op, same type id - excluded
                counter(1, 10_000, "aeron-driver-something")));  // driver counter - excluded
        var check = new CacheStateChangeSnapshotPurgeCheck(RATE, THRESHOLD, snapshot::get);

        check.evaluate(); // baseline counts only the 10 adds

        // Reads surge, mutations do not: must still skip.
        snapshot.set(snapshot(
                stateChange("AddCacheEntry", 10),
                stateChange("GetCacheEntry", 99_999),
                counter(1, 99_999, "aeron-driver-something")));
        assertFalse(check.evaluate().proceed());
    }

    @Test
    @DisplayName("A counter reset (driver restart) rebaselines instead of reporting negative change")
    void rebaselinesOnCounterReset() {
        var snapshot = new AtomicReference<>(snapshot(stateChange("AddCacheEntry", 5_000)));
        var check = new CacheStateChangeSnapshotPurgeCheck(RATE, THRESHOLD, snapshot::get);

        check.evaluate(); // baseline at 5000

        snapshot.set(snapshot(stateChange("AddCacheEntry", 20)));
        var reset = check.evaluate();
        assertFalse(reset.proceed());
        assertTrue(reset.reason().contains("rebaselined"));

        // New baseline is 20; +120 then clears the threshold.
        snapshot.set(snapshot(stateChange("AddCacheEntry", 140)));
        assertTrue(check.evaluate().proceed());
    }

    @Test
    @DisplayName("Exposes its configured schedule rate")
    void exposesScheduleRate() {
        var check = new CacheStateChangeSnapshotPurgeCheck(
                RATE, THRESHOLD, () -> snapshot(stateChange("AddCacheEntry", 0)));

        assertEquals(RATE, check.getScheduleRate());
    }
}
