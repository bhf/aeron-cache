package com.bhf.aeroncache.integration.config;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public class CounterTestEndpoints implements TestEndpointsProvider{

    final String CREATE_ENDPOINT_COUNTER = "/api/v1/counter/";
    final String PUT_ITEM_ENDPOINT_COUNTER = "/api/v1/counter/";
    final String DELETE_ENDPOINT_COUNTER = "/api/v1/counter/";
    final String CLEAR_ENDPOINT_COUNTER = "/api/v1/counter/";
    final String REMOVE_ITEM_ENDPOINT = "/api/v1/counter/";
    final String GET_ITEM_ENDPOINT = "/api/v1/counter/";

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
        return null;
    }

    @Override
    public String getRemoveItemEndpoint() {
        return REMOVE_ITEM_ENDPOINT;
    }

    @Override
    public String getItemEndpoint() {
        return GET_ITEM_ENDPOINT;
    }
}
