package com.bhf.aeroncache.clustertools.counters;

import com.bhf.aeroncache.http.responses.AeronCounter;
import com.bhf.aeroncache.http.responses.AeronCountersResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders a scraped {@link AeronCountersResponse} as Prometheus text exposition format
 * (version 0.0.4) so the Aeron media driver counters can be scraped alongside the JVM metrics.
 *
 * <p>Three metric families are produced:
 * <ul>
 *     <li>{@code aeron_counter{counter_id,type_id,label}} - every allocated driver counter, verbatim.
 *     This is the complete, lossless export and powers the general Aeron transport / media driver
 *     dashboards (the driver's own labels, e.g. "Bytes sent", are stable enough to select on).</li>
 *     <li>The cache request counters registered by the cache tracing service (type id
 *     {@value #CACHE_REQUEST_TYPE_ID}) are additionally parsed into
 *     {@code aeron_cache_requests_total{operation,node}} for a clean per-operation cache dashboard.</li>
 *     <li>The gateway counters (type id {@value #GATEWAY_TYPE_ID}) are additionally parsed into
 *     {@code aeron_gateway_requests_total{type,node}}, {@code aeron_gateway_sessions_connected{node}},
 *     {@code aeron_gateway_sessions_total{node}} and
 *     {@code aeron_gateway_stream_updates_published_total{node}} for the gateway dashboard.</li>
 * </ul>
 *
 * <p>The parsed families are a convenience layer on top of the raw {@code aeron_counter} export: a
 * counter whose label does not match the expected structured pattern is simply left out of the parsed
 * families but is always present in {@code aeron_counter}, so nothing is ever dropped.
 */
public final class AeronCountersPrometheusFormatter {

    /**
     * Type id of the counters registered by the cache tracing service
     * ({@code com.bhf.aeroncache.services.tracing.impl.AeronCounterTracingService}).
     */
    public static final int CACHE_REQUEST_TYPE_ID = 2000;

    /**
     * Type id of the counters registered by the gateway metrics
     * ({@code com.bhf.aeroncache.gateway.metrics.AeronCounterGatewayMetrics}).
     */
    public static final int GATEWAY_TYPE_ID = 2100;

    // AeronCache-Requests-<Operation>-node<nodeId> (operation labels never contain a hyphen).
    private static final Pattern CACHE_REQUEST =
            Pattern.compile("^AeronCache-Requests-([^-]+)-node(.+)$");
    // AeronCache-Gateway-Requests-<Type>-<nodeId> (request-type labels never contain a hyphen).
    private static final Pattern GATEWAY_REQUEST =
            Pattern.compile("^AeronCache-Gateway-Requests-([^-]+)-(.+)$");
    private static final Pattern GATEWAY_SESSIONS_CONNECTED =
            Pattern.compile("^AeronCache-Gateway-SessionsConnected-(.+)$");
    private static final Pattern GATEWAY_SESSIONS_TOTAL =
            Pattern.compile("^AeronCache-Gateway-SessionsTotal-(.+)$");
    private static final Pattern GATEWAY_STREAM_UPDATES =
            Pattern.compile("^AeronCache-Gateway-StreamUpdatesPublished-(.+)$");

    private AeronCountersPrometheusFormatter() {
    }

    /**
     * Render the counters as Prometheus text exposition format.
     *
     * @param response the scraped counters.
     * @return the metrics as a single Prometheus exposition string, ending with a newline.
     */
    public static String format(final AeronCountersResponse response) {
        final List<String> cacheRequests = new ArrayList<>();
        final List<String> gatewayRequests = new ArrayList<>();
        final List<String> gatewaySessionsConnected = new ArrayList<>();
        final List<String> gatewaySessionsTotal = new ArrayList<>();
        final List<String> gatewayStreamUpdates = new ArrayList<>();

        final StringBuilder raw = new StringBuilder();
        raw.append("# HELP aeron_counter Raw Aeron media driver counter value scraped from the CnC file.\n");
        raw.append("# TYPE aeron_counter gauge\n");

        for (final AeronCounter counter : response.counters()) {
            raw.append("aeron_counter{counter_id=\"").append(counter.counterId())
                    .append("\",type_id=\"").append(counter.typeId())
                    .append("\",label=\"").append(escape(counter.label()))
                    .append("\"} ").append(counter.value()).append('\n');

            parseKnown(counter, cacheRequests, gatewayRequests, gatewaySessionsConnected,
                    gatewaySessionsTotal, gatewayStreamUpdates);
        }

        final StringBuilder out = new StringBuilder(raw);
        appendFamily(out, "aeron_cache_requests_total",
                "Cache requests counted by the Aeron counter tracing service.", "counter", cacheRequests);
        appendFamily(out, "aeron_gateway_requests_total",
                "Requests serviced by the Aeron transport gateway, by type.", "counter", gatewayRequests);
        appendFamily(out, "aeron_gateway_sessions_connected",
                "Clients currently connected to the Aeron transport gateway.", "gauge", gatewaySessionsConnected);
        appendFamily(out, "aeron_gateway_sessions_total",
                "Client connections to the Aeron transport gateway since start.", "counter", gatewaySessionsTotal);
        appendFamily(out, "aeron_gateway_stream_updates_published_total",
                "Streaming subscription updates published to gateway clients.", "counter", gatewayStreamUpdates);

        return out.toString();
    }

    private static void parseKnown(
            final AeronCounter counter,
            final List<String> cacheRequests,
            final List<String> gatewayRequests,
            final List<String> gatewaySessionsConnected,
            final List<String> gatewaySessionsTotal,
            final List<String> gatewayStreamUpdates) {

        final long value = counter.value();
        if (counter.typeId() == CACHE_REQUEST_TYPE_ID) {
            final Matcher m = CACHE_REQUEST.matcher(counter.label());
            if (m.matches()) {
                cacheRequests.add("{operation=\"" + escape(m.group(1)) + "\",node=\""
                        + escape(m.group(2)) + "\"} " + value);
            }
        } else if (counter.typeId() == GATEWAY_TYPE_ID) {
            Matcher m = GATEWAY_REQUEST.matcher(counter.label());
            if (m.matches()) {
                gatewayRequests.add("{type=\"" + escape(m.group(1)) + "\",node=\""
                        + escape(m.group(2)) + "\"} " + value);
                return;
            }
            m = GATEWAY_SESSIONS_CONNECTED.matcher(counter.label());
            if (m.matches()) {
                gatewaySessionsConnected.add("{node=\"" + escape(m.group(1)) + "\"} " + value);
                return;
            }
            m = GATEWAY_SESSIONS_TOTAL.matcher(counter.label());
            if (m.matches()) {
                gatewaySessionsTotal.add("{node=\"" + escape(m.group(1)) + "\"} " + value);
                return;
            }
            m = GATEWAY_STREAM_UPDATES.matcher(counter.label());
            if (m.matches()) {
                gatewayStreamUpdates.add("{node=\"" + escape(m.group(1)) + "\"} " + value);
            }
        }
    }

    private static void appendFamily(
            final StringBuilder out,
            final String metric,
            final String help,
            final String type,
            final List<String> samples) {
        if (samples.isEmpty()) {
            return;
        }
        out.append("# HELP ").append(metric).append(' ').append(help).append('\n');
        out.append("# TYPE ").append(metric).append(' ').append(type).append('\n');
        for (final String sample : samples) {
            out.append(metric).append(sample).append('\n');
        }
    }

    /**
     * Escape a string for use as a Prometheus label value (backslash, double quote and newline).
     *
     * @param value the raw label value.
     * @return the escaped value.
     */
    private static String escape(final String value) {
        if (value == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
