package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import lombok.extern.log4j.Log4j2;

/**
 * Decode SBE messages representing cache actions. This level of
 * abstraction is an implementation which does have responsibility for
 * message decoding and encoding.
 */
@Log4j2
public class SBEDecodingCacheClusterService<I extends Reusable, K extends Reusable, V extends Reusable> extends AbstractCacheClusterService<I, K, V>{

    public SBEDecodingCacheClusterService(String nodeId, CacheTracingService tracingService, CacheManagerFactory<I, K, V> cacheManagerFactory) {
        super(nodeId, tracingService, cacheManagerFactory);
    }

    public SBEDecodingCacheClusterService(String nodeId, CacheTracingService tracingService, CacheManagerFactory<I, K, V> cacheManagerFactory, boolean dynamicCacheCreation) {
        super(nodeId, tracingService, cacheManagerFactory);
        setDynamicCacheCreationEnabled(dynamicCacheCreation);
    }

}
