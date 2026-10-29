# Near Cache Implementation

## cache-near-common

The reusable near (local) cache abstractions - `NearCacheManager` / `NearCache` and the
`ConcurrentNearCacheManager` implementation - that keep a local copy of cache contents in sync with
cluster update events.

## Near cache HTTP endpoints

The near cache read endpoints now live in the main HTTP interface
(`cache-http:http-server-javalin`) rather than a standalone service. They are served by
`NearCacheRouteHandlers` and are only registered when the `NEAR_CACHE_ENABLED` environment variable
is set to `true`.

### Read Ahead Caching

When enabled, the first read of a cache subscribes to it (requesting a snapshot) and locally caches
items on all keys, not just the ones you do GET operations on. Subsequent update events keep the
local copy in sync. A read that misses the local near cache falls back to fetching from the source
cache over the cluster.

Exposed endpoints:

- `GET /api/v1/near/cache/<cacheId>` - get the full cache snapshot from the local near cache.
- `GET /api/v1/near/cache/<cacheId>/<key>` - get a single item.

Cache creation, puts and deletes continue to use the regular `/api/v1/cache/` endpoints, which the
near cache picks up via its subscription.
