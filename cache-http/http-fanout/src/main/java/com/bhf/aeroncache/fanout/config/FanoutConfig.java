package com.bhf.aeroncache.fanout.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Runtime configuration for the HTTP fan-out aggregator, read from the environment.
 *
 * <p>The aggregator fronts several {@code http-server-javalin} gateways (each in front of its own,
 * disjoint cache cluster) and scatters admin/observability requests to all of them, gathering the
 * responses back into a single result. The set of downstream gateways is a static list supplied in
 * {@code GATEWAY_ADDRESSES}, mirroring how the HTTP gateway itself reads {@code CLUSTER_ADDRESSES}.
 *
 * @param gateways        the downstream gateway base URLs, e.g. {@code http://aeroncache-http-a:7070}.
 * @param port            the port the aggregator's own HTTP interface listens on.
 * @param corsOrigins     the allowed CORS origins for the aggregator's web interface.
 * @param requestTimeout  the per-gateway timeout for a single fan-out call.
 */
public record FanoutConfig(List<String> gateways, int port, List<String> corsOrigins, Duration requestTimeout) {

    /** Default port for the aggregator's HTTP interface (distinct from the HTTP gateway's 7070). */
    public static final int DEFAULT_PORT = 7071;

    /** Default per-gateway request timeout. */
    public static final long DEFAULT_REQUEST_TIMEOUT_MS = 5_000L;

    /** Default CORS origins (local dev), used when {@code CORS_ALLOWED_ORIGINS} is unset. */
    private static final String DEFAULT_CORS = "http://localhost:3000,http://localhost:3001,"
            + "http://localhost:3002,http://localhost:3003,http://localhost:3004,http://localhost:3005,"
            + "http://localhost";

    /**
     * Build the configuration from the process environment.
     *
     * @return the parsed configuration.
     */
    public static FanoutConfig fromEnv() {
        return fromEnv(System::getenv);
    }

    /**
     * Build the configuration from an arbitrary environment lookup, for testing.
     *
     * @param env the environment lookup.
     * @return the parsed configuration.
     */
    public static FanoutConfig fromEnv(Function<String, String> env) {
        var gateways = splitCsv(env.apply("GATEWAY_ADDRESSES")).stream()
                .map(FanoutConfig::normaliseBaseUrl)
                .toList();

        var portStr = env.apply("HTTP_PORT");
        int port = portStr == null || portStr.isBlank() ? DEFAULT_PORT : Integer.parseInt(portStr.trim());

        var corsStr = env.apply("CORS_ALLOWED_ORIGINS");
        var corsOrigins = splitCsv(corsStr == null || corsStr.isBlank() ? DEFAULT_CORS : corsStr);

        var timeoutStr = env.apply("REQUEST_TIMEOUT_MS");
        long timeoutMs = timeoutStr == null || timeoutStr.isBlank()
                ? DEFAULT_REQUEST_TIMEOUT_MS
                : Long.parseLong(timeoutStr.trim());

        return new FanoutConfig(gateways, port, corsOrigins, Duration.ofMillis(timeoutMs));
    }

    private static List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Normalise a gateway base URL: default the scheme to http and strip any trailing slash so it can
     * be concatenated with an absolute API path.
     *
     * @param raw the configured gateway address.
     * @return the normalised base URL.
     */
    static String normaliseBaseUrl(String raw) {
        var url = raw;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
