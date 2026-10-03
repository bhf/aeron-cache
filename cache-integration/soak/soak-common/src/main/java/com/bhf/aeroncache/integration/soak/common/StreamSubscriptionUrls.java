package com.bhf.aeroncache.integration.soak.common;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Builds the subscription URL path (everything after {@code host:port}) for the URL-based streaming gateways
 * from a {@link SubscriptionSpec}. The WebSocket and SSE routes are identical apart from the {@code api} infix
 * ({@code ws} vs {@code sse}), so both transports share this builder and only differ in scheme/host/port.
 *
 * <p>Mirrors the routes the integration-test streaming helpers use:
 * {@code /api/<infix>/v1/<flavour>[/hydrate]/<cacheIds>} with {@code <flavour>} one of
 * {@code cache|caches|counter|counters} (singular for single-cardinality, plural for multi), an optional
 * {@code ?keys=<cacheId:key,...>} filter and an optional {@code &mode=patch} (cache flavour only; counters
 * coerce patch to full server-side).
 */
public final class StreamSubscriptionUrls {

    private StreamSubscriptionUrls() {
    }

    /** @param apiInfix the route infix: {@code "ws"} or {@code "sse"}. */
    public static String path(String apiInfix, SubscriptionSpec spec) {
        var flavour = spec.counters()
                ? (spec.multi() ? "counters" : "counter")
                : (spec.multi() ? "caches" : "cache");
        var base = "/api/" + apiInfix + "/v1/" + flavour + "/";
        if (spec.hydrate()) {
            base += "hydrate/";
        }
        var path = base + String.join(",", spec.cacheIds());

        var query = new ArrayList<String>();
        if (spec.keyScoped()) {
            // cacheId:key tokens target the key on each specific cache (works for single and multi routes).
            query.add("keys=" + spec.cacheIds().stream()
                    .map(c -> c + ":" + spec.key())
                    .collect(Collectors.joining(",")));
        }
        if (spec.patch() && !spec.counters()) {
            query.add("mode=patch");
        }
        if (!query.isEmpty()) {
            path += "?" + String.join("&", query);
        }
        return path;
    }
}
