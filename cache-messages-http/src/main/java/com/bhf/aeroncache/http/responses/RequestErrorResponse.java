package com.bhf.aeroncache.http.responses;

/**
 * The response to a bad request.
 * @param errorMsg The error that occurred.
 * @param helpMsg Any helpful hints.
 * @param operationStatus The status of the operation.
 */
public record RequestErrorResponse(String errorMsg, String helpMsg, com.bhf.aeroncache.messages.CacheOperationStatus operationStatus) {

}
