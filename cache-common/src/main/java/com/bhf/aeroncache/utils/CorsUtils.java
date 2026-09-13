package com.bhf.aeroncache.utils;

import java.util.Arrays;
import java.util.List;

/**
 * Utilities and helpers for CORS configuration on the web facing applications.
 */
public class CorsUtils {

    /**
     * Environment variable holding a comma separated list of allowed CORS origins/hosts.
     */
    public static final String CORS_ALLOWED_ORIGINS_ENV = "CORS_ALLOWED_ORIGINS";

    /**
     * Resolve the list of allowed CORS origins.
     * <p>
     * When the {@value #CORS_ALLOWED_ORIGINS_ENV} environment variable is set, its comma separated
     * values (trimmed, blanks removed) are used. Otherwise the supplied defaults are returned so the
     * existing hardcoded behaviour is preserved when the deployment does not configure origins.
     *
     * @param defaults the fallback origins to use when the environment variable is not set.
     * @return the ordered list of allowed origins.
     */
    public static List<String> getAllowedOrigins(final String... defaults) {
        final String configured = System.getenv(CORS_ALLOWED_ORIGINS_ENV);
        if (null == configured || configured.isBlank()) {
            return List.of(defaults);
        }

        final List<String> origins = Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        return origins.isEmpty() ? List.of(defaults) : origins;
    }
}
