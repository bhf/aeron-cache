package com.bhf.aeroncache.clustertools.schedule.check;

import com.bhf.aeroncache.http.responses.RecordingPurgeResponse;
import com.bhf.aeroncache.http.responses.SnapshotInfoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the growth-threshold decision logic of {@link RecordingGrowthSnapshotPurgeCheck},
 * using plain suppliers instead of a live cluster or archive.
 */
class RecordingGrowthSnapshotPurgeCheckTest {

    private static final Duration RATE = Duration.ofMinutes(1);
    private static final long THRESHOLD = 1_000L;

    private static SnapshotInfoResponse archiveOf(final long bytes) {
        return new SnapshotInfoResponse(true, 0, 0, 0, 0, 1, bytes, null);
    }

    private static RecordingPurgeResponse purgedTo(final long archiveBytesAfter) {
        return new RecordingPurgeResponse(true, 1, 0, 0, archiveBytesAfter, 0, "ok");
    }

    @Test
    @DisplayName("Skips when growth since the last purge baseline is below the threshold")
    void skipsBelowThreshold() {
        var check = new RecordingGrowthSnapshotPurgeCheck(
                RATE, THRESHOLD, () -> archiveOf(5_500L), () -> purgedTo(5_000L));

        var decision = check.evaluate();

        assertFalse(decision.proceed());
        assertTrue(decision.reason().contains("below threshold"));
    }

    @Test
    @DisplayName("Proceeds when growth since the last purge baseline reaches the threshold")
    void proceedsAtThreshold() {
        var check = new RecordingGrowthSnapshotPurgeCheck(
                RATE, THRESHOLD, () -> archiveOf(6_000L), () -> purgedTo(5_000L));

        var decision = check.evaluate();

        assertTrue(decision.proceed());
        assertTrue(decision.reason().contains("at or above threshold"));
    }

    @Test
    @DisplayName("With no prior purge the baseline is zero, so current size alone drives the decision")
    void baselineIsZeroWhenNoPriorPurge() {
        var belowFromZero = new RecordingGrowthSnapshotPurgeCheck(
                RATE, THRESHOLD, () -> archiveOf(900L), () -> null);
        assertFalse(belowFromZero.evaluate().proceed());

        var aboveFromZero = new RecordingGrowthSnapshotPurgeCheck(
                RATE, THRESHOLD, () -> archiveOf(1_000L), () -> null);
        assertTrue(aboveFromZero.evaluate().proceed());
    }

    @Test
    @DisplayName("A failed last purge is ignored for the baseline, falling back to zero")
    void failedPurgeIgnoredForBaseline() {
        var failedPurge = new RecordingPurgeResponse(false, -1, -1, 0, 9_999L, 0, "boom");
        var check = new RecordingGrowthSnapshotPurgeCheck(
                RATE, THRESHOLD, () -> archiveOf(2_000L), () -> failedPurge);

        // Baseline falls back to 0 (not the failed purge's 9_999), so 2_000 of growth clears 1_000.
        assertTrue(check.evaluate().proceed());
    }

    @Test
    @DisplayName("Exposes its configured schedule rate")
    void exposesScheduleRate() {
        var check = new RecordingGrowthSnapshotPurgeCheck(
                RATE, THRESHOLD, () -> archiveOf(0L), () -> null);

        assertEquals(RATE, check.getScheduleRate());
    }
}
