package com.bhf.aeroncache.integration.config;

public interface StreamingTestEndpointsProvider {


    String getStreamingApiPrefix();

    String getStreamingMultiCacheApiPrefix();

    String getStreamingHydrateApiPrefix();

    String getStreamingMultiCacheHydrateApiPrefix();
}
