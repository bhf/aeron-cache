package com.bhf.aeroncache.integration.config;

import lombok.Getter;

@Getter
public class CacheTestEndpoints implements TestEndpointsProvider{
    final String CREATE_ENDPOINT_CACHE = "/api/v1/cache/";
    final String PUT_ITEM_ENDPOINT_CACHE = "/api/v1/cache/";
    final String DELETE_ENDPOINT_CACHE = "/api/v1/cache/";
    final String CLEAR_ENDPOINT_CACHE = "/api/v1/cache/";
    final String BULK_ITEM_ENDPOINT_CACHE = "/api/v1/cache/bulkops/";
    final String REMOVE_ITEM_ENDPOINT = "/api/v1/cache/";
    final String GET_ITEM_ENDPOINT = "/api/v1/cache/";

    @Override
    public String getCreateEndpointCache() {
        return CREATE_ENDPOINT_CACHE;
    }

    @Override
    public String getPutItemEndpointCache() {
        return PUT_ITEM_ENDPOINT_CACHE;
    }

    @Override
    public String getDeleteEndpointCache() {
        return DELETE_ENDPOINT_CACHE;
    }

    @Override
    public String getClearEndpointCache() {
        return CLEAR_ENDPOINT_CACHE;
    }

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
}
