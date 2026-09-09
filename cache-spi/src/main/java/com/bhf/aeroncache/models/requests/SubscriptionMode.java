package com.bhf.aeroncache.models.requests;

/**
 * The kind of subscription a client is requesting for a cache or key.
 *
 * <ul>
 *     <li>{@link #FULL} - the subscriber receives full entry values on every update.</li>
 *     <li>{@link #PATCH} - the subscriber receives patch deltas rather than merged values.</li>
 * </ul>
 */
public enum SubscriptionMode {
    FULL,
    PATCH
}
