package com.bhf.aeroncache.clustertools.snapshot;

import com.bhf.aeroncache.http.responses.RecordingPurgeResponse;
import com.bhf.aeroncache.http.responses.SnapshotInfoResponse;
import io.aeron.cluster.ConsensusModule;
import io.aeron.cluster.RecordingLog;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
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

    /** Timeout for the {@code du} subprocess used to measure allocated disk usage. */
    private static final long DU_TIMEOUT_SECONDS = 5L;

    /**
     * Compute the actual disk space used by a directory tree, in bytes. Returns 0 for a null or
     * missing directory.
     *
     * <p>Reports space actually allocated on disk ({@code du}-style), not the files' apparent length.
     * This matters for the Aeron archive: it pre-allocates each recording segment to the full
     * segment-file length (128 MB by default) as a <em>sparse</em> file, so a nearly empty archive has
     * a large apparent size (hundreds of MB) while occupying almost no disk. The JDK exposes no API for
     * a file's allocated block count, so this shells out to {@code du}, falling back to the apparent
     * size (summed file lengths) when {@code du} is unavailable, e.g. on a non-POSIX platform.
     *
     * @param dir the directory to size.
     * @return the total allocated size in bytes, or the apparent size when it cannot be measured.
     */
    public static long directorySize(final File dir) {
        if (dir == null || !dir.exists()) {
            return 0L;
        }
        final long allocated = allocatedSize(dir);
        return allocated >= 0 ? allocated : apparentSize(dir);
    }

    /**
     * Measure allocated disk usage via {@code du -sk} (block usage in 1 KiB units, supported by both
     * GNU coreutils and busybox; unlike {@code --apparent-size} it counts allocated blocks, so sparse
     * segment files are sized by their real footprint).
     *
     * @param dir the directory to size.
     * @return the allocated size in bytes, or -1 if {@code du} could not be run or parsed.
     */
    private static long allocatedSize(final File dir) {
        try {
            final Process process = new ProcessBuilder("du", "-sk", dir.getAbsolutePath()).start();
            final String firstLine;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                firstLine = reader.readLine();
            }
            if (!process.waitFor(DU_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return -1L;
            }
            if (process.exitValue() != 0 || firstLine == null || firstLine.isBlank()) {
                return -1L;
            }
            // Output is "<kibibytes>\t<path>"; take the leading number.
            final String kib = firstLine.trim().split("\\s+", 2)[0];
            return Long.parseLong(kib) * 1024L;
        } catch (final Exception e) {
            log.debug("du unavailable for {}, falling back to apparent size", dir, e);
            return -1L;
        }
    }

    private static long apparentSize(final File dir) {
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
