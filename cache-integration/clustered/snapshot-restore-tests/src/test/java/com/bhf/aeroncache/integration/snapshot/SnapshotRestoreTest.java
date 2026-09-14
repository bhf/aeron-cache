package com.bhf.aeroncache.integration.snapshot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Restore side of the snapshot backward-compatibility harness: boots the current-build cache
 * against a captured data artifact and asserts every {@link SnapshotFixture} entry recovered.
 *
 * <p>Two modes:
 * <ul>
 *   <li><b>Regression</b> - pass {@code -Dsnapshot.restore.artifact=/path/cache-data-vOLD.tar.gz}
 *       (an artifact captured by an <em>older</em> release). Green means the current binary can
 *       still load that older snapshot, i.e. the snapshot format is backward compatible.</li>
 *   <li><b>Round-trip self-test</b> (default, no artifact supplied) - capture an artifact from the
 *       current build, then restore it. Always runnable, so the harness itself is exercised on
 *       every PR even before any release artifact exists.</li>
 * </ul>
 *
 * <p>By default the recording log is reseeded from the snapshot before boot
 * ({@code -Dsnapshot.restore.seedFromSnapshot=false} to disable), so recovery is forced to come
 * from the snapshot rather than command-log replay - the strongest compatibility signal.
 */
class SnapshotRestoreTest {

    @Test
    @DisplayName("Current build recovers all fixture values from a captured data artifact")
    void restoresFixtureFromArtifact() throws Exception {
        boolean seedFromSnapshot =
                Boolean.parseBoolean(System.getProperty("snapshot.restore.seedFromSnapshot", "true"));

        Path artifact = resolveArtifact();
        try (var restored = SnapshotClusterHarness.restore(artifact, seedFromSnapshot)) {
            restored.verifyFixture();
        }
    }

    private static Path resolveArtifact() throws Exception {
        String provided = System.getProperty("snapshot.restore.artifact");
        if (provided != null && !provided.isBlank()) {
            Path artifact = Path.of(provided);
            assertTrue(Files.exists(artifact), "configured artifact does not exist: " + artifact);
            System.out.println("Restoring supplied artifact: " + artifact.toAbsolutePath());
            return artifact;
        }

        // No external artifact - capture one from the current build so the test is self-contained.
        Path artifact = Files.createTempFile("cache-data-roundtrip-", ".tar.gz");
        Files.delete(artifact); // capture (via tar) writes the file itself
        String version = System.getProperty("aeroncache.image.tag", "local");
        System.out.println("No artifact supplied; capturing a round-trip artifact from current build");
        SnapshotClusterHarness.capture(artifact, version);
        return artifact;
    }
}
