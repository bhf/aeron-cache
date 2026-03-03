rootProject.name = "aeron-cache"

include("cache-cluster")
include("cache-client")
include("cache-common")
include("cache-messages-sbe")
include("cache-messages-http")
include("cache-http:http-server-javalin")
include("cache-near:http-server-near-javalin")
include("cache-ws:ws-server-javalin")
include("cache-sse:sse-server-jooby")
include("cache-http:http-clustertools")

include("cache-integration:integration-tests-common")
include("cache-integration:http-integration-tests")