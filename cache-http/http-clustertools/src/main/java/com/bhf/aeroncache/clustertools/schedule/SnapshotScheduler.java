package com.bhf.aeroncache.clustertools.schedule;

import com.bhf.aeroncache.clustertools.archive.RecordingPurger;
import com.bhf.aeroncache.clustertools.config.ClusterToolsConfig;
import com.bhf.aeroncache.clustertools.process.ClusterToolProcessRunner;
import com.bhf.aeroncache.clustertools.snapshot.SnapshotInfoReader;
import com.bhf.aeroncache.http.responses.RecordingPurgeResponse;
import com.bhf.aeroncache.http.responses.SnapshotInfoResponse;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Periodically takes a cluster snapshot and then purges old log recording segments, reclaiming disk.
 *
 * <p>Each run takes a snapshot via {@link ClusterToolProcessRunner} (which routes to the leader),
 * waits until the snapshot is durable by watching the latest snapshot log position advance in the
 * recording log — the same signal the integration tests trust, rather than the ClusterTool exit code
 * — and only then purges. If the snapshot does not become durable within a timeout, the purge is
 * skipped so segments are never removed without a fresh snapshot to recover from.
 *
 * <p>Disabled unless {@link ClusterToolsConfig#isScheduleEnabled()} is true, so embedding in the
 * monolith does not schedule snapshots implicitly.
 */
@Log4j2
public class SnapshotScheduler {

    private static final String SNAPSHOT_COMMAND = "snapshot";
    private static final long DURABILITY_TIMEOUT_MS = 60_000L;
    private static final long DURABILITY_POLL_MS = 500L;

    private final ClusterToolsConfig config;
    private final ClusterToolProcessRunner toolRunner;
    private final SnapshotInfoReader infoReader;
    private final RecordingPurger purger;
    private final Consumer<RecordingPurgeResponse> purgeResultSink;
    private final ScheduledExecutorService executor;

    public SnapshotScheduler(
            final ClusterToolsConfig config,
            final ClusterToolProcessRunner toolRunner,
            final SnapshotInfoReader infoReader,
            final RecordingPurger purger,
            final Consumer<RecordingPurgeResponse> purgeResultSink) {
        this.config = config;
        this.toolRunner = toolRunner;
        this.infoReader = infoReader;
        this.purger = purger;
        this.purgeResultSink = purgeResultSink;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "snapshot-scheduler");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Start the periodic timer if scheduling is enabled; otherwise do nothing.
     */
    public void start() {
        if (!config.isScheduleEnabled()) {
            log.info("Scheduled snapshot + purge is disabled (SNAPSHOT_SCHEDULE_ENABLED=false)");
            return;
        }
        if (config.getClusterFolder() == null) {
            log.error("Scheduled snapshot + purge enabled but CLUSTER_FOLDER is not set; not scheduling");
            return;
        }
        final long periodMs = config.getSnapshotInterval().toMillis();
        log.info("Scheduling snapshot + purge every {} (retaining {} snapshots)",
                config.getSnapshotInterval(), config.getSnapshotRetentionCount());
        executor.scheduleWithFixedDelay(this::runQuietly, periodMs, periodMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Stop the scheduler, releasing its thread.
     */
    public void stop() {
        executor.shutdownNow();
    }

    private void runQuietly() {
        try {
            snapshotAndPurge();
        } catch (final Throwable t) {
            // Never let an exception kill the scheduled task.
            log.error("Scheduled snapshot + purge run failed", t);
        }
    }

    /**
     * Take a snapshot, wait for it to become durable, then purge old log segments. Also usable for an
     * on-demand snapshot-and-purge request.
     *
     * @return the purge result, which is also published to the result sink.
     */
    public RecordingPurgeResponse snapshotAndPurge() {
        final File clusterDir = new File(config.getClusterFolder());
        final long before = latestSnapshotPosition(clusterDir);

        final int exitCode;
        try {
            exitCode = toolRunner.run(config.getClusterFolder(), SNAPSHOT_COMMAND);
        } catch (final Exception e) {
            return publish(new RecordingPurgeResponse(false, -1, -1, 0, 0,
                    System.currentTimeMillis(), "failed to trigger snapshot: " + e.getMessage()));
        }
        log.info("Snapshot command exited with code {}, awaiting durability", exitCode);

        if (!awaitSnapshotDurable(clusterDir, before)) {
            return publish(new RecordingPurgeResponse(false, -1, -1, 0, 0,
                    System.currentTimeMillis(),
                    "snapshot did not become durable within " + DURABILITY_TIMEOUT_MS + "ms; skipping purge"));
        }

        return publish(purger.purge());
    }

    private boolean awaitSnapshotDurable(final File clusterDir, final long positionBefore) {
        final long deadline = System.currentTimeMillis() + DURABILITY_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            final long current = latestSnapshotPosition(clusterDir);
            if (current > positionBefore) {
                return true;
            }
            try {
                Thread.sleep(DURABILITY_POLL_MS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private long latestSnapshotPosition(final File clusterDir) {
        final SnapshotInfoResponse info = infoReader.read(clusterDir, config.getArchiveDir(), null);
        return info.snapshotPresent() ? info.logPosition() : -1L;
    }

    private RecordingPurgeResponse publish(final RecordingPurgeResponse result) {
        purgeResultSink.accept(result);
        return result;
    }
}
