package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.messages.OperationStatus;

/**
 * The response to a bad request.
 * @param errorMsg The error that occurred.
 * @param helpMsg Any helpful hints.
 * @param operationStatus The status of the operation.
 */
public record RequestErrorResponse(String errorMsg, String helpMsg, OperationStatus operationStatus) {

}
