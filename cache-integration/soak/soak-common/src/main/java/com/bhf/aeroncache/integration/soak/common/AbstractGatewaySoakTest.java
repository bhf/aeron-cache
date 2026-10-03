package com.bhf.aeroncache.integration.soak.common;

import com.bhf.aeroncache.gateway.client.GatewayClient;
import com.bhf.aeroncache.utils.ClusterUtils;
import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.CloseHelper;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/**
 * Shared lifecycle harness for gateway-driven soak suites. Concrete subclasses supply the backend the
 * gateway runs against via {@link #startBackend()} (e.g. a real in-process RAFT cluster) and the workload
 * to drive via {@link #createRun(GatewayClient, SoakRecordingListener)}; everything downstream - the
 * dedicated client media driver, the {@link GatewayClient}, the recording listener, the connection
 * warm-up, and writing the metrics summary - lives here and matches the gateway e2e harness, so the soak
 * exercises the same production path.
 *
 * <p>The summary is always written in {@code @AfterAll}, whether the run passed or failed, so a failing
 * run still leaves a triage artifact (including the seed to reproduce it).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractGatewaySoakTest {

    private static final Logger log = LogManager.getLogger(AbstractGatewaySoakTest.class);

    protected static final String REQUEST_ENDPOINT = "localhost:7075";
    protected static final String RESPONSE_CONTROL_ENDPOINT = "localhost:7076";
    protected static final int REQUEST_STREAM_ID = 100;
    protected static final int RESPONSE_STREAM_ID = 101;

    private static final Path REPORT_PATH = Path.of("build", "reports", "soak", "summary.json");

    private MediaDriver clientMediaDriver;
    private Aeron clientAeron;
    private AgentRunner clientRunner;

    protected GatewayClient client;
    protected SoakRecordingListener listener;

    private SoakRun run;

    /** Starts the backend (cluster/ephemeral cache) and the gateway the client will drive. */
    protected abstract void startBackend() throws Exception;

    /** Builds the suite-specific workload. Called once; its {@link SoakRun#report()} must be ready immediately. */
    protected abstract SoakRun createRun(GatewayClient client, SoakRecordingListener listener);

    @BeforeAll
    void startBackendAndClient() throws Exception {
        startBackend();

        // The client uses its own dedicated media driver (its own Aeron directory) to avoid colliding with
        // the gateway's, with the configured term length applied so the request publication matches the
        // cluster/gateway drivers.
        var clientAeronDir = Files.createTempDirectory("soak-client-aeron").toString();
        clientMediaDriver = MediaDriver.launchEmbedded(ClusterUtils.applyConfiguredTermLength(new MediaDriver.Context()
                .aeronDirectoryName(clientAeronDir)
                .threadingMode(ThreadingMode.SHARED)
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true)));
        clientAeron = Aeron.connect(new Aeron.Context()
                .aeronDirectoryName(clientMediaDriver.aeronDirectoryName()));

        listener = new SoakRecordingListener();
        client = new GatewayClient(clientAeron, REQUEST_ENDPOINT, REQUEST_STREAM_ID,
                RESPONSE_CONTROL_ENDPOINT, RESPONSE_STREAM_ID, listener);
        clientRunner = new AgentRunner(new SleepingMillisIdleStrategy(1),
                Throwable::printStackTrace, null, client);
        AgentRunner.startOnThread(clientRunner);

        // The gateway creates a client's response publication lazily, on its first request frame, so
        // isConnected() cannot become true until we send something. Probe until both channels connect.
        await().atMost(120, SECONDS).pollInterval(250, MILLISECONDS).until(() -> {
            client.getStats("connection-warmup");
            return client.isConnected();
        });
        // Drain the warm-up probe responses so they do not accumulate in the listener.
        listener.commandResponses.clear();
        listener.entriesComplete.clear();
        listener.entriesStatus.clear();
        listener.entriesAccumulated.clear();
    }

    @AfterAll
    void stopClient() {
        if (run != null) {
            run.report().writeTo(REPORT_PATH);
            log.info("Wrote soak report to {}", REPORT_PATH.toAbsolutePath());
        }
        CloseHelper.quietClose(clientRunner);
        CloseHelper.quietClose(clientAeron);
        CloseHelper.quietClose(clientMediaDriver);
        // The in-process backend and gateway are torn down when the (forked) test JVM exits.
    }

    @Test
    void soak() {
        run = createRun(client, listener);
        run.run();
    }
}
