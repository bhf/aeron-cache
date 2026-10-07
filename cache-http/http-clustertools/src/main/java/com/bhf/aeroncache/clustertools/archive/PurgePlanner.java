package com.bhf.aeroncache.clustertools.archive;

import io.aeron.archive.client.AeronArchive;

import java.util.List;

/**
 * Pure computation of how far the cluster log recording can be purged, given the retained snapshot
 * positions and the recording's segment geometry. Separated from {@link RecordingPurger} so the
 * (segment-boundary) arithmetic can be unit tested without an Aeron client or containers.
 */
public final class PurgePlanner {

    private PurgePlanner() {
    }

    /** The planned purge: whether to purge, the segment-aligned floor, and the reason when not. */
    public record PurgePlan(boolean shouldPurge, long alignedFloor, String reason) {

        static PurgePlan noOp(String reason) {
            return new PurgePlan(false, -1, reason);
        }

        static PurgePlan purgeTo(long alignedFloor) {
            return new PurgePlan(true, alignedFloor, "purge to " + alignedFloor);
        }
    }

    /**
     * Decide the purge floor for a log recording.
     *
     * @param snapshotLogPositionsAscending consensus snapshot log positions, oldest to newest.
     * @param retentionCount                number of most recent snapshots to retain; the floor is
     *                                      the Nth most recent snapshot's position.
     * @param recordingStartPosition        the log recording's current start position.
     * @param termBufferLength              the recording's term buffer length.
     * @param segmentFileLength             the recording's segment file length.
     * @return the plan; {@code shouldPurge} is false when there is not enough snapshot history or the
     * segment-aligned floor is at or before the recording start (nothing whole to remove).
     */
    public static PurgePlan plan(
            final List<Long> snapshotLogPositionsAscending,
            final int retentionCount,
            final long recordingStartPosition,
            final int termBufferLength,
            final int segmentFileLength) {

        if (retentionCount < 1) {
            return PurgePlan.noOp("retention count must be >= 1 but was " + retentionCount);
        }
        final int count = snapshotLogPositionsAscending.size();
        if (count < retentionCount) {
            return PurgePlan.noOp("not enough snapshots to purge: have " + count
                    + ", retaining " + retentionCount);
        }

        final long floorPosition = snapshotLogPositionsAscending.get(count - retentionCount);
        final long alignedFloor = AeronArchive.segmentFileBasePosition(
                recordingStartPosition, floorPosition, termBufferLength, segmentFileLength);

        if (alignedFloor <= recordingStartPosition) {
            return PurgePlan.noOp("nothing to purge: aligned floor " + alignedFloor
                    + " is at or before recording start " + recordingStartPosition);
        }

        return PurgePlan.purgeTo(alignedFloor);
    }
}
