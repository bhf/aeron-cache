package com.bhf.aeroncache.clustertools.snapshot;

import com.bhf.aeroncache.http.responses.RecordingPurgeResponse;
import com.bhf.aeroncache.http.responses.SnapshotInfoResponse;
import io.aeron.cluster.ConsensusModule;
import io.aeron.cluster.RecordingLog;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Reads the cluster {@link RecordingLog} to report information about the latest snapshot, plus the
 * current archive directory size on disk. Does not require an Aeron client, so it is safe to call
 * against a live cluster directory.
 */
@Log4j2
public class SnapshotInfoReader {

    private static final int CONSENSUS_SERVICE_ID = ConsensusModule.Configuration.SERVICE_ID;

    /**
     * Read the latest snapshot info from the given cluster directory.
     *
     * @param clusterDir the cluster data directory containing {@code recording.log}.
     * @param archiveDir the archive directory whose on-disk size is reported; may be null.
     * @param lastPurge  the most recent purge result to include, or null when none has run.
     * @return a populated response; {@code snapshotPresent} is false when no valid snapshot exists.
     */
    public SnapshotInfoResponse read(final File clusterDir, final File archiveDir, final RecordingPurgeResponse lastPurge) {
        final long archiveBytes = directorySize(archiveDir);

        if (clusterDir == null || !clusterDir.exists()) {
            log.warn("Cluster directory {} does not exist, cannot read snapshot info", clusterDir);
            return new SnapshotInfoResponse(false, -1, -1, -1, -1, 0, archiveBytes, lastPurge);
        }

        try (RecordingLog recordingLog = new RecordingLog(clusterDir, false)) {
            final RecordingLog.Entry latest = recordingLog.getLatestSnapshot(CONSENSUS_SERVICE_ID);
            final int snapshotCount = countConsensusSnapshots(recordingLog);

            if (latest == null) {
                return new SnapshotInfoResponse(false, -1, -1, -1, -1, snapshotCount, archiveBytes, lastPurge);
            }

            return new SnapshotInfoResponse(
                    true,
                    latest.logPosition,
                    latest.termBaseLogPosition,
                    latest.timestamp,
                    latest.recordingId,
                    snapshotCount,
                    archiveBytes,
                    lastPurge);
        } catch (final Exception e) {
            log.error("Failed to read snapshot info from cluster directory {}", clusterDir, e);
            return new SnapshotInfoResponse(false, -1, -1, -1, -1, 0, archiveBytes, lastPurge);
        }
    }

    private static int countConsensusSnapshots(final RecordingLog recordingLog) {
        int count = 0;
        for (final RecordingLog.Entry entry : recordingLog.entries()) {
            if (entry.isValid
                    && entry.type == RecordingLog.ENTRY_TYPE_SNAPSHOT
                    && entry.serviceId == CONSENSUS_SERVICE_ID) {
                count++;
            }
        }
        return count;
    }

    /**
     * Compute the total size on disk of a directory tree, in bytes. Returns 0 for a null or missing
     * directory, and best-effort skips files that cannot be read.
     *
     * @param dir the directory to size.
     * @return the total size in bytes.
     */
    public static long directorySize(final File dir) {
        if (dir == null || !dir.exists()) {
            return 0L;
        }
        try (Stream<Path> stream = Files.walk(dir.toPath())) {
            return stream.filter(Files::isRegularFile).mapToLong(SnapshotInfoReader::fileSize).sum();
        } catch (final IOException e) {
            log.warn("Failed to compute size of directory {}", dir, e);
            return 0L;
        }
    }

    private static long fileSize(final Path path) {
        try {
            return Files.size(path);
        } catch (final IOException e) {
            return 0L;
        }
    }
}
