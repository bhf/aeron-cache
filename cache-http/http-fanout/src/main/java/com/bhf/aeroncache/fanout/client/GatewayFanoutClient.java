package com.bhf.aeroncache.fanout.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Issues one HTTP request per downstream gateway in parallel and returns a per-gateway
 * {@link GatewayResult}. This centralises the request building, timeout and error handling that the
 * HTTP gateway currently inlines for its clustertools fan-out, so the aggregators can focus purely on
 * merging the parsed responses.
 */
@Log4j2
public class GatewayFanoutClient {

    private final List<String> baseUrls;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    /**
     * @param baseUrls the downstream gateway base URLs (already normalised, no trailing slash).
     * @param timeout  the per-gateway request timeout.
     * @param mapper   the JSON mapper used to deserialise responses.
     */
    public GatewayFanoutClient(List<String> baseUrls, Duration timeout, ObjectMapper mapper) {
        this.baseUrls = List.copyOf(baseUrls);
        this.timeout = timeout;
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    /**
     * The configured downstream gateways, in order.
     *
     * @return the gateway base URLs.
     */
    public List<String> gateways() {
        return baseUrls;
    }

    /**
     * GET {@code path} from every gateway in parallel.
     *
     * @param path    the absolute API path, e.g. {@code /api/v1/caches}.
     * @param typeRef the response type.
     * @param <T>     the response type.
     * @return one result per gateway, in configured order.
     */
    public <T> List<GatewayResult<T>> getAll(String path, TypeReference<T> typeRef) {
        return fanOut("GET", path, null, typeRef);
    }

    /**
     * POST {@code body} to {@code path} on every gateway in parallel.
     *
     * @param path    the absolute API path, e.g. {@code /api/v1/snapshot}.
     * @param body    the request body (may be empty; the gateways build their own clustertools body).
     * @param typeRef the response type.
     * @param <T>     the response type.
     * @return one result per gateway, in configured order.
     */
    public <T> List<GatewayResult<T>> postAll(String path, String body, TypeReference<T> typeRef) {
        return fanOut("POST", path, body == null ? "" : body, typeRef);
    }

    private <T> List<GatewayResult<T>> fanOut(String method, String path, String body, TypeReference<T> typeRef) {
        List<CompletableFuture<GatewayResult<T>>> futures = baseUrls.stream()
                .map(base -> dispatch(method, base, path, body, typeRef))
                .toList();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private <T> CompletableFuture<GatewayResult<T>> dispatch(String method, String base, String path,
                                                             String body, TypeReference<T> typeRef) {
        final URI uri = URI.create(base + path);
        var builder = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Accept", "application/json");
        if ("POST".equals(method)) {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.GET();
        }
        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .handle((response, ex) -> toResult(base, uri, response, ex, typeRef));
    }

    private <T> GatewayResult<T> toResult(String base, URI uri, HttpResponse<String> response,
                                          Throwable ex, TypeReference<T> typeRef) {
        if (ex != null) {
            log.warn("Fan-out request to {} failed: {}", uri, ex.getMessage());
            return GatewayResult.failure(base, "request failed: " + ex.getMessage());
        }
        if (response.statusCode() / 100 != 2) {
            log.warn("Fan-out request to {} returned status {}", uri, response.statusCode());
            return GatewayResult.failure(base, "status " + response.statusCode());
        }
        try {
            return GatewayResult.ok(base, mapper.readValue(response.body(), typeRef));
        } catch (Exception e) {
            log.warn("Fan-out response from {} could not be parsed: {}", uri, e.getMessage());
            return GatewayResult.failure(base, "parse error: " + e.getMessage());
        }
    }
}
