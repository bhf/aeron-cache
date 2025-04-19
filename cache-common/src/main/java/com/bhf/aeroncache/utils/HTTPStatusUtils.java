package com.bhf.aeroncache.utils;

import com.bhf.aeroncache.messages.OperationStatus;

/**
 * Mappings of internal statuses to HTTP codes.
 */
public class HTTPStatusUtils {

    public static final int SERVICE_READY = 200;
    public static final int SERVICE_NOT_READY = 503;
    public static final int SERVICE_LIVE = 200;
    public static final int SERVICE_NOT_LIVE = 503;
    public static final int BAD_REQUEST = 400;

    /**
     * Get the HTTP code associated with this {@link OperationStatus}.
     *
     * @param status The status of the operation.
     * @return The associated HTTP status code.
     */
    public static int getHTTPCode(OperationStatus status) {
        return switch (status) {
            case NONE -> 218;
            case SUCCESS -> 200;
            case ERROR -> 500;
            case CACHE_EXISTS, DUPLICATE_SUBSCRIPTION -> 400;
            case UNKNOWN_CACHE, UNKNOWN_KEY, UNKNOWN_SUBSCRIPTION -> 404;
            case NULL_VAL -> 418;
        };
    }
}
