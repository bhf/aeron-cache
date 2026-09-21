package com.bhf.aeroncache.transport;

import io.aeron.ChannelUriStringBuilder;
import lombok.extern.log4j.Log4j2;

/**
 * The Aeron media used by the transport gateway's response-channel transport, either UDP (the
 * default) or IPC.
 * <p>
 * Response channels ({@code control-mode=response}) work over both media in Aeron: the driver keys
 * each client's response publication on the request publication's {@code response-correlation-id}
 * regardless of media, so no endpoints are required for the correlation itself. The only difference
 * is that UDP channels carry {@code endpoint}/{@code control-endpoint}/{@code response-endpoint}
 * addresses, whereas IPC channels have none - so those params are omitted for {@link #IPC} (setting
 * an endpoint on an {@code aeron:ipc} channel is rejected by the driver).
 * <p>
 * IPC requires the gateway client and server to share a single media driver (same host, same
 * {@code aeron.dir}); it is intended for co-located deployments. UDP remains the default for
 * everything else.
 */
@Log4j2
public enum TransportMedia {

    UDP("udp"),
    IPC("ipc");

    /** Environment variable selecting the gateway transport media ({@code udp} or {@code ipc}). */
    public static final String MEDIA_ENV = "GATEWAY_TRANSPORT_MEDIA";

    private final String media;

    TransportMedia(String media) {
        this.media = media;
    }

    /** @return the Aeron media name ({@code udp} or {@code ipc}). */
    public String media() {
        return media;
    }

    public boolean isIpc() {
        return this == IPC;
    }

    /**
     * Resolve the transport media from the {@link #MEDIA_ENV} environment variable, defaulting to
     * {@link #UDP} when unset or unrecognised.
     */
    public static TransportMedia fromEnv() {
        return parse(System.getenv(MEDIA_ENV));
    }

    /**
     * Parse a media selection, defaulting to {@link #UDP} when {@code null}, blank, or unrecognised.
     * An unrecognised value is logged and treated as UDP so a typo can never silently disable the
     * transport.
     */
    public static TransportMedia parse(String value) {
        if (value == null || value.isBlank()) {
            return UDP;
        }
        switch (value.trim().toLowerCase()) {
            case "ipc":
                return IPC;
            case "udp":
                return UDP;
            default:
                log.warn("Unrecognised {}={}, defaulting to udp", MEDIA_ENV, value);
                return UDP;
        }
    }

    // ------------------------------------------------------------------ channel builders
    //
    // Each method returns a fresh builder so the caller can chain the correlation id, e.g.
    // responseChannel(ep).responseCorrelationId(id).build(). For IPC the endpoint/control-endpoint/
    // response-endpoint params are omitted - IPC channels have no addresses.

    /**
     * Base builder for the client's request {@link io.aeron.ExclusivePublication}. The caller adds
     * {@code responseCorrelationId(responseSubscription.registrationId())}.
     *
     * @param requestEndpoint the server's request endpoint (host:port); ignored for IPC.
     */
    public ChannelUriStringBuilder requestPublication(String requestEndpoint) {
        final ChannelUriStringBuilder builder = new ChannelUriStringBuilder().media(media);
        if (this == UDP) {
            builder.endpoint(requestEndpoint);
        }
        return builder;
    }

    /**
     * Builder for the server's request {@link io.aeron.Subscription}.
     *
     * @param requestEndpoint         the endpoint the server listens on (host:port); ignored for IPC.
     * @param responseControlEndpoint the response control endpoint advertised to clients; ignored for IPC.
     */
    public ChannelUriStringBuilder requestSubscription(String requestEndpoint, String responseControlEndpoint) {
        final ChannelUriStringBuilder builder = new ChannelUriStringBuilder().media(media);
        if (this == UDP) {
            builder.endpoint(requestEndpoint).responseEndpoint(responseControlEndpoint);
        }
        return builder;
    }

    /**
     * Base builder for a response channel ({@code control-mode=response}) - used both for the client's
     * response {@link io.aeron.Subscription} and the server's per-session response
     * {@link io.aeron.Publication}. The caller adds {@code responseCorrelationId(...)} on the server
     * side (the request image's correlation id).
     *
     * @param responseControlEndpoint the response control endpoint (host:port); ignored for IPC.
     */
    public ChannelUriStringBuilder responseChannel(String responseControlEndpoint) {
        final ChannelUriStringBuilder builder = new ChannelUriStringBuilder()
                .media(media)
                .controlMode("response");
        if (this == UDP) {
            builder.controlEndpoint(responseControlEndpoint);
        }
        return builder;
    }
}
