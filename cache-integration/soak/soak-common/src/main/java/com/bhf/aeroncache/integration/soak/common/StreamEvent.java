package com.bhf.aeroncache.integration.soak.common;

/**
 * A transport-neutral cache stream update observed by a {@link SoakStreamSubscriber}. The URL-based streaming
 * soaks (WebSocket and SSE) normalise each {@code CacheUpdateEvent} frame into this record so the shared
 * {@link UrlStreamingSoakWorkload} can verify received streams without depending on any transport's wire type.
 *
 * <p>{@code eventType} is the {@code CacheUpdateEvent.EventType} name (ADD_ITEM / PATCH_ITEM / REMOVE_ITEM /
 * CLEAR_CACHE / DELETE_CACHE). {@code value} is the item value rendered as text (counter values arrive as
 * numbers over the wire and are rendered as their decimal string), matching how the workload builds its
 * expectations; {@code key} and {@code value} are {@code null} for cache-wide events.
 */
public record StreamEvent(String eventType, String cacheId, String key, String value) {
}
