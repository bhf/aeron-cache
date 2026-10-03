package com.bhf.aeroncache.integration.soak;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

/**
 * Accumulates metrics over a soak run and writes a machine-readable summary to
 * {@code build/reports/soak/summary.json} (uploaded as a workflow artifact). Everything needed to
 * reproduce and triage a run lives here: the seed and config, per-operation counts, throughput, peak
 * heap, and whether a correctness mismatch was seen.
 */
final class SoakReport {

    private final SoakConfig config;
    private final EnumMap<SoakWorkload.OpType, Long> opCounts = new EnumMap<>(SoakWorkload.OpType.class);
    private final Instant startedAt = Instant.now();

    private long totalOps;
    private long verifications;
    private long peakHeapUsedBytes;
    private String mismatch;

    SoakReport(SoakConfig config) {
        this.config = config;
        for (var op : SoakWorkload.OpType.values()) {
            opCounts.put(op, 0L);
        }
    }

    void recordOp(SoakWorkload.OpType op) {
        opCounts.merge(op, 1L, Long::sum);
        totalOps++;
    }

    void recordVerification() {
        verifications++;
    }

    void sampleHeap() {
        var rt = Runtime.getRuntime();
        var used = rt.totalMemory() - rt.freeMemory();
        if (used > peakHeapUsedBytes) {
            peakHeapUsedBytes = used;
        }
    }

    void recordMismatch(String detail) {
        if (mismatch == null) {
            mismatch = detail;
        }
    }

    long totalOps() {
        return totalOps;
    }

    String progressLine() {
        var elapsed = Duration.between(startedAt, Instant.now());
        var seconds = Math.max(1, elapsed.toSeconds());
        return String.format(
                "soak progress: ops=%d (%.0f ops/s), verifications=%d, elapsed=%ds, heapUsed=%dMB, peakHeap=%dMB",
                totalOps, (double) totalOps / seconds, verifications, elapsed.toSeconds(),
                (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024),
                peakHeapUsedBytes / (1024 * 1024));
    }

    void writeTo(Path path) {
        var elapsed = Duration.between(startedAt, Instant.now());
        var seconds = Math.max(1, elapsed.toSeconds());

        var json = new StringBuilder();
        json.append("{\n");
        json.append("  \"seed\": ").append(config.seed).append(",\n");
        json.append("  \"durationSeconds\": ").append(config.durationSeconds).append(",\n");
        json.append("  \"elapsedSeconds\": ").append(elapsed.toSeconds()).append(",\n");
        json.append("  \"kvCacheCount\": ").append(config.kvCacheCount).append(",\n");
        json.append("  \"counterCacheCount\": ").append(config.counterCacheCount).append(",\n");
        json.append("  \"keySpace\": ").append(config.keySpace).append(",\n");
        json.append("  \"valueSizeBytes\": ").append(config.valueSizeBytes).append(",\n");
        json.append("  \"totalOps\": ").append(totalOps).append(",\n");
        json.append("  \"opsPerSecond\": ").append(totalOps / seconds).append(",\n");
        json.append("  \"verifications\": ").append(verifications).append(",\n");
        json.append("  \"peakHeapUsedMB\": ").append(peakHeapUsedBytes / (1024 * 1024)).append(",\n");
        json.append("  \"passed\": ").append(mismatch == null).append(",\n");
        json.append("  \"mismatch\": ").append(mismatch == null ? "null" : quote(mismatch)).append(",\n");
        json.append("  \"opCounts\": {\n");
        int i = 0;
        for (Map.Entry<SoakWorkload.OpType, Long> e : opCounts.entrySet()) {
            json.append("    \"").append(e.getKey().name()).append("\": ").append(e.getValue());
            json.append(++i < opCounts.size() ? ",\n" : "\n");
        }
        json.append("  }\n");
        json.append("}\n");

        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, json.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write soak report to " + path, e);
        }
    }

    private static String quote(String s) {
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + '"';
    }
}
