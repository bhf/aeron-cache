package com.bhf.aeroncache.application.ephemeral;

import io.aeron.Aeron;
import io.aeron.Image;
import io.aeron.ConcurrentPublication;
import io.aeron.Publication;
import io.aeron.cluster.service.ClientSession;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the per client session model that makes response-channel routing correct: each client
 * image gets its own stable {@link ClientSession} keyed by {@link Image#correlationId()}, and
 * removing a session closes its response publication. This is the mechanism the cache subscription
 * service relies on (it stores {@link ClientSession} objects and offers to each), so distinct
 * sessions per client give per-session isolation for free.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Ephemeral session registry")
class EphemeralSessionRegistryTest {

    private static final int RESPONSE_STREAM_ID = 201;

    @Mock
    private Aeron aeron;

    private EphemeralSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new EphemeralSessionRegistry(aeron, "localhost:8076", RESPONSE_STREAM_ID);
    }

    @Test
    @DisplayName("creates one session per client image and reuses it for the same image")
    void reusesSessionForSameImage() {
        final Image image = image(42L);
        final ConcurrentPublication publication = mock(ConcurrentPublication.class);
        when(aeron.addPublication(anyString(), anyInt())).thenReturn(publication);

        final ClientSession first = registry.getOrCreate(image);
        final ClientSession second = registry.getOrCreate(image);

        assertSame(first, second, "same image must resolve to the same session instance");
        assertEquals(42L, first.id(), "session id must be the image correlation id");
        verify(aeron, times(1)).addPublication(anyString(), anyInt());
    }

    @Test
    @DisplayName("creates distinct sessions for distinct client images")
    void distinctSessionsPerImage() {
        final ConcurrentPublication pubA = mock(ConcurrentPublication.class);
        final ConcurrentPublication pubB = mock(ConcurrentPublication.class);
        when(aeron.addPublication(anyString(), anyInt())).thenReturn(pubA, pubB);

        final ClientSession a = registry.getOrCreate(image(1L));
        final ClientSession b = registry.getOrCreate(image(2L));

        assertNotSame(a, b);
        assertEquals(1L, a.id());
        assertEquals(2L, b.id());
        verify(aeron, times(2)).addPublication(anyString(), anyInt());
    }

    @Test
    @DisplayName("get resolves a live session and remove closes its publication")
    void removeClosesPublication() {
        final ConcurrentPublication publication = mock(ConcurrentPublication.class);
        when(aeron.addPublication(anyString(), anyInt())).thenReturn(publication);

        final ClientSession session = registry.getOrCreate(image(7L));
        assertSame(session, registry.get(7L));

        registry.remove(7L);

        verify(publication).close();
        assertNull(registry.get(7L), "removed session must no longer resolve");
    }

    @Test
    @DisplayName("offer to a session whose image has closed reports success rather than wedging the caller")
    void offerAfterImageClosedDoesNotWedge() {
        final Image image = image(9L);
        final ConcurrentPublication publication = mock(ConcurrentPublication.class);
        when(aeron.addPublication(anyString(), anyInt())).thenReturn(publication);

        final ClientSession session = registry.getOrCreate(image);

        // Client has gone: the subscription service's send loop spins while offer < 0, so a dead
        // session must return a non-negative value to let the caller move on.
        when(image.isClosed()).thenReturn(true);
        final UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocate(16));

        final long result = session.offer(buffer, 0, 16);

        org.junit.jupiter.api.Assertions.assertEquals(16, result);
    }

    private static Image image(long correlationId) {
        final Image image = mock(Image.class);
        when(image.correlationId()).thenReturn(correlationId);
        return image;
    }

    private static <T> T mock(Class<T> type) {
        return org.mockito.Mockito.mock(type);
    }
}
