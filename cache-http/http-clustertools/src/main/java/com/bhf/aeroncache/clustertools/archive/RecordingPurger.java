package com.bhf.aeroncache.clustertools.archive;

import com.bhf.aeroncache.clustertools.config.ClusterToolsConfig;
import com.bhf.aeroncache.clustertools.snapshot.SnapshotInfoReader;
import com.bhf.aeroncache.http.responses.RecordingPurgeResponse;
import io.aeron.archive.client.AeronArchive;
import io.aeron.cluster.ConsensusModule;
import io.aeron.cluster.RecordingLog;
import lombok.extern.log4j.Log4j2;
import org.agrona.concurrent.NoOpLock;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Reclaims disk by purging old segments of the cluster log recording up to a retained snapshot
 * position.
 *
 * <p>Aeron 1.50.0 exposes no {@code purge-segments} CLI command, so this connects an
 * {@link AeronArchive} client to the co-located node's archive (over its IPC local control channel)
 * and calls {@link AeronArchive#purgeSegments(long, long)}. The purge floor is the log position of
 * the Nth most recent snapshot (see {@link ClusterToolsConfig#getSnapshotRetentionCount()}), never
 * the latest, so lagging followers retain enough log to catch up and the previous snapshot stays
 * usable for {@code invalidate-latest-snapshot} rollback.
 *
 * <p>A fresh Archive connection is made per purge and closed afterwards, so a connection failure
 * never leaves the sidecar holding a dead client.
 */
@Log4j2
public class RecordingPurger {

    private static final int CONSENSUS_SERVICE_ID = ConsensusModule.Configuration.SERVICE_ID;

    private final ClusterToolsConfig config;

    public RecordingPurger(final ClusterToolsConfig config) {
        this.config = config;
    }

    /**
     * Purge log recording segments older than the retained snapshot floor.
     *
     * @return the outcome, including bytes reclaimed; {@code success} is false only when an error
     * occurred (a no-op because there is not enough snapshot history is reported as success).
     */
    public RecordingPurgeResponse purge() {
        final long now = System.currentTimeMillis();
        final String clusterFolder = config.getClusterFolder();
        final File archiveDir = config.getArchiveDir();

        if (clusterFolder == null) {
            return failure(now, "CLUSTER_FOLDER is not configured");
        }
        final File clusterDir = new File(clusterFolder);
        if (!clusterDir.exists()) {
            return failure(now, "cluster directory does not exist: " + clusterDir);
        }

        final List<Long> snapshotPositions;
        final long logRecordingId;
        try (RecordingLog recordingLog = new RecordingLog(clusterDir, false)) {
            snapshotPositions = consensusSnapshotPositions(recordingLog);
            logRecordingId = recordingLog.findLastTermRecordingId();
        } catch (final Exception e) {
            log.error("Failed to read recording log for purge in {}", clusterDir, e);
            return failure(now, "failed to read recording log: " + e.getMessage());
        }

        if (logRecordingId == io.aeron.Aeron.NULL_VALUE) {
            return noOp(now, archiveDir, "no log recording found to purge");
        }

        final long bytesBefore = SnapshotInfoReader.directorySize(archiveDir);

        final AeronArchive.Context ctx = new AeronArchive.Context()
                .lock(NoOpLock.INSTANCE)
                .controlRequestChannel(config.getArchiveControlChannel())
                .controlResponseChannel(config.getArchiveControlChannel());
        if (config.getAeronDirectoryName() != null) {
            ctx.aeronDirectoryName(config.getAeronDirectoryName());
        }

        try (AeronArchive archive = AeronArchive.connect(ctx)) {
            final RecordingDescriptor descriptor = lookupRecording(archive, logRecordingId);
            if (descriptor == null) {
                return failure(now, "log recording " + logRecordingId + " not found in archive");
            }

            final PurgePlanner.PurgePlan plan = PurgePlanner.plan(
                    snapshotPositions, config.getSnapshotRetentionCount(),
                    descriptor.startPosition, descriptor.termBufferLength, descriptor.segmentFileLength);

            if (!plan.shouldPurge()) {
                return noOp(now, archiveDir, plan.reason());
            }

            final long alignedFloor = plan.alignedFloor();
            final long deletedSegments = archive.purgeSegments(logRecordingId, alignedFloor);
            final long bytesAfter = SnapshotInfoReader.directorySize(archiveDir);
            final long reclaimed = Math.max(0L, bytesBefore - bytesAfter);
            log.info("Purged {} segments of log recording {} up to position {} ({} bytes reclaimed)",
                    deletedSegments, logRecordingId, alignedFloor, reclaimed);

            return new RecordingPurgeResponse(true, logRecordingId, alignedFloor, reclaimed, bytesAfter, now,
                    "purged " + deletedSegments + " segment(s)");
        } catch (final Exception e) {
            log.error("Failed to purge log recording {} segments", logRecordingId, e);
            return failure(now, "purge failed: " + e.getMessage());
        }
    }

    private static List<Long> consensusSnapshotPositions(final RecordingLog recordingLog) {
        final List<Long> positions = new ArrayList<>();
        for (final RecordingLog.Entry entry : recordingLog.entries()) {
            if (entry.isValid
                    && entry.type == RecordingLog.ENTRY_TYPE_SNAPSHOT
                    && entry.serviceId == CONSENSUS_SERVICE_ID) {
                positions.add(entry.logPosition);
            }
        }
        positions.sort(Long::compare);
        return positions;
    }

    private static RecordingDescriptor lookupRecording(final AeronArchive archive, final long recordingId) {
        final RecordingDescriptor holder = new RecordingDescriptor();
        final int found = archive.listRecording(recordingId, (controlSessionId, correlationId, recId, startTimestamp,
                stopTimestamp, startPosition, stopPosition, initialTermId, segmentFileLength, termBufferLength,
                mtuLength, sessionId, streamId, strippedChannel, originalChannel, sourceIdentity) -> {
            holder.startPosition = startPosition;
            holder.segmentFileLength = segmentFileLength;
            holder.termBufferLength = termBufferLength;
            holder.present = true;
        });
        return found > 0 && holder.present ? holder : null;
    }

    private RecordingPurgeResponse noOp(final long now, final File archiveDir, final String message) {
        log.info("Recording purge no-op: {}", message);
        final long bytes = SnapshotInfoReader.directorySize(archiveDir);
        return new RecordingPurgeResponse(true, -1, -1, 0, bytes, now, message);
    }

    private RecordingPurgeResponse failure(final long now, final String message) {
        return new RecordingPurgeResponse(false, -1, -1, 0, 0, now, message);
    }

    private static final class RecordingDescriptor {
        private long startPosition;
        private int segmentFileLength;
        private int termBufferLength;
        private boolean present;
    }
}
