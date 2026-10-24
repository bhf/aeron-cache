package com.bhf.aeroncache.integration.snapshot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Soak for the {@code RECORDING_GROWTH} scheduled-snapshot strategy: the clustertools sidecar runs its
 * in-process scheduler gated by the recording-growth check, which only snapshots once the archive has
 * grown by a configured number of bytes since the last purge.
 *
 * <p>Asserts both halves of the contract without the test ever calling a snapshot/purge endpoint:
 * while the cache is idle the archive stays below the growth threshold so the check <em>holds off</em>,
 * and once the churn pushes enough log to cross it the scheduler <em>fires</em> a snapshot + purge,
 * leaving the cluster still serving the fixture.
 *
 * <p>This is heavyweight, like the recording-purge soak: it pushes a large burst of log through the
 * cluster, so the node needs an oversized {@code /dev/shm} so the burst does not wedge the media driver.
 * The growth threshold (default 80MB) sits above the idle archive footprint (a few MB after seeding)
 * and comfortably below what the churn adds (~144MB of 64KB values), so the check holds off while idle
 * and fires after the churn. Tune the threshold and churn together if a given image sizes its archive
 * differently. Tunables (system properties, forwarded by the {@code snapshot.} prefix):
 * {@code -Dsnapshot.scheduled.archiveGrowthBytes} (default 83886080), {@code -Dsnapshot.scheduled.churnItems}
 * (default 2300), {@code -Dsnapshot.scheduled.churnItemBytes} (default 65536),
 * {@code -Dsnapshot.scheduled.retention} (default 2).
 */
class ScheduledRecordingGrowthSnapshotTest {

    @Test
    @DisplayName("Scheduler holds off while idle, then snapshots once the archive grows past the threshold")
    void snapshotsWhenRecordingLogGrowsEnough() throws Exception {
        long growthBytes = Long.getLong("snapshot.scheduled.archiveGrowthBytes", 80L * 1024 * 1024);
        int churnItems = Integer.getInteger("snapshot.scheduled.churnItems", 2300);
        int churnItemBytes = Integer.getInteger("snapshot.scheduled.churnItemBytes", 64 * 1024);
        int retention = Integer.getInteger("snapshot.scheduled.retention", 2);

        Map<String, String> strategyEnv = Map.of(
                "SNAPSHOT_CHECK_STRATEGY", "RECORDING_GROWTH",
                "SNAPSHOT_CHECK_POLL_INTERVAL", "PT2S",
                "SNAPSHOT_CHECK_ARCHIVE_GROWTH_BYTES", Long.toString(growthBytes),
                // Irrelevant to this strategy, but set so a misconfiguration can't fall back to a short
                // ALWAYS interval and mask the behaviour under test.
                "SNAPSHOT_INTERVAL", "PT6H");

        SnapshotClusterHarness.ScheduledCheckOutcome outcome = SnapshotClusterHarness.scheduledCheckUnderChurn(
                strategyEnv, retention,
                Duration.ofSeconds(20), Duration.ofMinutes(5),
                churnItems, churnItemBytes, true);

        System.out.println("RECORDING_GROWTH outcome: firedDuringIdle=" + outcome.firedDuringIdle()
                + ", firedAfterChurn=" + outcome.firedAfterChurn()
                + ", idleArchiveBytes=" + outcome.idleArchiveBytes()
                + ", postChurnArchiveBytes=" + outcome.postChurnArchiveBytes()
                + ", threshold=" + growthBytes
                + ", reclaimedBytes=" + outcome.reclaimedBytesAfterChurn()
                + ", snapshotCountAfter=" + outcome.snapshotCountAfter()
                + ", fixtureStillServed=" + outcome.fixtureStillServed()
                + " | " + outcome.lastPurge());

        assertFalse(outcome.firedDuringIdle(),
                "scheduler took a snapshot while idle - the growth check did not hold off below the threshold"
                        + " (idle archive " + outcome.idleArchiveBytes() + " vs threshold " + growthBytes + ")");
        if (!outcome.firedAfterChurn()) {
            // Distinguish a mis-sized test (churn did not grow the archive past the threshold, so the
            // check correctly held off) from a real product failure (it grew past but nothing fired).
            assertTrue(outcome.postChurnArchiveBytes() >= growthBytes,
                    "churn did not grow the archive past the threshold: postChurn=" + outcome.postChurnArchiveBytes()
                            + " < threshold=" + growthBytes + "; raise churnItems/churnItemBytes or lower the threshold");
        }
        assertTrue(outcome.firedAfterChurn(),
                "scheduler never fired after churn grew the archive past the threshold");
        assertTrue(outcome.purgeSucceededAfterChurn(),
                "scheduled snapshot + purge did not succeed: " + outcome.lastPurge());
        assertTrue(outcome.fixtureStillServed(),
                "cluster no longer serves the fixture after the scheduled snapshot + purge");
    }
}
