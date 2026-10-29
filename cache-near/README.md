# Near Cache Implementation

## cache-near-common

The reusable near (local) cache abstractions - `NearCacheManager` / `NearCache` and the
`ConcurrentNearCacheManager` implementation - that keep a local copy of cache contents in sync with
cluster update events.

## Near cache on the HTTP interface

The near cache lives in the main HTTP interface (`cache-http:http-server-javalin`) rather than a
standalone service, and is enabled as a **deployment option** via the `NEAR_CACHE_ENABLED`
environment variable (`true`/`false`, default `false`). It is **transparent**: when enabled, the
interface serves its reads from the near cache; clients make the same calls to the same URIs either
way, so turning near caching on or off needs no client change.

### Read Ahead Caching

When enabled, the first read of a cache subscribes to it (requesting a snapshot) and locally caches
items on all keys, not just the ones you do GET operations on. Subsequent update events keep the
local copy in sync. A read that misses the local near cache falls back to fetching from the source
cache over the cluster. Note that a read of an already-warm key returns the local (eventually
consistent) copy, which can briefly lag a concurrent write until the update event arrives.

### Reads: the regular read URIs, served from the near cache

With `NEAR_CACHE_ENABLED=true`, the regular GET read routes are bound to `NearCacheRouteHandlers`
(and `CountersNearCacheRouteHandlers`) instead of the strong cluster-read handlers — there are no
separate `/api/v1/near/cache/...` read URIs. Reads served from the near cache:

- `GET /api/v1/cache/<cacheId>` / `GET /api/v1/cache/<cacheId>/<key>` - full snapshot / single item.
- `GET /api/v1/counters/<cacheId>` / `GET /api/v1/counters/<cacheId>/<key>` - counter equivalents.

Cache creation, puts and deletes always go to the cluster over the same regular endpoints, which the
near cache picks up via its subscription. This mirrors the streaming interfaces (WS/SSE), where a
hydrating subscription is already served transparently from the near cache on its normal URIs.

### Near-specific management endpoints

Only the management operations, which have no regular-endpoint equivalent, keep a dedicated
`/api/v1/near/...` surface (also registered only when the near cache is enabled):

- `GET /api/v1/near/caches` / `GET /api/v1/near/counters-caches` - list the caches currently held
  warm, with their local item counts.
- `DELETE /api/v1/near/cache/<cacheId>` / `DELETE /api/v1/near/counters/<cacheId>` - stop near
  caching a cache: drop the cluster subscription and remove the local copy (idempotent).
