package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.streaming.AbstractBidiCommandTests;

/** Unclustered (ephemeral) variant of the command-over-socket BIDI suite. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralBidiCommandTests extends AbstractBidiCommandTests {

    @Override
    protected String cacheIdPrefix() {
        return "EphemeralBidiCommandTests";
    }

    /**
     * Cache stats are not served by the unclustered single-node backend (no ephemeral stats coverage
     * exists anywhere in the suite), so the getStats scenario is not applicable here. Overriding without
     * {@code @Test} means JUnit does not run it for this variant.
     */
    @Override
    protected void shouldReturnStatsOverSocket() {
        // Not applicable in unclustered mode.
    }
}
