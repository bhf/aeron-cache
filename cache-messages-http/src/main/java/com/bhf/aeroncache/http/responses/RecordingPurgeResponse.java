package com.bhf.aeroncache.http.responses;

/**
 * Result of purging old cluster log recording segments after a snapshot.
 *
 * @param success              whether the purge completed without error.
 * @param logRecordingId       the cluster log recording that was purged, or -1 when none was purged.
 * @param purgedToPosition     the segment-aligned log position segments were purged up to.
 * @param reclaimedBytes       bytes reclaimed on disk (archive dir size before minus after).
 * @param archiveDirBytesAfter archive directory size on disk after the purge.
 * @param timestamp            epoch millis when the purge ran.
 * @param message              human readable detail, including the reason when nothing was purged.
 */
public record RecordingPurgeResponse(
        boolean success,
        long logRecordingId,
        long purgedToPosition,
        long reclaimedBytes,
        long archiveDirBytesAfter,
        long timestamp,
        String message) {
}
