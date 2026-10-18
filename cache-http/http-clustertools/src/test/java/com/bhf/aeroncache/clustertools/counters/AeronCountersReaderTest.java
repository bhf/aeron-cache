package com.bhf.aeroncache.clustertools.counters;

import com.bhf.aeroncache.http.responses.AeronCounter;
import com.bhf.aeroncache.http.responses.AeronCountersResponse;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Scrapes a real embedded media driver's CnC file and asserts the reader returns the driver's own
 * counters. This exercises the full map-CnC / {@code CountersReader} path against a live driver
 * rather than a hand-rolled buffer.
 */
class AeronCountersReaderTest {

    @TempDir
    Path tempDir;

    private MediaDriver mediaDriver;
    private String aeronDirectoryName;

    @BeforeEach
    void startDriver() {
        aeronDirectoryName = tempDir.resolve("aeron").toString();
        mediaDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()
                .aeronDirectoryName(aeronDirectoryName)
                .threadingMode(ThreadingMode.SHARED)
                .sharedIdleStrategy(new BusySpinIdleStrategy())
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true));
        aeronDirectoryName = mediaDriver.aeronDirectoryName();
    }

    @AfterEach
    void stopDriver() {
        if (mediaDriver != null) {
            mediaDriver.close();
        }
    }

    @Test
    @DisplayName("Scrapes the running media driver's counters from its CnC file")
    void scrapesDriverCounters() {
        AeronCountersResponse response = AeronCountersReader.read(aeronDirectoryName);

        assertNotNull(response);
        assertEquals(aeronDirectoryName, response.aeronDirectoryName());
        assertTrue(response.cncVersion() > 0, "expected a populated CnC version");
        assertTrue(response.count() > 0, "a running driver always publishes system counters");
        assertEquals(response.count(), response.counters().size());

        for (AeronCounter counter : response.counters()) {
            assertNotNull(counter.label());
            assertFalse(counter.label().isBlank(), "driver counters are always labelled");
        }
    }

    @Test
    @DisplayName("Fails cleanly when the aeron directory has no CnC file")
    void failsWhenNoCncFile() {
        String emptyDir = tempDir.resolve("no-driver-here").toString();
        assertTrue(new File(emptyDir).mkdirs());

        assertThrows(RuntimeException.class, () -> AeronCountersReader.read(emptyDir));
    }

    @Test
    @DisplayName("Instance read uses the configured directory")
    void instanceReadDoesNotThrow() {
        assertDoesNotThrow(() -> AeronCountersReader.read(aeronDirectoryName));
    }
}
