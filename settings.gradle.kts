rootProject.name = "aeron-cache"

include("cache-spi")
include("cache-common")
include("cache-spi-impl:map-cache:map-cache-core")
include("cache-spi-impl:map-cache:map-cache-sbe")
include("cache-spi-impl:map-cache:map-cache-test")

include("cache-cluster")
include("cache-client")
include("cache-messages-http")

include("cache-http:http-server-javalin")
include("cache-near:http-server-near-javalin")
include("cache-ws:ws-server-javalin")
include("cache-sse:sse-server-jooby")
include("cache-http:http-clustertools")
include("cache-monolith")

include("cache-integration:integration-common")
include("cache-integration:integration-common-http")
include("cache-integration:integration-common-streaming")

include("cache-integration:clustered:http-integration-tests")
include("cache-integration:clustered:http-near-integration-tests")
include("cache-integration:clustered:http-dynamic-integration-tests")
include("cache-integration:clustered:ws-integration-tests")
include("cache-integration:clustered:sse-integration-tests")
include("cache-integration:clustered:multistream-integration-tests")
include("cache-integration:clustered:multistream-restart-tests")
include("cache-integration:clustered:multistream-dynamic-integration-tests")
include("cache-integration:clustered:shutdown-integration-tests")

include("cache-integration:ephemeral:core-integration-tests")
include("cache-integration:ephemeral:dynamic-integration-tests")
