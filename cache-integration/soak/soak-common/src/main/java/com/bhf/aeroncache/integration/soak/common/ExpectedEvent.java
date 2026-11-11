package com.bhf.aeroncache.integration.soak.common;

/**
 * An event the subscription under test is expected to receive, matched type-aware against received
 * {@link StreamEvent}s. {@code type} is one of the {@link StreamEvent} type-name constants; {@code key}/
 * {@code value} are ignored for the cache-wide events (CLEAR_CACHE / DELETE_CACHE) and {@code value} is ignored
 * for REMOVE_ITEM.
 */
record ExpectedEvent(String cacheId, String type, String key, String value) {

    boolean matches(StreamEvent u) {
        if (!type.equals(u.eventType()) || !cacheId.equals(u.cacheId())) {
            return false;
        }
        return switch (type) {
            case StreamEvent.ADD_ITEM, StreamEvent.PATCH_ITEM -> key.equals(u.key()) && value.equals(u.value());
            case StreamEvent.REMOVE_ITEM -> key.equals(u.key());
            case StreamEvent.CLEAR_CACHE, StreamEvent.DELETE_CACHE -> true;
            default -> false;
        };
    }

    /** A content signature used for unordered (multiset) comparison of snapshot-hydration replays. */
    String sig() {
        return switch (type) {
            case StreamEvent.ADD_ITEM, StreamEvent.PATCH_ITEM -> type + "|" + key + "|" + value;
            case StreamEvent.REMOVE_ITEM -> type + "|" + key + "|";
            default -> type + "||";
        };
    }

    @Override
    public String toString() {
        return type + "(" + cacheId + "/" + key + "=" + value + ")";
    }
}
