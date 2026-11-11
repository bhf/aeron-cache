package com.bhf.aeroncache.integration.soak.common;

import com.bhf.aeroncache.gateway.client.GatewayClient;
import com.bhf.aeroncache.gateway.messages.OperationStatus;
import io.aeron.Aeron;

import java.util.concurrent.locks.LockSupport;
import java.util.function.LongSupplier;

/**
 * Applies cache mutations over the binary Aeron {@link GatewayClient} and awaits their acknowledgement - the
 * single-writer mutation path shared by every streaming soak transport (the subscription being observed is a
 * separate seam; see {@link SoakStreamSubscriber}). Every call sends with backpressure retry, blocks for the
 * correlated response and asserts the expected status, so the workload reads as a sequence of plain mutations.
 */
final class GatewayCommandDriver {

    private static final long TTL_NONE = 0L;
    private static final long PARK_NANOS = 50_000L;

    private final GatewayClient client;
    private final SoakRecordingListener listener;
    private final SoakFailures failures;
    private final long opTimeoutNanos;
    private final int opTimeoutSeconds;

    private long corrSeq;

    GatewayCommandDriver(GatewayClient client, SoakRecordingListener listener, SoakFailures failures,
                         long opTimeoutNanos, int opTimeoutSeconds) {
        this.client = client;
        this.listener = listener;
        this.failures = failures;
        this.opTimeoutNanos = opTimeoutNanos;
        this.opTimeoutSeconds = opTimeoutSeconds;
    }

    // ---------------------------------------------------------------- cache lifecycle

    void createCache(String cache, boolean counters) {
        if (counters) {
            sendCommand(c -> client.createCounterCache(c, cache), "createCounterCache " + cache);
        } else {
            sendCommand(c -> client.createCache(c, cache), "createCache " + cache);
        }
    }

    void deleteCache(String cache, boolean counters) {
        if (counters) {
            sendCommand(c -> client.deleteCounterCache(c, cache), "deleteCounterCache " + cache);
        } else {
            sendCommand(c -> client.deleteCache(c, cache), "deleteCache " + cache);
        }
    }

    void clearCache(String cache, boolean counters) {
        if (counters) {
            sendCommand(c -> client.clearCounterCache(c, cache), "clearCounterCache " + cache);
        } else {
            sendCommand(c -> client.clearCache(c, cache), "clearCache " + cache);
        }
    }

    // ---------------------------------------------------------------- item mutations

    void addEntry(String cache, String key, String value) {
        sendCommand(c -> client.addEntry(c, cache, key, value, TTL_NONE), "addEntry " + cache + "/" + key);
    }

    void addCounterEntry(String cache, String key, long init) {
        sendCommand(c -> client.addCounterEntry(c, cache, key, init, TTL_NONE), "addCounterEntry " + cache + "/" + key);
    }

    void incrementCounter(String cache, String key, long delta) {
        sendCommand(c -> client.incrementCounter(c, cache, key, delta, TTL_NONE), "incrementCounter " + cache + "/" + key);
    }

    void decrementCounter(String cache, String key, long delta) {
        sendCommand(c -> client.decrementCounter(c, cache, key, delta, TTL_NONE), "decrementCounter " + cache + "/" + key);
    }

    void setCounter(String cache, String key, long value) {
        sendCommand(c -> client.setCounter(c, cache, key, value, TTL_NONE), "setCounter " + cache + "/" + key);
    }

    /** Removes a key, tolerating {@link OperationStatus#UNKNOWN_KEY} (the key may have been removed already). */
    void removeEntry(String cache, String key) {
        var corr = nextCorr();
        sendWithRetry(() -> client.removeEntry(corr, cache, key));
        var r = awaitCommand(corr);
        if (r.status() != OperationStatus.SUCCESS && r.status() != OperationStatus.UNKNOWN_KEY) {
            throw failures.fail("removeEntry " + cache + "/" + key + " unexpected status " + r.status());
        }
    }

    // ---------------------------------------------------------------- primitives

    String nextCorr() {
        return "c" + (++corrSeq);
    }

    /** Sends a command (with backpressure retry), awaits its response and asserts SUCCESS. */
    private void sendCommand(CorrLongSupplier op, String what) {
        var corr = nextCorr();
        sendWithRetry(() -> op.send(corr));
        var r = awaitCommand(corr);
        if (r.status() != OperationStatus.SUCCESS) {
            throw failures.fail(what + " expected SUCCESS but got " + r.status());
        }
    }

    private SoakRecordingListener.CommandResponse awaitCommand(String corr) {
        var deadline = System.nanoTime() + opTimeoutNanos;
        SoakRecordingListener.CommandResponse response;
        while ((response = listener.commandResponses.remove(corr)) == null) {
            checkHealth();
            if (System.nanoTime() > deadline) {
                throw failures.fail("timed out after " + opTimeoutSeconds + "s awaiting response for " + corr);
            }
            LockSupport.parkNanos(PARK_NANOS);
        }
        return response;
    }

    void sendWithRetry(LongSupplier send) {
        var deadline = System.nanoTime() + opTimeoutNanos;
        while (send.getAsLong() == Aeron.NULL_VALUE) {
            if (System.nanoTime() > deadline) {
                throw failures.fail("timed out offering a request frame (sustained backpressure)");
            }
            LockSupport.parkNanos(PARK_NANOS);
        }
    }

    /** Asserts the mutation path is still viable; throws if the client disconnected or the gateway reported an error. */
    void checkHealth() {
        if (!client.isConnected()) {
            throw failures.fail("gateway client disconnected during soak run");
        }
        if (!listener.errors.isEmpty()) {
            var error = listener.errors.entrySet().iterator().next();
            throw failures.fail("gateway reported an error for " + error.getKey() + ": " + error.getValue());
        }
    }

    /** A command send that needs the per-call correlation id (so the driver can await the response). */
    @FunctionalInterface
    private interface CorrLongSupplier {
        long send(String correlationId);
    }
}
