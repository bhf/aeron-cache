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
 *     {@code aeron_gateway_sessions_total{node}},
 *     {@code aeron_gateway_stream_updates_published_total{node}}, the subscription gauges
 *     {@code aeron_gateway_subscriptions_active{domain,type,node}} and
 *     {@code aeron_gateway_subscribers{domain,node}}, and the near cache gauges
 *     {@code aeron_gateway_near_warm_caches{domain,node}},
 *     {@code aeron_gateway_near_subscribers{domain,type,node}}, the PATCH subscriber gauge
 *     {@code aeron_gateway_near_patch_subscribers{domain,node}}, the hit counter
 *     {@code aeron_gateway_near_hydrations_served_total{domain,type,node}} and the synthetic patch counter
 *     {@code aeron_gateway_near_patches_total{domain,outcome,node}} for the gateway dashboard.</li>
 *     <li>The subscription metrics counters registered by the cache subscription services (type id
 *     {@value #SUBSCRIPTION_TYPE_ID}) are additionally parsed into
 *     {@code aeron_cache_subscriptions_total{subscription,event,node}} for subscribe/unsubscribe/update
 *     events and {@code aeron_cache_subscriptions_active{subscription,node}} for the active gauge.</li>
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

    /**
     * Type id of the counters registered by the cache subscription services
     * ({@code com.bhf.aeroncache.services.subscription.metrics.AeronCounterSubscriptionServiceMetrics}).
     */
    public static final int SUBSCRIPTION_TYPE_ID = 2200;

    // AeronCache-Requests-<Operation>-node<nodeId> (operation labels never contain a hyphen).
    private static final Pattern CACHE_REQUEST =
            Pattern.compile("^AeronCache-Requests-([^-]+)-node(.+)$");
    // AeronCache-Subscriptions-<type>-<Event>-node<nodeId> (type and event labels never contain a hyphen).
    private static final Pattern SUBSCRIPTION =
            Pattern.compile("^AeronCache-Subscriptions-([^-]+)-([^-]+)-node(.+)$");
    // The one subscription event that is a gauge rather than a monotonic counter.
    private static final String SUBSCRIPTION_ACTIVE_EVENT = "Active";
    // AeronCache-Gateway-Requests-<Type>-<nodeId> (request-type labels never contain a hyphen).
    private static final Pattern GATEWAY_REQUEST =
            Pattern.compile("^AeronCache-Gateway-Requests-([^-]+)-(.+)$");
    private static final Pattern GATEWAY_SESSIONS_CONNECTED =
            Pattern.compile("^AeronCache-Gateway-SessionsConnected-(.+)$");
    private static final Pattern GATEWAY_SESSIONS_TOTAL =
            Pattern.compile("^AeronCache-Gateway-SessionsTotal-(.+)$");
    private static final Pattern GATEWAY_STREAM_UPDATES =
            Pattern.compile("^AeronCache-Gateway-StreamUpdatesPublished-(.+)$");
    // AeronCache-Gateway-Subscriptions-<domain>-<type>-<nodeId> (domain and type never contain a hyphen).
    private static final Pattern GATEWAY_SUBSCRIPTIONS =
            Pattern.compile("^AeronCache-Gateway-Subscriptions-([^-]+)-([^-]+)-(.+)$");
    // AeronCache-Gateway-Subscribers-<domain>-<nodeId> (domain never contains a hyphen).
    private static final Pattern GATEWAY_SUBSCRIBERS =
            Pattern.compile("^AeronCache-Gateway-Subscribers-([^-]+)-(.+)$");
    // AeronCache-Gateway-NearWarmCaches-<domain>-<nodeId> (domain never contains a hyphen).
    private static final Pattern GATEWAY_NEAR_WARM_CACHES =
            Pattern.compile("^AeronCache-Gateway-NearWarmCaches-([^-]+)-(.+)$");
    // AeronCache-Gateway-NearSubscribers-<domain>-<type>-<nodeId> (domain and type never contain a hyphen).
    private static final Pattern GATEWAY_NEAR_SUBSCRIBERS =
            Pattern.compile("^AeronCache-Gateway-NearSubscribers-([^-]+)-([^-]+)-(.+)$");
    // AeronCache-Gateway-NearHydrationsServed-<domain>-<type>-<nodeId> (domain and type never contain a hyphen).
    private static final Pattern GATEWAY_NEAR_HYDRATIONS =
            Pattern.compile("^AeronCache-Gateway-NearHydrationsServed-([^-]+)-([^-]+)-(.+)$");
    // AeronCache-Gateway-NearPatchSubscribers-<domain>-<nodeId> (domain never contains a hyphen).
    private static final Pattern GATEWAY_NEAR_PATCH_SUBSCRIBERS =
            Pattern.compile("^AeronCache-Gateway-NearPatchSubscribers-([^-]+)-(.+)$");
    // AeronCache-Gateway-NearPatches-<domain>-<outcome>-<nodeId> (domain and outcome never contain a hyphen).
    private static final Pattern GATEWAY_NEAR_PATCHES =
            Pattern.compile("^AeronCache-Gateway-NearPatches-([^-]+)-([^-]+)-(.+)$");

    private AeronCountersPrometheusFormatter() {
    }

    /** Accumulates the parsed samples for each structured metric family while scanning the counters. */
    private static final class Families {
        final List<String> cacheRequests = new ArrayList<>();
        final List<String> subscriptionEvents = new ArrayList<>();
        final List<String> subscriptionsActive = new ArrayList<>();
        final List<String> gatewayRequests = new ArrayList<>();
        final List<String> gatewaySessionsConnected = new ArrayList<>();
        final List<String> gatewaySessionsTotal = new ArrayList<>();
        final List<String> gatewayStreamUpdates = new ArrayList<>();
        final List<String> gatewaySubscriptionsActive = new ArrayList<>();
        final List<String> gatewaySubscribers = new ArrayList<>();
        final List<String> gatewayNearWarmCaches = new ArrayList<>();
        final List<String> gatewayNearSubscribers = new ArrayList<>();
        final List<String> gatewayNearHydrations = new ArrayList<>();
        final List<String> gatewayNearPatchSubscribers = new ArrayList<>();
        final List<String> gatewayNearPatches = new ArrayList<>();
    }

    /**
     * Render the counters as Prometheus text exposition format.
     *
     * @param response the scraped counters.
     * @return the metrics as a single Prometheus exposition string, ending with a newline.
     */
    public static String format(final AeronCountersResponse response) {
        final Families families = new Families();

        final StringBuilder raw = new StringBuilder();
        raw.append("# HELP aeron_counter Raw Aeron media driver counter value scraped from the CnC file.\n");
        raw.append("# TYPE aeron_counter gauge\n");

        for (final AeronCounter counter : response.counters()) {
            raw.append("aeron_counter{counter_id=\"").append(counter.counterId())
                    .append("\",type_id=\"").append(counter.typeId())
                    .append("\",label=\"").append(escape(counter.label()))
                    .append("\"} ").append(counter.value()).append('\n');

            parseKnown(counter, families);
        }

        final StringBuilder out = new StringBuilder(raw);
        appendFamily(out, "aeron_cache_requests_total",
                "Cache requests counted by the Aeron counter tracing service.", "counter", families.cacheRequests);
        appendFamily(out, "aeron_cache_subscriptions_total",
                "Cache subscription events counted by the subscription services, by event.", "counter",
                families.subscriptionEvents);
        appendFamily(out, "aeron_cache_subscriptions_active",
                "Active cache subscriptions (one session registered against one target).", "gauge",
                families.subscriptionsActive);
        appendFamily(out, "aeron_gateway_requests_total",
                "Requests serviced by the Aeron transport gateway, by type.", "counter", families.gatewayRequests);
        appendFamily(out, "aeron_gateway_sessions_connected",
                "Clients currently connected to the Aeron transport gateway.", "gauge", families.gatewaySessionsConnected);
        appendFamily(out, "aeron_gateway_sessions_total",
                "Client connections to the Aeron transport gateway since start.", "counter", families.gatewaySessionsTotal);
        appendFamily(out, "aeron_gateway_stream_updates_published_total",
                "Streaming subscription updates published to gateway clients.", "counter", families.gatewayStreamUpdates);
        appendFamily(out, "aeron_gateway_subscriptions_active",
                "Active gateway subscriptions (one client session on one cache), by domain and whole/keyed type.",
                "gauge", families.gatewaySubscriptionsActive);
        appendFamily(out, "aeron_gateway_subscribers",
                "Distinct gateway client sessions holding at least one subscription, by domain.", "gauge",
                families.gatewaySubscribers);
        appendFamily(out, "aeron_gateway_near_warm_caches",
                "Caches currently kept warm in the gateway near cache, by domain.", "gauge",
                families.gatewayNearWarmCaches);
        appendFamily(out, "aeron_gateway_near_subscribers",
                "Distinct gateway client sessions served by the near cache, by domain and whole/keyed type.",
                "gauge", families.gatewayNearSubscribers);
        appendFamily(out, "aeron_gateway_near_hydrations_served_total",
                "Hydrating subscriptions served from the gateway near cache, by domain and whole/keyed type.",
                "counter", families.gatewayNearHydrations);
        appendFamily(out, "aeron_gateway_near_patch_subscribers",
                "Distinct gateway client sessions holding a PATCH-mode near cache subscription, by domain.",
                "gauge", families.gatewayNearPatchSubscribers);
        appendFamily(out, "aeron_gateway_near_patches_total",
                "Synthetic patches generated for gateway near cache PATCH subscribers, by domain and outcome "
                        + "(delta/baseline/suppressed).", "counter", families.gatewayNearPatches);

        return out.toString();
    }

    private static void parseKnown(final AeronCounter counter, final Families families) {
        final long value = counter.value();
        if (counter.typeId() == CACHE_REQUEST_TYPE_ID) {
            final Matcher m = CACHE_REQUEST.matcher(counter.label());
            if (m.matches()) {
                families.cacheRequests.add("{operation=\"" + escape(m.group(1)) + "\",node=\""
                        + escape(m.group(2)) + "\"} " + value);
            }
        } else if (counter.typeId() == SUBSCRIPTION_TYPE_ID) {
            final Matcher m = SUBSCRIPTION.matcher(counter.label());
            if (m.matches()) {
                final String subscription = escape(m.group(1));
                final String event = m.group(2);
                final String node = escape(m.group(3));
                if (SUBSCRIPTION_ACTIVE_EVENT.equals(event)) {
                    families.subscriptionsActive.add("{subscription=\"" + subscription + "\",node=\"" + node + "\"} " + value);
                } else {
                    families.subscriptionEvents.add("{subscription=\"" + subscription + "\",event=\"" + escape(event)
                            + "\",node=\"" + node + "\"} " + value);
                }
            }
        } else if (counter.typeId() == GATEWAY_TYPE_ID) {
            parseGatewayCounter(counter, value, families);
        }
    }

    private static void parseGatewayCounter(final AeronCounter counter, final long value, final Families families) {
        Matcher m = GATEWAY_REQUEST.matcher(counter.label());
        if (m.matches()) {
            families.gatewayRequests.add("{type=\"" + escape(m.group(1)) + "\",node=\""
                    + escape(m.group(2)) + "\"} " + value);
            return;
        }
        m = GATEWAY_SUBSCRIPTIONS.matcher(counter.label());
        if (m.matches()) {
            families.gatewaySubscriptionsActive.add("{domain=\"" + escape(m.group(1)) + "\",type=\""
                    + escape(m.group(2)) + "\",node=\"" + escape(m.group(3)) + "\"} " + value);
            return;
        }
        m = GATEWAY_SUBSCRIBERS.matcher(counter.label());
        if (m.matches()) {
            families.gatewaySubscribers.add("{domain=\"" + escape(m.group(1)) + "\",node=\""
                    + escape(m.group(2)) + "\"} " + value);
            return;
        }
        m = GATEWAY_NEAR_WARM_CACHES.matcher(counter.label());
        if (m.matches()) {
            families.gatewayNearWarmCaches.add("{domain=\"" + escape(m.group(1)) + "\",node=\""
                    + escape(m.group(2)) + "\"} " + value);
            return;
        }
        m = GATEWAY_NEAR_SUBSCRIBERS.matcher(counter.label());
        if (m.matches()) {
            families.gatewayNearSubscribers.add("{domain=\"" + escape(m.group(1)) + "\",type=\""
                    + escape(m.group(2)) + "\",node=\"" + escape(m.group(3)) + "\"} " + value);
            return;
        }
        m = GATEWAY_NEAR_HYDRATIONS.matcher(counter.label());
        if (m.matches()) {
            families.gatewayNearHydrations.add("{domain=\"" + escape(m.group(1)) + "\",type=\""
                    + escape(m.group(2)) + "\",node=\"" + escape(m.group(3)) + "\"} " + value);
            return;
        }
        m = GATEWAY_NEAR_PATCH_SUBSCRIBERS.matcher(counter.label());
        if (m.matches()) {
            families.gatewayNearPatchSubscribers.add("{domain=\"" + escape(m.group(1)) + "\",node=\""
                    + escape(m.group(2)) + "\"} " + value);
            return;
        }
        m = GATEWAY_NEAR_PATCHES.matcher(counter.label());
        if (m.matches()) {
            families.gatewayNearPatches.add("{domain=\"" + escape(m.group(1)) + "\",outcome=\""
                    + escape(m.group(2)) + "\",node=\"" + escape(m.group(3)) + "\"} " + value);
            return;
        }
        m = GATEWAY_SESSIONS_CONNECTED.matcher(counter.label());
        if (m.matches()) {
            families.gatewaySessionsConnected.add("{node=\"" + escape(m.group(1)) + "\"} " + value);
            return;
        }
        m = GATEWAY_SESSIONS_TOTAL.matcher(counter.label());
        if (m.matches()) {
            families.gatewaySessionsTotal.add("{node=\"" + escape(m.group(1)) + "\"} " + value);
            return;
        }
        m = GATEWAY_STREAM_UPDATES.matcher(counter.label());
        if (m.matches()) {
            families.gatewayStreamUpdates.add("{node=\"" + escape(m.group(1)) + "\"} " + value);
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
