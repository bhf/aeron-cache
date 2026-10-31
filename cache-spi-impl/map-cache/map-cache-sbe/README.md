# map-cache-sbe

SBE schemas for the map cache. Codecs are generated at build time from the XML in
[src/main/resources/sbe](src/main/resources/sbe) into the `com.bhf.aeroncache.messages` package.

## Message ID allocation

All three schemas share the same package and schema `id="1"`, and their codecs are consumed through the same
decoders, so **message (template) IDs must be unique across all schemas**. Each schema owns a contiguous block:

| Schema | Block | Currently used |
|---|---|---|
| [cache-schema.xml](src/main/resources/sbe/cache-schema.xml) | 100–199 | 100–126 |
| [counters-schema.xml](src/main/resources/sbe/counters-schema.xml) | 200–299 | 200–228 |
| [bulk-schema.xml](src/main/resources/sbe/bulk-schema.xml) | 300–399 | 300–301 |

New messages take the next free ID in their schema's block. Don't reuse or renumber IDs of existing messages
unless you are happy to break wire compatibility (all nodes and clients rebuilt together, no old cluster log or
snapshot replayed).

### cache-schema.xml (100–199)

| ID | Message |
|---|---|
| 100 | CreateCache |
| 101 | DeleteCache |
| 102 | ClearCache |
| 103 | AddCacheEntry |
| 104 | RemoveCacheEntry |
| 105 | CacheCreated |
| 106 | CacheEntryCreated |
| 107 | CacheEntryRemoved |
| 108 | CacheCleared |
| 109 | CacheDeleted |
| 110 | GetCacheEntry |
| 111 | CacheEntryResult |
| 112 | AllCacheEntriesResult |
| 113 | GetAllCacheEntries |
| 114 | GetCacheStats |
| 115 | AllCacheStatsResult |
| 116 | CacheSubscriptionRequest |
| 117 | CacheSubscriptionResponse |
| 118 | CacheUnsubscribeRequest |
| 119 | CacheUnsubscribeResponse |
| 120 | CacheEntryUpdate |
| 121 | PatchCacheEntry |
| 122 | CacheEntryPatched |
| 123 | CancelCacheItemRemoval |
| 124 | CacheItemRemovalCancelled |
| 125 | GetAllTimers |
| 126 | AllTimersResult |

### counters-schema.xml (200–299)

| ID | Message |
|---|---|
| 200 | CreateCounterCache |
| 201 | CreateCounterCacheResponse |
| 202 | AddCounterRequest |
| 203 | AddCounterResponse |
| 204 | RemoveCounterRequest |
| 205 | RemoveCounterResponse |
| 206 | IncrementCounterRequest |
| 207 | IncrementCounterResponse |
| 208 | DecrementCounterRequest |
| 209 | DecrementCounterResponse |
| 210 | SetCounterRequest |
| 211 | SetCounterResponse |
| 212 | DeleteCounterCache |
| 213 | DeleteCounterCacheResponse |
| 214 | CounterCacheSubscriptionRequest |
| 215 | CounterCacheSubscriptionResponse |
| 216 | CounterCacheUnsubscribeRequest |
| 217 | CounterCacheUnsubscribeResponse |
| 218 | GetCounterCacheEntry |
| 219 | CounterCacheEntryResult |
| 220 | AllCounterCacheEntriesResult |
| 221 | GetAllCounterCacheEntries |
| 222 | ClearCounterCacheRequest |
| 223 | ClearCounterCacheResponse |
| 224 | CounterCacheEntryUpdate |
| 225 | GetCounterStats |
| 226 | AllCounterCacheStatsResult |
| 227 | CancelCounterItemRemoval |
| 228 | CounterItemRemovalCancelled |

### bulk-schema.xml (300–399)

| ID | Message |
|---|---|
| 300 | BulkOperationRequest |
| 301 | BulkOperationResponse |
