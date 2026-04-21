rootProject.name = "aeron-cache"

include("cache-spi")
include("cache-common")
include("cache-spi-impl:map-cache:map-cache-core")
include("cache-spi-impl:map-cache:map-cache-sbe")

include("cache-cluster")
include("cache-client")
include("cache-messages-http")

include("cache-http:http-server-javalin")
include("cache-near:http-server-near-javalin")
include("cache-ws:ws-server-javalin")
include("cache-sse:sse-server-jooby")
include("cache-http:http-clustertools")

include("cache-integration:integration-common")
include("cache-integration:integration-common-http")
include("cache-integration:integration-common-streaming")
include("cache-integration:integration-common-streaming")

include("cache-integration:clustered:http-integration-tests")
include("cache-integration:clustered:http-near-integration-tests")
include("cache-integration:clustered:ws-integration-tests")
include("cache-integration:clustered:sse-integration-tests")
include("cache-integration:clustered:multistream-integration-tests")
include("cache-integration:clustered:shutdown-integration-tests")

include("cache-integration:singlenode:core-integration-tests")
include("cache-spi-impl:map-cache:map-cache-test")