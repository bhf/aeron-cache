package com.bhf.aeroncache.fanout.client;

/**
 * The outcome of a single fan-out call to one downstream gateway.
 *
 * <p>A gateway that is unreachable, times out, returns a non-2xx status or sends an unparseable body
 * yields a failed result rather than aborting the whole fan-out, so the aggregate can degrade
 * gracefully (the failed gateway is simply excluded, or surfaced as an unreachable contributor).
 *
 * @param gateway the downstream gateway base URL this result came from.
 * @param ok      whether the call succeeded and the body parsed.
 * @param value   the parsed response, or {@code null} when {@code ok} is false.
 * @param error   the failure reason, or {@code null} when {@code ok} is true.
 * @param <T>     the response type.
 */
public record GatewayResult<T>(String gateway, boolean ok, T value, String error) {

    /**
     * A successful result.
     *
     * @param gateway the gateway base URL.
     * @param value   the parsed response.
     * @param <T>     the response type.
     * @return the result.
     */
    public static <T> GatewayResult<T> ok(String gateway, T value) {
        return new GatewayResult<>(gateway, true, value, null);
    }

    /**
     * A failed result.
     *
     * @param gateway the gateway base URL.
     * @param error   the failure reason.
     * @param <T>     the response type.
     * @return the result.
     */
    public static <T> GatewayResult<T> failure(String gateway, String error) {
        return new GatewayResult<>(gateway, false, null, error);
    }
}
