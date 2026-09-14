package com.bhf.aeroncache.integration.snapshot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Capture side of the snapshot backward-compatibility harness: brings up a single-node clustered
 * cache, seeds the known {@link SnapshotFixture}, snapshots it through the product
 * {@code POST /api/v1/snapshot} endpoint, and writes the recovered on-disk cluster directory to a
 * {@code cache-data-<version>.tar.gz} release artifact.
 *
 * <p>The artifact location and recorded version are overridable so the release workflow can place
 * the tarball where it uploads release assets from:
 * <ul>
 *   <li>{@code -Dsnapshot.capture.output=/path/cache-data-v1.2.3.tar.gz}</li>
 *   <li>{@code -Dsnapshot.artifact.version=v1.2.3}</li>
 * </ul>
 */
class SnapshotCaptureTest {

    @Test
    @DisplayName("Captures a seeded, snapshotted single-node cache into a release data artifact")
    void capturesSnapshotArtifact() throws Exception {
        String version = System.getProperty("snapshot.artifact.version",
                System.getProperty("aeroncache.image.tag", "local"));
        Path output = Path.of(System.getProperty("snapshot.capture.output",
                "build/artifacts/cache-data-" + version + ".tar.gz"));

        SnapshotClusterHarness.capture(output, version);

        assertTrue(Files.exists(output), "artifact not produced: " + output);
        assertTrue(Files.size(output) > 0, "artifact is empty: " + output);
        System.out.println("Captured snapshot artifact: " + output.toAbsolutePath()
                + " (" + Files.size(output) + " bytes)");
    }
}
