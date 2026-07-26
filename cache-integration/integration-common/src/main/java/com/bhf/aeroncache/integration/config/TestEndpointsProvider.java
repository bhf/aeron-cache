package com.bhf.aeroncache.integration.config;

public interface TestEndpointsProvider {

    String getCreateEndpointCache();
    String getPutItemEndpointCache();
    String getDeleteEndpointCache();
    String getClearEndpointCache();
    String getBulkItemEndpointCache();
    String getRemoveItemEndpoint();

    String getItemEndpoint();
}
