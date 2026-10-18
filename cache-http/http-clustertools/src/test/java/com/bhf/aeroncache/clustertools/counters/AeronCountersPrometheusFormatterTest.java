package com.bhf.aeroncache.clustertools.counters;

import com.bhf.aeroncache.http.responses.AeronCounter;
import com.bhf.aeroncache.http.responses.AeronCountersResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the Prometheus text exposition produced from a scraped {@link AeronCountersResponse}: every
 * counter is exported verbatim as {@code aeron_counter}, and the cache (type 2000) and gateway
 * (type 2100) application counters are additionally parsed into clean, labelled metric families.
 */
class AeronCountersPrometheusFormatterTest {

    private static AeronCountersResponse response(AeronCounter... counters) {
        return new AeronCountersResponse("/dev/shm/aeron", 0, 0L, counters.length, List.of(counters));
    }

    @Test
    @DisplayName("Exports every counter verbatim as aeron_counter with HELP and TYPE")
    void exportsRawCounters() {
        String out = AeronCountersPrometheusFormatter.format(response(
                new AeronCounter(0, 0, 1234L, "Bytes sent"),
                new AeronCounter(1, 0, 56L, "NAKs received")));

        assertTrue(out.contains("# HELP aeron_counter"));
        assertTrue(out.contains("# TYPE aeron_counter gauge"));
        assertTrue(out.contains("aeron_counter{counter_id=\"0\",type_id=\"0\",label=\"Bytes sent\"} 1234"));
        assertTrue(out.contains("aeron_counter{counter_id=\"1\",type_id=\"0\",label=\"NAKs received\"} 56"));
    }

    @Test
    @DisplayName("Parses cache tracing counters (type 2000) into aeron_cache_requests_total")
    void parsesCacheRequestCounters() {
        String out = AeronCountersPrometheusFormatter.format(response(
                new AeronCounter(10, 2000, 42L, "AeronCache-Requests-AddCacheEntry-node0"),
                new AeronCounter(11, 2000, 7L, "AeronCache-Requests-GetCacheEntry-node2")));

        assertTrue(out.contains("# TYPE aeron_cache_requests_total counter"));
        assertTrue(out.contains("aeron_cache_requests_total{operation=\"AddCacheEntry\",node=\"0\"} 42"));
        assertTrue(out.contains("aeron_cache_requests_total{operation=\"GetCacheEntry\",node=\"2\"} 7"));
    }

    @Test
    @DisplayName("Parses gateway counters (type 2100) into the gateway metric families")
    void parsesGatewayCounters() {
        String out = AeronCountersPrometheusFormatter.format(response(
                new AeronCounter(20, 2100, 3L, "AeronCache-Gateway-SessionsConnected-aeroncache-gateway-0"),
                new AeronCounter(21, 2100, 9L, "AeronCache-Gateway-SessionsTotal-aeroncache-gateway-0"),
                new AeronCounter(22, 2100, 100L, "AeronCache-Gateway-StreamUpdatesPublished-aeroncache-gateway-0"),
                new AeronCounter(23, 2100, 55L, "AeronCache-Gateway-Requests-GetCacheEntry-aeroncache-gateway-0")));

        assertTrue(out.contains("aeron_gateway_sessions_connected{node=\"aeroncache-gateway-0\"} 3"));
        assertTrue(out.contains("aeron_gateway_sessions_total{node=\"aeroncache-gateway-0\"} 9"));
        assertTrue(out.contains(
                "aeron_gateway_stream_updates_published_total{node=\"aeroncache-gateway-0\"} 100"));
        assertTrue(out.contains(
                "aeron_gateway_requests_total{type=\"GetCacheEntry\",node=\"aeroncache-gateway-0\"} 55"));
    }

    @Test
    @DisplayName("A counter with an unexpected label is still exported as aeron_counter")
    void keepsUnparseableCounters() {
        String out = AeronCountersPrometheusFormatter.format(response(
                new AeronCounter(30, 2000, 1L, "some-free-form-label")));

        assertTrue(out.contains("aeron_counter{counter_id=\"30\",type_id=\"2000\",label=\"some-free-form-label\"} 1"));
        assertFalse(out.contains("aeron_cache_requests_total"),
                "an unparseable label must not emit a parsed family");
    }

    @Test
    @DisplayName("Escapes backslashes and double quotes in labels")
    void escapesLabelValues() {
        String out = AeronCountersPrometheusFormatter.format(response(
                new AeronCounter(40, 0, 1L, "pub \"aeron:udp?endpoint=host\\x\"")));

        assertTrue(out.contains("label=\"pub \\\"aeron:udp?endpoint=host\\\\x\\\"\""));
    }

    @Test
    @DisplayName("Omits parsed families entirely when there are no matching counters")
    void omitsEmptyFamilies() {
        String out = AeronCountersPrometheusFormatter.format(response(
                new AeronCounter(0, 0, 1L, "Bytes sent")));

        assertFalse(out.contains("aeron_cache_requests_total"));
        assertFalse(out.contains("aeron_gateway_requests_total"));
        assertFalse(out.contains("aeron_gateway_sessions_connected"));
    }
}
