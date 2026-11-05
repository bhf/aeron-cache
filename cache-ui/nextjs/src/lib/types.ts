/**
 * Type definitions.
 */

/**
 * Information about a specific cache instance.
 */
export interface CacheInfo {
    cacheId: number
    itemCount: number
}

/**
 * The gateway whose near cache is being managed. Each gateway keeps its own independent near cache,
 * reachable over its own HTTP management port, so near cache operations are always scoped to one of
 * these.
 */
export type NearCacheGateway = "http" | "ws" | "sse" | "aeron"

/**
 * Which flavour of near cache an operation targets: regular caches or counter caches. These map to the
 * two families of near management endpoints on each gateway ({@code /near/cache...} vs
 * {@code /near/counters...}).
 */
export type NearCacheKind = "caches" | "counters"

/**
 * A cache currently held warm in a gateway's near cache, with the number of items in the local copy.
 */
export interface WarmCacheInfo {
    cacheId: string
    itemCount: number
}

/**
 * The outcome of a near cache management operation (prime or stop) as surfaced to the UI.
 *
 * @property cacheId        the cache the operation targeted.
 * @property operationStatus the backend operation status (e.g. "SUCCESS"), when the request reached the gateway.
 * @property wasAlreadyWarm  for a prime: whether the cache was already warm (no re-subscribe happened).
 * @property wasCached       for a stop: whether a local copy existed and was removed.
 * @property error           whether the request failed to send or the gateway reported a failure.
 * @property message         a human readable description of the outcome, suitable for a toast.
 */
export interface NearCacheOpResult {
    cacheId: string
    operationStatus?: string
    wasAlreadyWarm?: boolean
    wasCached?: boolean
    error: boolean
    message: string
}

/**
 * Information about a specific counter cache instance.
 */
export interface CounterCacheInfo {
    cacheId: number
    itemCount: number
}

/**
 * A single counter entry (key and its current numeric value).
 */
export interface CounterItem {
    key: string
    value: number
}

/**
 * The response returned from a counter operation
 * (increment, decrement or set).
 */
export interface CounterOpResponse {
    cacheId: string
    key: string
    value: number
    operationStatus: string
}

/**
 * A single pending TTL removal timer.
 *
 * @property timerType Either "CACHE" or "COUNTER", identifying which kind of
 *                     cache the entry will be removed from.
 * @property cacheId   The id of the cache (or counter cache) the entry lives in.
 * @property key       The key that is scheduled to be removed.
 * @property deadline  The epoch time (millis) at which the removal will fire.
 */
export interface TimerInfo {
    timerType: string
    cacheId: string
    key: string
    deadline: number
}

/**
 * The response from getting all pending TTL removal timers across
 * caches and counter caches.
 */
export interface GetTimersResponse {
    operationStatus: string
    timers: TimerInfo[]
}

/**
 * The result of the most recent recording-segment purge run (reclaiming disk
 * by removing old cluster log segments after a snapshot).
 */
export interface RecordingPurge {
    success: boolean
    logRecordingId: number
    purgedToPosition: number
    reclaimedBytes: number
    archiveDirBytesAfter: number
    timestamp: number
    message: string
}

/**
 * The purge outcome for a single cluster node, returned in the per-node breakdown of a
 * cluster-wide snapshot-and-purge.
 *
 * @property host           the clustertools sidecar host that was contacted.
 * @property success        whether this node's purge (and, for the first node, the snapshot) succeeded.
 * @property reclaimedBytes bytes reclaimed on disk on this node.
 * @property message        human readable detail from this node, or the failure reason.
 */
export interface NodePurgeResult {
    host: string
    success: boolean
    reclaimedBytes: number
    message: string
}

/**
 * Information about the latest cluster snapshot and the archive disk usage,
 * surfaced for display in the UI.
 *
 * @property snapshotPresent     whether a valid consensus module snapshot exists.
 * @property logPosition         the log position the latest snapshot was taken at.
 * @property termBaseLogPosition the log position at the base of the snapshot's leadership term.
 * @property timestamp           the cluster timestamp recorded for the latest snapshot (epoch millis).
 * @property recordingId         the archive recording id holding the latest snapshot.
 * @property snapshotCount       the number of valid snapshots currently retained.
 * @property archiveDirBytes     current size of the archive directory on disk, in bytes.
 * @property lastPurge           the most recent purge result, or null if none has run.
 */
export interface SnapshotInfo {
    snapshotPresent: boolean
    logPosition: number
    termBaseLogPosition: number
    timestamp: number
    recordingId: number
    snapshotCount: number
    archiveDirBytes: number
    lastPurge: RecordingPurge | null
}