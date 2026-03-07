package com.bhf.aeroncache.integration.clients;

import com.bhf.aeroncache.integration.BackendTestResource;

import java.util.concurrent.CompletableFuture;

public interface StreamingHelper {
    CompletableFuture<String> getSingleValue(BackendTestResource backend);
}
