package com.bhf.aeroncache.integration.shutdown;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.config.TestEndpointsProvider;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import com.bhf.aeroncache.integration.utils.ContainerRestartUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Restart recovery specifically through the <em>snapshot</em> path.
 *
 * <p>{@link ClusterRestartTests} restarts the cluster without taking a snapshot, so the node recovers
 * by replaying the command log - which never invokes {@code loadSnapshot}. This test instead takes a
 * real snapshot before restarting, so recovery loads that snapshot. Because the snapshot is the last
 * action before the stop, there is no post-snapshot log to replay: state can only come from
 * {@code loadSnapshot}, which is where snapshot serialization bugs live.
 *
 * <p>It seeds both a regular cache and a counter cache. Those are snapshotted by two managers that
 * share one snapshot stream, so asserting both recover guards the manager boundary - the regular
 * cache loader must stop at its own end marker and leave the counter records for the counters
 * manager, rather than consuming them.
 *
 * <p>The snapshot is triggered by running Aeron's {@code ClusterTool snapshot} inside the node
 * container (the same mechanism the product's clustertools sidecar uses), avoiding any dependency on
 * a running sidecar in the standard test environment.
 */
@ExtendWith(BackendTestLauncher.class)
@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusterSnapshotRestartTests {

    private static final String GET_ENDPOINT = "/api/v1/cache/";
    private static final String GET_COUNTERS_ENDPOINT = "/api/v1/counters/";

    private static final TestEndpointsProvider cacheEndpoints = new CacheTestEndpoints();
    private static final TestEndpointsProvider countersEndpoints = new CounterTestEndpoints();

    private static final String CACHE_ID = "snapshot-cache";
    private static final String CACHE_KEY = "cacheKey";
    private static final String CACHE_VALUE = "cacheValue";

    private static final String COUNTERS_CACHE_ID = "snapshot-counters";
    private static final String COUNTERS_KEY = "counterKey";
    private static final Integer COUNTERS_VALUE = 4242;

    // The node writes its cluster directory under CACHE_DATA_DIR (/tmp/data) as node0/cluster.
    private static final String NODE_CLUSTER_DIR = "/tmp/data/node0/cluster";

    @Test
    @DisplayName("Should recover regular and counter caches from a snapshot after a restart")
    @HappyPath
    void shouldRecoverCachesAndCountersFromSnapshotPostRestart(BackendTestResource backend) throws Exception {
        // Arrange - seed a regular cache and a counter cache with known values.
        CacheTestUtils.createCache(CACHE_ID, backend, cacheEndpoints);
        CacheTestUtils.addItem(CACHE_ID, CACHE_KEY, CACHE_VALUE, backend, cacheEndpoints);

        CacheTestUtils.createCache(COUNTERS_CACHE_ID, backend, countersEndpoints);
        CacheTestUtils.addItem(COUNTERS_CACHE_ID, COUNTERS_KEY, COUNTERS_VALUE, backend, countersEndpoints);

        // Take a real snapshot as the last action, so restart recovery is snapshot-only.
        takeSnapshot(backend);

        // Act - restart the cluster; with a snapshot present, recovery goes through loadSnapshot.
        ContainerRestartUtils.stopHTTPInterface(backend);
        ContainerRestartUtils.stopClusterContainers(backend);
        ContainerRestartUtils.awaitAeronCacheClusterRestart(backend);
        var mapped = ContainerRestartUtils.awaitHTTPInterfaceRestart(backend);

        // Assert - both the regular cache entry and the counter survived the snapshot round-trip.
        RestAssured.given().port(mapped.mappedPort())
                .baseUri(mapped.mappedHost())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when().get(GET_ENDPOINT + CACHE_ID + "/" + CACHE_KEY)
                .then().assertThat()
                .statusCode(200)
                .body("value", Matchers.comparesEqualTo(CACHE_VALUE));

        RestAssured.given().port(mapped.mappedPort())
                .baseUri(mapped.mappedHost())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when().get(GET_COUNTERS_ENDPOINT + COUNTERS_CACHE_ID + "/" + COUNTERS_KEY)
                .then().assertThat()
                .statusCode(200)
                .body("value", Matchers.comparesEqualTo(COUNTERS_VALUE));
    }

    /**
     * Take a cluster snapshot by running {@code ClusterTool snapshot} inside the node container. The
     * command applies the snapshot synchronously and prints a success line we assert on, so the
     * snapshot is durable before we proceed to restart.
     */
    private static void takeSnapshot(BackendTestResource backend) throws Exception {
        var node = backend.getContainers().clusterContainers().get(0);
        var result = node.execInContainer("java",
                "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
                "-cp", "@/app/jib-classpath-file",
                "io.aeron.cluster.ClusterTool",
                NODE_CLUSTER_DIR, "snapshot");

        var output = result.getStdout() + result.getStderr();
        assertTrue(output.contains("SNAPSHOT applied successfully"),
                "ClusterTool snapshot did not confirm success. Output:\n" + output);
    }
}
