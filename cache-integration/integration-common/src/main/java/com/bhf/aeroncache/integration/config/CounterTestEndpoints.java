package com.bhf.aeroncache.integration.config;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public class CounterTestEndpoints implements TestEndpointsProvider{

    final String CREATE_ENDPOINT_COUNTER = "/api/v1/counters/";
    final String PUT_ITEM_ENDPOINT_COUNTER = "/api/v1/counters/";
    final String DELETE_ENDPOINT_COUNTER = "/api/v1/counters/";
    final String CLEAR_ENDPOINT_COUNTER = "/api/v1/counters/";
    final String REMOVE_ITEM_ENDPOINT = "/api/v1/counters/";
    final String GET_ITEM_ENDPOINT = "/api/v1/counters/";
    final String BULK_ITEM_ENDPOINT_CACHE = "/api/v1/cache/bulkops/";

    @Override
    public String getCreateEndpointCache() {
        return CREATE_ENDPOINT_COUNTER;
    }

    @Override
    public String getPutItemEndpointCache() {
        return PUT_ITEM_ENDPOINT_COUNTER;
    }

    @Override
    public String getDeleteEndpointCache() {
        return DELETE_ENDPOINT_COUNTER;
    }

    @Override
    public String getClearEndpointCache() {
        return CLEAR_ENDPOINT_COUNTER;
    }

    @Nullable
    @Override
    public String getBulkItemEndpointCache() {
        return BULK_ITEM_ENDPOINT_CACHE;
    }

    @Override
    public String getRemoveItemEndpoint() {
        return REMOVE_ITEM_ENDPOINT;
    }

    @Override
    public String getItemEndpoint() {
        return GET_ITEM_ENDPOINT;
    }

    @Override
    public String getPatchItemEndpointCache() {
        return PUT_ITEM_ENDPOINT_COUNTER;
    }
}
