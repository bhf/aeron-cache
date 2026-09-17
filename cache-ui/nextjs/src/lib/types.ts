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