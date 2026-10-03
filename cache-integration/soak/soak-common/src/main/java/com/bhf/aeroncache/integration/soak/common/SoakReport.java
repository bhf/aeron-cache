package com.bhf.aeroncache.integration.soak.common;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Accumulates metrics over a soak run and writes a machine-readable summary (uploaded as a workflow
 * artifact). Suite-agnostic: the config is supplied as an ordered map, operation/event tallies go through
 * {@link #recordCount}, and a suite can attach extra named count sections (e.g. the streaming coverage
 * matrix) via {@link #putSection}. Everything needed to reproduce and triage a run lives here - the seed,
 * the config, the tallies, throughput, peak heap and whether a correctness mismatch was seen.
 */
public final class SoakReport {

    private final long seed;
    private final long durationSeconds;
    private final Map<String, Object> config;
    private final Map<String, Long> counts = new TreeMap<>();
    private final Map<String, Map<String, Long>> sections = new LinkedHashMap<>();
    private final Instant startedAt = Instant.now();

    private long total;
    private long verifications;
    private long peakHeapUsedBytes;
    private String mismatch;

    public SoakReport(long seed, long durationSeconds, Map<String, Object> config) {
        this.seed = seed;
        this.durationSeconds = durationSeconds;
        this.config = new LinkedHashMap<>(config);
    }

    public void recordCount(String key) {
        counts.merge(key, 1L, Long::sum);
        total++;
    }

    public void recordVerification() {
        verifications++;
    }

    public void sampleHeap() {
        var rt = Runtime.getRuntime();
        var used = rt.totalMemory() - rt.freeMemory();
        if (used > peakHeapUsedBytes) {
            peakHeapUsedBytes = used;
        }
    }

    public void recordMismatch(String detail) {
        if (mismatch == null) {
            mismatch = detail;
        }
    }

    /** Attaches (or replaces) a named count section, e.g. the streaming coverage-matrix cell counts. */
    public void putSection(String name, Map<String, Long> section) {
        sections.put(name, new TreeMap<>(section));
    }

    public long total() {
        return total;
    }

    public String progressLine() {
        var elapsed = Duration.between(startedAt, Instant.now());
        var seconds = Math.max(1, elapsed.toSeconds());
        return String.format(
                "soak progress: total=%d (%.0f/s), verifications=%d, elapsed=%ds, heapUsed=%dMB, peakHeap=%dMB",
                total, (double) total / seconds, verifications, elapsed.toSeconds(),
                (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024),
                peakHeapUsedBytes / (1024 * 1024));
    }

    public void writeTo(Path path) {
        var elapsed = Duration.between(startedAt, Instant.now());
        var seconds = Math.max(1, elapsed.toSeconds());

        var json = new StringBuilder();
        json.append("{\n");
        json.append("  \"seed\": ").append(seed).append(",\n");
        json.append("  \"durationSeconds\": ").append(durationSeconds).append(",\n");
        json.append("  \"elapsedSeconds\": ").append(elapsed.toSeconds()).append(",\n");
        config.forEach((k, v) -> json.append("  ").append(quote(k)).append(": ").append(scalar(v)).append(",\n"));
        json.append("  \"total\": ").append(total).append(",\n");
        json.append("  \"perSecond\": ").append(total / seconds).append(",\n");
        json.append("  \"verifications\": ").append(verifications).append(",\n");
        json.append("  \"peakHeapUsedMB\": ").append(peakHeapUsedBytes / (1024 * 1024)).append(",\n");
        json.append("  \"passed\": ").append(mismatch == null).append(",\n");
        json.append("  \"mismatch\": ").append(mismatch == null ? "null" : quote(mismatch)).append(",\n");
        appendCountMap(json, "counts", counts, !sections.isEmpty());
        var sectionNames = sections.keySet().iterator();
        while (sectionNames.hasNext()) {
            var name = sectionNames.next();
            appendCountMap(json, name, sections.get(name), sectionNames.hasNext());
        }
        json.append("}\n");

        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, json.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write soak report to " + path, e);
        }
    }

    private static void appendCountMap(StringBuilder json, String name, Map<String, Long> map, boolean trailingComma) {
        json.append("  ").append(quote(name)).append(": {\n");
        int i = 0;
        for (var e : map.entrySet()) {
            json.append("    ").append(quote(e.getKey())).append(": ").append(e.getValue());
            json.append(++i < map.size() ? ",\n" : "\n");
        }
        json.append("  }").append(trailingComma ? ",\n" : "\n");
    }

    private static String scalar(Object v) {
        return (v instanceof Number || v instanceof Boolean) ? String.valueOf(v) : quote(String.valueOf(v));
    }

    private static String quote(String s) {
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + '"';
    }
}
