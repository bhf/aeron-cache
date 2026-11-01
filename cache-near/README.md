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
- `POST /api/v1/near/cache/<cacheId>` / `POST /api/v1/near/counters/<cacheId>` - prime a cache up
  front: start the internal subscription (requesting a snapshot) so the local copy is warmed before any
  read or hydrating subscription arrives (which then rides the warm copy). The snapshot populates the
  local copy asynchronously, so success means priming was started; idempotent (an already-warm cache
  reports `wasAlreadyWarm=true` and is not re-subscribed).
- `DELETE /api/v1/near/cache/<cacheId>` / `DELETE /api/v1/near/counters/<cacheId>` - stop near
  caching a cache: drop the cluster subscription and remove the local copy (idempotent).

## Near cache on the streaming interfaces (WS / SSE / bidi)

The streaming interfaces (`cache-ws`, `cache-sse`) take the same `NEAR_CACHE_ENABLED` deployment
option and are equally transparent. Near caching engages for **FULL-mode, hydrating** subscriptions:
the first *whole-cache* subscriber to a cold cache hydrates from a single cluster snapshot while teeing
that snapshot into the near cache, and an internal subscription then keeps the local copy warm and in
sync after the client disconnects. Later hydrating subscribers to a warm cache are served entirely from
the local copy, with no per-subscriber cluster snapshot - whole-cache subscribers get the full copy, and
**key-based subscribers get just the keys they asked for**, replayed from the local copy and then kept in
sync (filtered to those keys) through the internal subscription. A key-based subscription to a *cold*
cache still takes the plain cluster path: a key snapshot carries only the requested keys, so it can never
warm a full local copy - warm the cache first with a whole-cache subscription or an explicit prime.

### Near-cache-generated patch deltas

A **patch-mode** (`?mode=patch`) hydrating subscription to a **warm** cache is served from the near cache
too, but its updates are delivered as deltas: the subscriber receives a full `ADD_ITEM` baseline on
hydration (and for any first-seen key), and thereafter the near cache computes an RFC 7386 JSON merge patch
between the value it already holds and each incoming full value, and forwards just that delta as a
`PATCH_ITEM`. The internal near cache session itself stays on a FULL subscription (it needs whole values to
diff against) - no cluster-side per-subscriber patch subscription is involved, so the delta computation is
offloaded to the near-cache layer. This is distinct from the cluster's write-driven patch subscriptions
(which relay patches authored by `patchValue` writes); here the delta is synthesised locally from the warm
copy. A patch subscription to a *cold* cache takes the plain cluster path (warm or prime the cache first to
get near-generated deltas), and patch deltas are only generated for JSON (string) values, not scalar
counters.

This is transparent to downstream consumers that embed a near cache: `ConcurrentNearCacheManager` applies a
`PATCH_ITEM` by merging the delta onto the value it already holds (`JsonMergePatch.applyMergePatch`), so an
embedded client just feeds every `CacheUpdateEvent` into its near cache manager as before and reconstructs
full values with no extra code. Both the merge-patch *compute* (produce) and *apply* directions live in
`com.bhf.aeroncache.near.patch.JsonMergePatch`, self-contained to the near-cache layer and independent of
the cluster's write-side patch provider.

Both the one-directional subscription routes and the single bidirectional endpoint
(`/api/ws/v1/bidi`) share the same subscription publishers, so a whole-cache hydrating subscribe over
either is served from the same near cache. The near cache management endpoints
(`/api/v1/near/caches`, `/api/v1/near/counters-caches`, and the `POST` prime / `DELETE` stop
equivalents) are served over HTTP on the streaming interface's own port.

## Near cache on the Aeron gateway

The Aeron transport gateway (`cache-aeron-gateway`) takes the same `NEAR_CACHE_ENABLED` option and
applies near caching to its subscription path identically: whole-cache and key-based hydrating subscribes
over the gateway's Aeron response channel warm and are served from the near cache exactly as on the
streaming interfaces. The gateway keeps its zero-allocation dispatch (a reused `GatewayStreamUpdate` flyweight)
on the hot path and feeds the near cache through the allocation-free
`NearCacheManager.handleCacheUpdate(cacheId, eventType, key, value)` overload. The near cache
management endpoints are served over HTTP on the gateway's health server port
(`/api/v1/near/caches`, `/api/v1/near/counters-caches`, and the `POST` prime / `DELETE` stop
equivalents).

Near-cache-generated patch deltas (see above) are not yet offered on the gateway - a patch-mode subscribe
over the gateway takes the plain cluster path. (It would need a flag on the gateway's SBE subscribe message.)
