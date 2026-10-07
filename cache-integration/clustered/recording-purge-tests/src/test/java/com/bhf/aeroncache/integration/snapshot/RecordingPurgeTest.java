package com.bhf.aeroncache.integration.snapshot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the cluster tools sidecar's recording purge: brings up a single-node clustered cache,
 * seeds the known {@link SnapshotFixture}, grows the Raft log across several snapshot rounds, then
 * purges old log segments down to the retained snapshot floor via the sidecar's
 * {@code POST /api/v1/clustertools/} {@code purge} endpoint.
 *
 * <p>It asserts both guarantees: disk is actually reclaimed (bytes drop after the purge), and the
 * purge does not corrupt the live cluster (the fixture is still served afterwards). Reclaiming a
 * whole 128MB archive segment requires pushing more than one segment of log before the retained
 * snapshot, so this is a heavyweight test - the churn defaults push ~144MB per round and the harness
 * gives the node a larger {@code /dev/shm} so the burst does not wedge the media driver.
 *
 * <p>Tunables (system properties, forwarded to the test JVM by the {@code snapshot.} prefix):
 * {@code -Dsnapshot.purge.snapshotRounds} (default 3), {@code -Dsnapshot.purge.retention}
 * (default 2), {@code -Dsnapshot.purge.churnItemsPerRound} (default 2300),
 * {@code -Dsnapshot.purge.churnItemBytes} (default 65536).
 */
class RecordingPurgeTest {

    @Test
    @DisplayName("Reclaims disk by purging old log segments after snapshots, without breaking the cluster")
    void purgesOldRecordingSegments() throws Exception {
        int snapshotRounds = Integer.getInteger("snapshot.purge.snapshotRounds", 3);
        int retention = Integer.getInteger("snapshot.purge.retention", 2);
        int churnItemsPerRound = Integer.getInteger("snapshot.purge.churnItemsPerRound", 2300);
        int churnItemBytes = Integer.getInteger("snapshot.purge.churnItemBytes", 64 * 1024);

        SnapshotClusterHarness.PurgeOutcome outcome = SnapshotClusterHarness.purgeReclaimsDisk(
                snapshotRounds, retention, churnItemsPerRound, churnItemBytes);

        System.out.println("Purge outcome: success=" + outcome.purgeSucceeded()
                + ", purgedToPosition=" + outcome.purgedToPosition()
                + ", reclaimedBytes=" + outcome.reclaimedBytes()
                + ", archiveBytes " + outcome.archiveBytesBefore() + " -> " + outcome.archiveBytesAfter()
                + ", fixtureStillServed=" + outcome.fixtureStillServed()
                + " | " + outcome.purgeResponse());

        assertTrue(outcome.purgeSucceeded(),
                "purge did not succeed: " + outcome.purgeResponse());
        assertTrue(outcome.reclaimedBytes() > 0,
                "expected disk to be reclaimed but reclaimedBytes was " + outcome.reclaimedBytes()
                        + "; the retained snapshot floor did not cross an archive segment boundary");
        assertTrue(outcome.fixtureStillServed(),
                "cluster no longer serves the fixture after purge - purge corrupted live state");
    }
}
