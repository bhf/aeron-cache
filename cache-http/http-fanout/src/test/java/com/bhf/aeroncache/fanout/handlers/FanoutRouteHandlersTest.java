package com.bhf.aeroncache.fanout.handlers;

import com.bhf.aeroncache.fanout.client.GatewayFanoutClient;
import com.bhf.aeroncache.fanout.client.GatewayResult;
import com.bhf.aeroncache.http.responses.ClusterToolsResponse;
import com.bhf.aeroncache.models.snapshot.SnapshotInfoResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import io.javalin.http.Context;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that the {@code /api/v1/multi/...} snapshot handlers serve the raw per-backend list
 * ({@link GatewayResult} per gateway) rather than collapsing it into the single representative
 * result the non-multi handlers return.
 */
class FanoutRouteHandlersTest {

    @Test
    void multiSnapshotInfoServesPerBackendListRatherThanRepresentative() {
        // Arrange - two gateways reporting distinct snapshot state, one with a snapshot present.
        var client = mock(GatewayFanoutClient.class);
        var ctx = mock(Context.class);
        var present = new SnapshotInfoResponse(true, 100L, 0L, 1L, 7L, 2, 2048L, null);
        var absent = new SnapshotInfoResponse(false, -1L, -1L, -1L, -1L, 0, 0L, null);
        List<GatewayResult<SnapshotInfoResponse>> perBackend = List.of(
                GatewayResult.ok("http://cache-a:7070", present),
                GatewayResult.ok("http://cache-b:7070", absent));
        when(client.getAll(eq("/api/v1/snapshot-info"), any(TypeReference.class))).thenReturn((List) perBackend);
        var handlers = new FanoutRouteHandlers(client);

        // Act
        handlers.handleGetSnapshotInfoMulti(ctx);

        // Assert - the exact per-backend list is served, not a single representative SnapshotInfoResponse.
        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(ctx).json(captor.capture());
        assertInstanceOf(List.class, captor.getValue());
        assertEquals(perBackend, captor.getValue());
    }

    @Test
    void multiSnapshotServesPerBackendClusterToolsResults() {
        // Arrange - one gateway snapshots cleanly, the other reports a non-zero exit code.
        var client = mock(GatewayFanoutClient.class);
        var ctx = mock(Context.class);
        List<GatewayResult<ClusterToolsResponse>> perBackend = List.of(
                GatewayResult.ok("http://cache-a:7070", new ClusterToolsResponse("snapshot", "folder-a", 0)),
                GatewayResult.ok("http://cache-b:7070", new ClusterToolsResponse("snapshot", "folder-b", -1)));
        when(client.postAll(eq("/api/v1/snapshot"), eq(""), any(TypeReference.class))).thenReturn((List) perBackend);
        var handlers = new FanoutRouteHandlers(client);

        // Act
        handlers.handleSnapshotMulti(ctx);

        // Assert - both backends' results are served individually, so a failed backend stays visible.
        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(ctx).json(captor.capture());
        assertEquals(perBackend, captor.getValue());
    }
}
