package com.bhf.aeroncache.integration.soak.streamingws;

import com.bhf.aeroncache.integration.soak.common.SoakStreamSubscriber;
import com.bhf.aeroncache.integration.soak.common.SoakStreamSubscriberFactory;
import com.bhf.aeroncache.integration.soak.common.StreamSubscriptionUrls;
import com.bhf.aeroncache.integration.soak.common.SubscriptionSpec;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

/**
 * Opens {@link WsSoakStreamSubscriber}s against the in-process streaming WebSocket server on the given port,
 * building each subscription URL from the shared {@link StreamSubscriptionUrls} template. One shared
 * {@link OkHttpClient} backs every subscription (OkHttp multiplexes many websockets over one client), so a
 * round's {@code close()} only tears down that round's socket - no per-round client/thread-pool churn over a
 * long run. The single client is shut down when the factory is closed.
 */
final class WsSoakStreamSubscriberFactory implements SoakStreamSubscriberFactory, AutoCloseable {

    private final int wsPort;
    private final OkHttpClient client;

    WsSoakStreamSubscriberFactory(int wsPort) {
        this.wsPort = wsPort;
        this.client = new OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
    }

    @Override
    public SoakStreamSubscriber open(SubscriptionSpec spec) {
        var uri = "ws://localhost:" + wsPort + StreamSubscriptionUrls.path("ws", spec);
        return new WsSoakStreamSubscriber(client, uri);
    }

    @Override
    public void close() {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }
}
