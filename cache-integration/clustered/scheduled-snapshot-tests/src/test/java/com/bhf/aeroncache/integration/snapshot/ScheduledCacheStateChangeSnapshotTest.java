package com.bhf.aeroncache.integration.snapshot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Soak for the {@code CACHE_STATE_CHANGE} scheduled-snapshot strategy: the clustertools sidecar runs
 * its in-process scheduler gated by the cache-state-change check, which reads the custom request
 * counters (type 2000) in the media driver and only snapshots once enough state-changing operations
 * have accrued.
 *
 * <p>Asserts both halves of the contract without the test ever calling a snapshot/purge endpoint:
 * while the cache is idle the check <em>holds off</em> (no scheduled run), and once the churn crosses
 * the configured operation threshold the scheduler <em>fires</em> a snapshot + purge, leaving the
 * cluster still serving the fixture.
 *
 * <p>This is light: the churn is thousands of tiny writes to bump the counters past a low threshold,
 * so it needs no oversized {@code /dev/shm}. Tunables (system properties, forwarded by the
 * {@code snapshot.} prefix): {@code -Dsnapshot.scheduled.stateChangeThreshold} (default 1000),
 * {@code -Dsnapshot.scheduled.churnItems} (default 2000), {@code -Dsnapshot.scheduled.churnItemBytes}
 * (default 64), {@code -Dsnapshot.scheduled.retention} (default 2).
 */
class ScheduledCacheStateChangeSnapshotTest {

    @Test
    @DisplayName("Scheduler holds off while idle, then snapshots once cache state-change ops cross the threshold")
    void snapshotsWhenCacheStateChangesEnough() throws Exception {
        int threshold = Integer.getInteger("snapshot.scheduled.stateChangeThreshold", 1000);
        int churnItems = Integer.getInteger("snapshot.scheduled.churnItems", 2000);
        int churnItemBytes = Integer.getInteger("snapshot.scheduled.churnItemBytes", 64);
        int retention = Integer.getInteger("snapshot.scheduled.retention", 2);

        Map<String, String> strategyEnv = Map.of(
                "SNAPSHOT_CHECK_STRATEGY", "CACHE_STATE_CHANGE",
                "SNAPSHOT_CHECK_POLL_INTERVAL", "PT2S",
                "SNAPSHOT_CHECK_STATE_CHANGE_THRESHOLD", Integer.toString(threshold),
                // Irrelevant to this strategy, but set so a misconfiguration can't fall back to a short
                // ALWAYS interval and mask the behaviour under test.
                "SNAPSHOT_INTERVAL", "PT6H");

        SnapshotClusterHarness.ScheduledCheckOutcome outcome = SnapshotClusterHarness.scheduledCheckUnderChurn(
                strategyEnv, retention,
                Duration.ofSeconds(20), Duration.ofMinutes(3),
                churnItems, churnItemBytes, false);

        System.out.println("CACHE_STATE_CHANGE outcome: firedDuringIdle=" + outcome.firedDuringIdle()
                + ", firedAfterChurn=" + outcome.firedAfterChurn()
                + ", snapshotCountAfter=" + outcome.snapshotCountAfter()
                + ", fixtureStillServed=" + outcome.fixtureStillServed()
                + " | " + outcome.lastPurge());

        assertFalse(outcome.firedDuringIdle(),
                "scheduler took a snapshot while idle - the state-change check did not hold off below threshold");
        assertTrue(outcome.firedAfterChurn(),
                "scheduler never fired after churn crossed the state-change threshold");
        assertTrue(outcome.purgeSucceededAfterChurn(),
                "scheduled snapshot + purge did not succeed: " + outcome.lastPurge());
        assertTrue(outcome.fixtureStillServed(),
                "cluster no longer serves the fixture after the scheduled snapshot + purge");
    }
}
