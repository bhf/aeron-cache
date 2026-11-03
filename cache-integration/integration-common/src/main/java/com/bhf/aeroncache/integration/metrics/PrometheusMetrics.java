package com.bhf.aeroncache.integration.metrics;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal helper for integration tests to read a single metric value from a web gateway's Micrometer
 * {@code /prometheus} endpoint (served by the HTTP, WebSocket and SSE interface apps on their HTTP port).
 *
 * <p>It scrapes the endpoint over plain HTTP and sums the values of the samples of a named metric whose
 * labels contain the requested label subset, so a test can assert on, say,
 * {@code aeroncache_near_warm_caches{domain="cache"}} without pulling in a Prometheus client. The common
 * {@code application} tag the registry adds is ignored unless asked for, so matching on {@code domain}
 * alone selects the one sample for that domain on a given interface.
 */
public final class PrometheusMetrics {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // A single sample line: metric name, an optional {label="value",...} block, then the value.
    private static final Pattern SAMPLE = Pattern.compile("^(\\S+?)(\\{[^}]*})?\\s+(\\S+)\\s*$");
    // A single label="value" pair within the label block.
    private static final Pattern LABEL = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)=\"([^\"]*)\"");

    private PrometheusMetrics() {
    }

    /**
     * Scrape {@code <baseUri>:<port>/prometheus} and sum the values of all samples of {@code metricName}
     * whose labels contain every entry in {@code requiredLabels}.
     *
     * @param baseUri        the interface base URI including scheme and host, no port (e.g. {@code http://localhost}).
     * @param port           the interface HTTP port.
     * @param metricName     the Prometheus metric name (e.g. {@code aeroncache_near_warm_caches}).
     * @param requiredLabels the label subset every matched sample must carry (may be empty).
     * @return the summed value of the matching samples, or {@code 0.0} if none match.
     */
    public static double sampleValue(String baseUri, int port, String metricName, Map<String, String> requiredLabels) {
        final String body = scrape(baseUri, port);
        double sum = 0.0;
        boolean matched = false;
        for (final String line : body.split("\n")) {
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            final Matcher sample = SAMPLE.matcher(line);
            if (!sample.matches() || !metricName.equals(sample.group(1))) {
                continue;
            }
            if (labelsContain(sample.group(2), requiredLabels)) {
                sum += Double.parseDouble(sample.group(3));
                matched = true;
            }
        }
        return matched ? sum : 0.0;
    }

    private static boolean labelsContain(String labelBlock, Map<String, String> requiredLabels) {
        if (requiredLabels.isEmpty()) {
            return true;
        }
        if (labelBlock == null) {
            return false;
        }
        final Map<String, String> present = new java.util.HashMap<>();
        final Matcher label = LABEL.matcher(labelBlock);
        while (label.find()) {
            present.put(label.group(1), label.group(2));
        }
        return present.entrySet().containsAll(requiredLabels.entrySet());
    }

    private static String scrape(String baseUri, int port) {
        final URI uri = URI.create(baseUri + ":" + port + "/prometheus");
        try {
            final HttpResponse<String> response = HTTP.send(
                    HttpRequest.newBuilder(uri).GET().timeout(Duration.ofSeconds(10)).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Scrape of " + uri + " returned HTTP " + response.statusCode());
            }
            return response.body();
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to scrape " + uri, e);
        }
    }
}
