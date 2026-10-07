package com.bhf.aeroncache.clustertools.config;

import com.bhf.aeroncache.utils.ClusterUtils;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.time.Duration;

/**
 * Environment driven configuration for the cluster tools sidecar.
 *
 * <p>All values are resolved from environment variables so the sidecar can be configured the same
 * way whether it runs as a k8s sidecar or embedded in the monolith launcher.
 */
@Getter
@Log4j2
public class ClusterToolsConfig {

    /**
     * Default Aeron term length used by the cluster when nothing is configured; mirrors the default
     * passed in {@code CacheNodeApplication} so the derived archive local control channel matches.
     */
    private static final int DEFAULT_TERM_LENGTH = 64 * 1024;

    private static final String ARCHIVE_LOCAL_CONTROL_ALIAS = "AeronCache-Archive-LocalControl";

    private static final Duration DEFAULT_SNAPSHOT_INTERVAL = Duration.ofHours(6);

    private static final int DEFAULT_RETENTION_COUNT = 2;

    /**
     * The cluster data directory the ClusterTool commands operate against. Null when unset.
     */
    private final String clusterFolder;

    /**
     * The archive directory whose recording segments are purged and whose size is reported. Defaults
     * to a sibling {@code archive} directory next to {@link #clusterFolder}.
     */
    private final File archiveDir;

    /**
     * The Aeron media driver directory shared with the co-located cluster node. Null means use the
     * Aeron default.
     */
    private final String aeronDirectoryName;

    /**
     * The archive local (IPC) control channel the purge Archive client connects over. Defaults to
     * the same channel string the cluster node configures for its archive local control.
     */
    private final String archiveControlChannel;

    /**
     * Whether the in-sidecar scheduled snapshot + purge timer is enabled. Defaults to false so the
     * monolith embedding does not schedule unless explicitly asked to.
     */
    private final boolean scheduleEnabled;

    /**
     * How often the scheduled snapshot + purge runs when {@link #scheduleEnabled} is true.
     */
    private final Duration snapshotInterval;

    /**
     * The number of most recent snapshots whose log history is retained; the purge never removes log
     * segments newer than the Nth most recent snapshot, leaving room for follower catch-up.
     */
    private final int snapshotRetentionCount;

    private ClusterToolsConfig(
            final String clusterFolder,
            final File archiveDir,
            final String aeronDirectoryName,
            final String archiveControlChannel,
            final boolean scheduleEnabled,
            final Duration snapshotInterval,
            final int snapshotRetentionCount) {
        this.clusterFolder = clusterFolder;
        this.archiveDir = archiveDir;
        this.aeronDirectoryName = aeronDirectoryName;
        this.archiveControlChannel = archiveControlChannel;
        this.scheduleEnabled = scheduleEnabled;
        this.snapshotInterval = snapshotInterval;
        this.snapshotRetentionCount = snapshotRetentionCount;
    }

    /**
     * Build configuration from the process environment.
     *
     * @return the resolved configuration.
     */
    public static ClusterToolsConfig fromEnvironment() {
        final String clusterFolder = trimToNull(System.getenv("CLUSTER_FOLDER"));

        final File archiveDir = resolveArchiveDir(clusterFolder, trimToNull(System.getenv("ARCHIVE_DIR")));

        final String aeronDir = trimToNull(System.getenv("AERON_DIR"));

        final int termLength = ClusterUtils.getConfiguredTermLength(DEFAULT_TERM_LENGTH);
        final String defaultControlChannel =
                "aeron:ipc?term-length=" + termLength + "|alias=" + ARCHIVE_LOCAL_CONTROL_ALIAS;
        final String archiveControlChannel =
                orDefault(trimToNull(System.getenv("ARCHIVE_CONTROL_CHANNEL")), defaultControlChannel);

        final boolean scheduleEnabled = Boolean.parseBoolean(
                orDefault(trimToNull(System.getenv("SNAPSHOT_SCHEDULE_ENABLED")), "false"));

        final Duration snapshotInterval = parseInterval(trimToNull(System.getenv("SNAPSHOT_INTERVAL")));

        final int retentionCount = parseRetention(trimToNull(System.getenv("SNAPSHOT_RETENTION_COUNT")));

        final ClusterToolsConfig config = new ClusterToolsConfig(
                clusterFolder, archiveDir, aeronDir, archiveControlChannel,
                scheduleEnabled, snapshotInterval, retentionCount);
        log.info("Resolved cluster tools config: clusterFolder={}, archiveDir={}, aeronDir={}, " +
                        "archiveControlChannel={}, scheduleEnabled={}, snapshotInterval={}, retentionCount={}",
                clusterFolder, archiveDir, aeronDir, archiveControlChannel,
                scheduleEnabled, snapshotInterval, retentionCount);
        return config;
    }

    private static File resolveArchiveDir(final String clusterFolder, final String archiveDirOverride) {
        if (archiveDirOverride != null) {
            return new File(archiveDirOverride);
        }
        if (clusterFolder == null) {
            return null;
        }
        final File parent = new File(clusterFolder).getParentFile();
        return parent == null ? null : new File(parent, "archive");
    }

    private static Duration parseInterval(final String value) {
        if (value == null) {
            return DEFAULT_SNAPSHOT_INTERVAL;
        }
        try {
            return Duration.parse(value);
        } catch (final Exception e) {
            log.error("Could not parse SNAPSHOT_INTERVAL '{}' as an ISO-8601 duration, using default {}",
                    value, DEFAULT_SNAPSHOT_INTERVAL);
            return DEFAULT_SNAPSHOT_INTERVAL;
        }
    }

    private static int parseRetention(final String value) {
        if (value == null) {
            return DEFAULT_RETENTION_COUNT;
        }
        try {
            final int parsed = Integer.parseInt(value);
            if (parsed < 1) {
                log.error("SNAPSHOT_RETENTION_COUNT '{}' must be >= 1, using default {}",
                        value, DEFAULT_RETENTION_COUNT);
                return DEFAULT_RETENTION_COUNT;
            }
            return parsed;
        } catch (final NumberFormatException e) {
            log.error("Could not parse SNAPSHOT_RETENTION_COUNT '{}' as an int, using default {}",
                    value, DEFAULT_RETENTION_COUNT);
            return DEFAULT_RETENTION_COUNT;
        }
    }

    private static String trimToNull(final String value) {
        if (value == null) {
            return null;
        }
        final String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String orDefault(final String value, final String fallback) {
        return value == null ? fallback : value;
    }
}
