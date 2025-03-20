package com.bhf.aeroncache.http.responses;

/**
 * The response to a bad request.
 * @param errorMsg The error that occurred.
 * @param helpMsg Any helpful hints.
 */
public record RequestErrorResponse(String errorMsg, String helpMsg) {

}
