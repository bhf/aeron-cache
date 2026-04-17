package com.bhf.aeroncache.integration.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BackendTestConfig {

    /**
     * HTTP Enabled by default.
     * @return
     */
    boolean httpEnabled() default true;

    /**
     * Whether WS is enabled - false by default.
     * @return
     */
    boolean wsEnabled() default false;

    /**
     * Whether SSE is enabled for tests - false by default.
     * @return
     */
    boolean sseEnabled() default false;

    /**
     * Whether we should use clustered mode.
     * @return
     */
    boolean useClusteredMode() default true;

    /**
     * Whether integration tests should be run using TestContainers.
     * @return
     */
    boolean useTestContainersEnvironment() default false;

    /**
     * Whether the HTTP near cache is enabled for tests - false by default.
     * @return
     */
    boolean httpNearCacheEnabled() default false;
}
