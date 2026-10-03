package com.bhf.aeroncache.integration.soak.sse;

import com.bhf.aeroncache.integration.soak.common.SoakStreamSubscriber;
import com.bhf.aeroncache.integration.soak.common.SoakStreamSubscriberFactory;
import com.bhf.aeroncache.integration.soak.common.StreamSubscriptionUrls;
import com.bhf.aeroncache.integration.soak.common.SubscriptionSpec;
import okhttp3.OkHttpClient;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSources;

import java.util.concurrent.TimeUnit;

/**
 * Opens {@link SseSoakStreamSubscriber}s against the in-process SSE server on the given port, building each
 * subscription URL from the shared {@link StreamSubscriptionUrls} template (SSE uses the {@code http} scheme).
 * One shared {@link OkHttpClient}/{@link EventSource.Factory} backs every subscription, so a round's
 * {@code close()} only cancels that round's EventSource - no per-round client churn over a long run. The client
 * is shut down when the factory is closed.
 */
final class SseSoakStreamSubscriberFactory implements SoakStreamSubscriberFactory, AutoCloseable {

    private final int ssePort;
    private final OkHttpClient client;
    private final EventSource.Factory eventSourceFactory;

    SseSoakStreamSubscriberFactory(int ssePort) {
        this.ssePort = ssePort;
        this.client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
        this.eventSourceFactory = EventSources.createFactory(client);
    }

    @Override
    public SoakStreamSubscriber open(SubscriptionSpec spec) {
        var uri = "http://localhost:" + ssePort + StreamSubscriptionUrls.path("sse", spec);
        return new SseSoakStreamSubscriber(eventSourceFactory, uri);
    }

    @Override
    public void close() {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }
}
