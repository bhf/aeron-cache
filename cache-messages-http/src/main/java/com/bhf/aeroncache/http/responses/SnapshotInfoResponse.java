package com.bhf.aeroncache.http.responses;

/**
 * Information about the latest cluster snapshot, surfaced over HTTP for display in the UI.
 *
 * @param snapshotPresent     whether a valid consensus module snapshot exists.
 * @param logPosition         the log position the latest snapshot was taken at.
 * @param termBaseLogPosition the log position at the base of the latest snapshot's leadership term.
 * @param timestamp           the cluster timestamp recorded for the latest snapshot.
 * @param recordingId         the archive recording id holding the latest consensus module snapshot.
 * @param snapshotCount       the number of valid consensus module snapshots currently retained.
 * @param archiveDirBytes     current size of the archive directory on disk, in bytes.
 * @param lastPurge           the result of the most recent purge run, or null if none has run.
 */
public record SnapshotInfoResponse(
        boolean snapshotPresent,
        long logPosition,
        long termBaseLogPosition,
        long timestamp,
        long recordingId,
        int snapshotCount,
        long archiveDirBytes,
        RecordingPurgeResponse lastPurge) {
}
