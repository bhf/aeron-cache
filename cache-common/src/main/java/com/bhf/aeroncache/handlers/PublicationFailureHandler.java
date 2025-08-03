package com.bhf.aeroncache.handlers;

/**
 * General handling for failing to publish.
 */
public interface PublicationFailureHandler {
    void handleOfferFailure(long offered);
}
