package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;

@BackendTestConfig(dynamicCacheCreationEnabled = true, httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralDynamicCacheCreation extends DynamicCreateCacheTests{
}
