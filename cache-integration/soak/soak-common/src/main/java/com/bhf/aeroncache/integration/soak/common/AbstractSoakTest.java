package com.bhf.aeroncache.integration.soak.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Path;

/**
 * Transport-neutral lifecycle harness shared by every soak suite. It owns the parts that do not depend on
 * how the client reaches the cluster: starting the backend ({@link #startBackend()}), building the
 * suite-specific workload ({@link #createRun()}), driving the single timed {@link #soak()} test, and - always,
 * pass or fail - writing the metrics summary so a failing run still leaves a triage artifact (including the
 * seed to reproduce it).
 *
 * <p>How the client connects is left to subclasses via the {@link #startClient()} / {@link #stopClient()}
 * hooks: {@link AbstractGatewaySoakTest} drives the binary Aeron {@code GatewayClient}, while a
 * websocket-based suite opens a socket instead. The report write in {@code @AfterAll} happens before
 * {@link #stopClient()} so the summary is captured even if client teardown throws.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractSoakTest {

    private static final Logger log = LogManager.getLogger(AbstractSoakTest.class);

    private static final Path REPORT_PATH = Path.of("build", "reports", "soak", "summary.json");

    private SoakRun run;

    /** Starts the backend (cluster/ephemeral cache plus whatever gateway edge the client drives). */
    protected abstract void startBackend() throws Exception;

    /** Builds the suite-specific workload. Called once; its {@link SoakRun#report()} must be ready immediately. */
    protected abstract SoakRun createRun();

    /** Hook: stand up the client the workload drives (e.g. the gateway client, or a websocket). */
    protected void startClient() throws Exception {
    }

    /** Hook: tear down whatever {@link #startClient()} created. Always called, even if the run failed. */
    protected void stopClient() {
    }

    @BeforeAll
    void startBackendAndClient() throws Exception {
        startBackend();
        startClient();
    }

    @AfterAll
    void teardown() {
        if (run != null) {
            run.report().writeTo(REPORT_PATH);
            log.info("Wrote soak report to {}", REPORT_PATH.toAbsolutePath());
        }
        stopClient();
        // The in-process backend is torn down when the (forked) test JVM exits.
    }

    @Test
    void soak() {
        run = createRun();
        run.run();
    }
}
