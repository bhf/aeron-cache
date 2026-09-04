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