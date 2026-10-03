package com.bhf.aeroncache.integration.soak.bidi;

import com.bhf.aeroncache.integration.soak.common.SoakReport;
import com.bhf.aeroncache.integration.soak.common.SoakRun;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * The bidi-websocket soak workload: drives both halves of the bidi protocol over one shared report and seed.
 * It runs a command/oracle phase ({@link BidiCommandSoakPhase}) then a streaming-coverage phase
 * ({@link BidiStreamingSoakPhase}), each time-bounded by its own slice of the configured duration, so a single
 * run exercises the full bidi command surface <em>and</em> the subscription matrix.
 *
 * <p>Both phases draw from one {@link Random} seeded from the config, applied sequentially by this single
 * workload thread, so the whole run is reproducible given {@code -Psoak.seed}. The command phase mutates over
 * the {@code mutator} socket; the streaming phase mutates over {@code mutator} and subscribes/observes over a
 * separate {@code subscriber} socket (see {@link BidiStreamingSoakPhase}).
 */
final class BidiSoakWorkload implements SoakRun {

    private static final Logger log = LogManager.getLogger(BidiSoakWorkload.class);

    private final BidiSoakClient mutator;
    private final BidiSoakClient subscriber;
    private final BidiSoakConfig cfg;
    private final SoakReport report;
    private final Random rnd;

    BidiSoakWorkload(BidiSoakClient mutator, BidiSoakClient subscriber, BidiSoakConfig cfg) {
        this.mutator = mutator;
        this.subscriber = subscriber;
        this.cfg = cfg;
        this.report = new SoakReport(cfg.seed, cfg.totalDurationSeconds(), cfg.toConfigMap());
        this.rnd = new Random(cfg.seed);
    }

    @Override
    public SoakReport report() {
        return report;
    }

    @Override
    public void run() {
        log.info("Starting bidi soak run with {}", cfg);

        var commandDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(cfg.commandDurationSeconds);
        new BidiCommandSoakPhase(mutator, cfg, report, rnd).run(commandDeadline);

        var streamingDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(cfg.streamingDurationSeconds);
        new BidiStreamingSoakPhase(mutator, subscriber, cfg, report, rnd).run(streamingDeadline);

        log.info("Bidi soak run complete: {}", report.progressLine());
    }
}
