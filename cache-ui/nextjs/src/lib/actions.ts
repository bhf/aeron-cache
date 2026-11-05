"use server"

import {getLogger} from "@/lib/loggingUtil";
import {revalidatePath, updateTag} from "next/cache";
import type {NearCacheGateway, NearCacheKind, NearCacheOpResult, NodePurgeResult, WarmCacheInfo} from "@/lib/types";

/**
 * React server actions.
 */

const logger = getLogger("ServerActions")

const headers = {
    'Accept': 'application/json',
    'Content-Type': 'application/json'
};

const AERON_CACHE_API = process.env.AERON_CACHE_API ? process.env.AERON_CACHE_API : "http://localhost:7070";

export async function getCacheAPIURI() {
    return AERON_CACHE_API
}

export async function getJaegerURL() {
    return process.env.JAEGER ? process.env.JAEGER : "http://localhost:5000";
}

export async function getPrometheusURL() {
    return process.env.PROMETHEUS ? process.env.PROMETHEUS : "http://localhost:5000";
}

export async function getWebsocketURL() {
    return process.env.AERON_CACHE_WS_API ? process.env.AERON_CACHE_WS_API : "ws:localhost:7071";
}

export async function getSSEURL(): Promise<string> {
    return process.env.AERON_CACHE_SSE_API ? process.env.AERON_CACHE_SSE_API : "http://localhost:7072";
}

/**
 * The near cache management API base URL for each gateway, e.g. {@code http://host:7070/api/v1/near}.
 *
 * Every gateway (http, ws, sse and the Aeron transport gateway) exposes the same near cache management
 * REST surface on its own HTTP port, so a near cache operation must say which gateway it targets. These
 * are resolved from env vars, falling back to the local docker-compose ports. The Aeron gateway keeps
 * back compatibility with the older {@code AERON_CACHE_NEAR_API} variable.
 */
const NEAR_API_DEFAULTS: Record<NearCacheGateway, string> = {
    http: "http://localhost:7070/api/v1/near",
    ws: "http://localhost:7071/api/v1/near",
    sse: "http://localhost:7072/api/v1/near",
    aeron: "http://localhost:7073/api/v1/near",
};

/**
 * Resolve the near cache management API base URL for a gateway.
 * @param gateway The gateway whose near cache is being managed.
 */
function nearApiBase(gateway: NearCacheGateway): string {
    switch (gateway) {
        case "http":
            return process.env.AERON_CACHE_HTTP_NEAR_API ?? NEAR_API_DEFAULTS.http;
        case "ws":
            return process.env.AERON_CACHE_WS_NEAR_API ?? NEAR_API_DEFAULTS.ws;
        case "sse":
            return process.env.AERON_CACHE_SSE_NEAR_API ?? NEAR_API_DEFAULTS.sse;
        case "aeron":
            return process.env.AERON_CACHE_AERON_NEAR_API ?? process.env.AERON_CACHE_NEAR_API ?? NEAR_API_DEFAULTS.aeron;
    }
}

/** Human readable gateway names for toasts and labels. */
const GATEWAY_LABELS: Record<NearCacheGateway, string> = {
    http: "HTTP",
    ws: "WebSocket",
    sse: "SSE",
    aeron: "Aeron",
};

export async function getGatewayLabel(gateway: NearCacheGateway): Promise<string> {
    return GATEWAY_LABELS[gateway];
}

/**
 * The URL path segment for listing warm near caches of a given kind.
 * Caches live under {@code /caches}; counter caches under {@code /counters-caches}.
 */
function warmListPath(kind: NearCacheKind): string {
    return kind === "counters" ? "/counters-caches" : "/caches";
}

/**
 * The URL path prefix for priming/stopping a single near cache of a given kind.
 * Caches use {@code /cache/<id>}; counter caches use {@code /counters/<id>}.
 */
function singleCachePath(kind: NearCacheKind, cacheId: string): string {
    const prefix = kind === "counters" ? "/counters/" : "/cache/";
    return prefix + encodeURIComponent(cacheId);
}

/**
 * List the caches currently held warm in a gateway's near cache.
 *
 * Tolerant by design: a gateway with the near cache disabled (no near routes) or that is unreachable
 * yields an empty list rather than throwing, so the management view degrades gracefully.
 *
 * @param gateway The gateway to query.
 * @param kind    Regular caches or counter caches.
 */
export async function listWarmNearCaches(gateway: NearCacheGateway, kind: NearCacheKind): Promise<WarmCacheInfo[]> {
    const url = nearApiBase(gateway) + warmListPath(kind);
    logger.info("Listing warm near caches from " + url);
    try {
        const rawResponse = await fetch(url, {method: 'GET', headers, cache: "no-cache"});
        if (!rawResponse.ok) {
            logger.warn("Non-OK listing warm near caches from " + url + ": " + rawResponse.status);
            return [];
        }
        const content = await rawResponse.json();
        if (!Array.isArray(content)) {
            return [];
        }
        return content.map((c) => ({cacheId: String(c.cacheId), itemCount: Number(c.itemCount ?? 0)}));
    } catch (err) {
        logger.warn("Error whilst listing warm near caches from " + url, err);
        return [];
    }
}

/**
 * Prime a cache into a gateway's near cache: subscribe so the local copy is warmed up front. Idempotent -
 * priming an already warm cache succeeds and reports {@code wasAlreadyWarm}.
 *
 * @param gateway The gateway whose near cache to prime.
 * @param kind    Regular caches or counter caches.
 * @param cacheId The cache to prime.
 */
export async function primeNearCache(gateway: NearCacheGateway, kind: NearCacheKind, cacheId: string): Promise<NearCacheOpResult> {
    const trimmed = (cacheId ?? "").trim();
    if (trimmed.length === 0) {
        return {cacheId: trimmed, error: true, message: "Enter a cache ID to prime"};
    }
    const url = nearApiBase(gateway) + singleCachePath(kind, trimmed);
    logger.info("Priming near cache via " + url);
    try {
        const rawResponse = await fetch(url, {method: 'POST', headers, cache: "no-cache"});
        const content = await rawResponse.json().catch(() => null);
        if (!rawResponse.ok || content?.operationStatus !== "SUCCESS") {
            return {
                cacheId: trimmed,
                operationStatus: content?.operationStatus,
                error: true,
                message: "Failed to prime " + trimmed + (content?.operationStatus ? ": " + content.operationStatus : ""),
            };
        }
        const wasAlreadyWarm = Boolean(content.wasAlreadyWarm);
        return {
            cacheId: trimmed,
            operationStatus: content.operationStatus,
            wasAlreadyWarm,
            error: false,
            message: wasAlreadyWarm ? trimmed + " was already warm" : "Priming " + trimmed + " started",
        };
    } catch (err) {
        logger.error("Error whilst priming near cache via " + url, err);
        return {cacheId: trimmed, error: true, message: "Error priming " + trimmed};
    }
}

/**
 * Stop near caching a cache on a gateway: drop the subscription and remove the local copy. Idempotent -
 * stopping a cache that is not warm succeeds and reports {@code wasCached=false}.
 *
 * @param gateway The gateway whose near cache to stop.
 * @param kind    Regular caches or counter caches.
 * @param cacheId The cache to stop near caching.
 */
export async function stopNearCache(gateway: NearCacheGateway, kind: NearCacheKind, cacheId: string): Promise<NearCacheOpResult> {
    const trimmed = (cacheId ?? "").trim();
    const url = nearApiBase(gateway) + singleCachePath(kind, trimmed);
    logger.info("Stopping near cache via " + url);
    try {
        const rawResponse = await fetch(url, {method: 'DELETE', headers, cache: "no-cache"});
        const content = await rawResponse.json().catch(() => null);
        if (!rawResponse.ok || content?.operationStatus !== "SUCCESS") {
            return {
                cacheId: trimmed,
                operationStatus: content?.operationStatus,
                error: true,
                message: "Failed to stop " + trimmed + (content?.operationStatus ? ": " + content.operationStatus : ""),
            };
        }
        const wasCached = Boolean(content.wasCached);
        return {
            cacheId: trimmed,
            operationStatus: content.operationStatus,
            wasCached,
            error: false,
            message: wasCached ? "Stopped near caching " + trimmed : trimmed + " was not warm",
        };
    } catch (err) {
        logger.error("Error whilst stopping near cache via " + url, err);
        return {cacheId: trimmed, error: true, message: "Error stopping " + trimmed};
    }
}

export async function isUrlAccessible(url: string): Promise<boolean> {
    try {
        const response = await fetch(url, { method: 'HEAD', next: { revalidate: 3600 } });
        return response.ok;
    } catch (e) {
        return false;
    }
}

/**
 * Create a cache.
 * @param currentState
 * @param formData The form data with the params used to create the cache.
 */
export async function createCacheRequest(currentState: { message: string, error: boolean }, formData: FormData) {
    const cacheId = formData.get('cacheId')

    const specialCharacterCheck = /[ `!@#$%^&*()_+\=\[\]{};':"\\|,.<>\/?~]/
    if (specialCharacterCheck.test(cacheId as string)) {
        return {message: "No special characters allowed in cache ID", error: true}
    }

    logger.info("Creating cache request with id", cacheId)
    try {
        const rawResponse = await fetch(AERON_CACHE_API + '/cache/', {
            method: 'POST',
            headers,
            body: JSON.stringify({cacheId}),
            cache: "no-cache"
        })
        const content = await rawResponse.json();
        logger.info("Got response from sending request to create cache ", content)

        if (rawResponse.status != 200) {
            return {message: "Problem creating cache: " + content.errorMsg, error: true};
        }

        revalidatePath("/")

        return {message: "Successfully created cache", error: false};
    } catch (err) {
        logger.error("Error whilst sending request to create cache ", err);
        return {message: "Error creating cache", error: true};
    }
}

/**
 * Delete a cache.
 * @param cacheId
 */
export async function deleteCacheRequest(cacheId: string) {
    logger.info("Delete cache request with id", cacheId)
    try {
        const rawResponse = await fetch(AERON_CACHE_API + '/cache/' + cacheId, {
                method: 'DELETE',
                headers,
                cache: "no-cache"
            },
        );
        const content = await rawResponse.json();
        logger.info("Got response from sending request to delete cache ", content)
        updateTag("Cache-" + cacheId)
        revalidatePath(await getCacheAPIURI() + "/stats")
        revalidatePath(await getCacheAPIURI() + "/caches")
        return content
    } catch (err) {
        logger.error("Error whilst sending request to delete cache ", err);
    }
}

/**
 * Clear a cache.
 * @param cacheId
 */
export async function clearCacheRequest(cacheId: string) {
    logger.info("Clear cache request with id", cacheId)
    try {
        const rawResponse = await fetch(AERON_CACHE_API + '/cache/' + cacheId, {
                method: 'PATCH',
                headers,
                cache: "no-cache"
            },
        );
        const content = await rawResponse.json();
        logger.info("Got response from sending request to clear cache ", content)
        updateTag("Cache-" + cacheId)
        revalidatePath(await getCacheAPIURI() + "/stats")
        return content
    } catch (err) {
        logger.error("Error whilst sending request to clear cache ", err);
    }
}

/**
 * Add an item to the cache.
 * @param formState
 * @param formData The form data with the params used to add the item to the cache.
 */
export async function addItemToCacheRequest(formState: { message: string; error: boolean }, formData: FormData) {
    const cacheId = formData.get('cacheId')
    const key = formData.get('key')
    const value = formData.get('value')
    const rawTtl = formData.get('ttl')
    const ttlUnit = formData.get('ttlUnit')

    // Convert the TTL to milliseconds based on the selected unit. The backend
    // expects the ttl in milliseconds on its timed add-item endpoint.
    const unitToMillis: Record<string, number> = {ms: 1, s: 1000, m: 60000}
    const ttlValue = rawTtl ? Number(rawTtl) : 0
    const ttl = ttlValue > 0 ? ttlValue * (unitToMillis[ttlUnit as string] ?? 1) : 0

    logger.info("Add item request for cache with id: " + cacheId + ", on key: " + key + " with value: " + value + ", ttl(ms): " + ttl)
    try {
        // Use the timed endpoint when a TTL has been specified, otherwise the item never expires.
        const url = ttl > 0
            ? AERON_CACHE_API + '/cache/timed/' + cacheId
            : AERON_CACHE_API + '/cache/' + cacheId
        const body = ttl > 0
            ? JSON.stringify({key, value, ttl})
            : JSON.stringify({key, value})
        const rawResponse = await fetch(url, {
                method: 'POST',
                headers,
                body,
                cache: "no-cache"
            },
        );
        const content = await rawResponse.json();
        logger.info("Got response from sending request to add item:", content)

        if (rawResponse.status != 200) {
            return {message: "Problem adding item to cache: " + content.operationStatus, error: true};
        }

        // revalidate the endpoint from which we get this cache's data
        updateTag("Cache-" + cacheId)
        revalidatePath(await getCacheAPIURI() + "/stats")

        return {message: "Successfully added item", error: false};
    } catch (err) {
        logger.error("Error whilst sending request to add item ", err);
        return {message: "Error trying to add item", error: true};
    }
}

/**
 * Remove an item from the cache.
 * @param props
 */
export async function removeItemFromCacheRequest(props: { cacheId: number, key: string }) {
    logger.info("Remove item request for cache with id " + props.cacheId + "on key " + props.key)
    try {
        const rawResponse = await fetch(AERON_CACHE_API + '/cache/' + props.cacheId + "/" + props.key, {
                method: 'DELETE',
                headers,
                body: JSON.stringify(props),
                cache: "no-cache"
            },
        );
        const content = await rawResponse.json();
        logger.info("Got response from sending request to remove item on key " + props.key + ", response:" + content)
        updateTag("Cache-" + props.cacheId)
        revalidatePath(await getCacheAPIURI() + "/stats")
        return content
    } catch (err) {
        logger.error("Error whilst sending request to remove item ", err);
    }
}

/**
 * Create a counter cache.
 * @param currentState
 * @param formData The form data with the params used to create the counter cache.
 */
export async function createCounterCacheRequest(currentState: { message: string, error: boolean }, formData: FormData) {
    const cacheId = formData.get('cacheId')

    const specialCharacterCheck = /[ `!@#$%^&*()_+\=\[\]{};':"\\|,.<>\/?~]/
    if (specialCharacterCheck.test(cacheId as string)) {
        return {message: "No special characters allowed in cache ID", error: true}
    }

    logger.info("Creating counter cache request with id", cacheId)
    try {
        const rawResponse = await fetch(AERON_CACHE_API + '/counters/', {
            method: 'POST',
            headers,
            body: JSON.stringify({cacheId}),
            cache: "no-cache"
        })
        const content = await rawResponse.json();
        logger.info("Got response from sending request to create counter cache ", content)

        if (rawResponse.status != 200) {
            return {message: "Problem creating counter cache: " + content.errorMsg, error: true};
        }

        revalidatePath("/")

        return {message: "Successfully created counter cache", error: false};
    } catch (err) {
        logger.error("Error whilst sending request to create counter cache ", err);
        return {message: "Error creating counter cache", error: true};
    }
}

/**
 * Delete a counter cache.
 * @param cacheId
 */
export async function deleteCounterCacheRequest(cacheId: string) {
    logger.info("Delete counter cache request with id", cacheId)
    try {
        const rawResponse = await fetch(AERON_CACHE_API + '/counters/' + cacheId, {
                method: 'DELETE',
                headers,
                cache: "no-cache"
            },
        );
        const content = await rawResponse.json();
        logger.info("Got response from sending request to delete counter cache ", content)
        updateTag("Counters-" + cacheId)
        revalidatePath(await getCacheAPIURI() + "/counters-stats")
        revalidatePath(await getCacheAPIURI() + "/counters-caches")
        return content
    } catch (err) {
        logger.error("Error whilst sending request to delete counter cache ", err);
    }
}

/**
 * Clear a counter cache.
 * @param cacheId
 */
export async function clearCounterCacheRequest(cacheId: string) {
    logger.info("Clear counter cache request with id", cacheId)
    try {
        const rawResponse = await fetch(AERON_CACHE_API + '/counters/' + cacheId, {
                method: 'PATCH',
                headers,
                cache: "no-cache"
            },
        );
        const content = await rawResponse.json();
        logger.info("Got response from sending request to clear counter cache ", content)
        updateTag("Counters-" + cacheId)
        revalidatePath(await getCacheAPIURI() + "/counters-stats")
        return content
    } catch (err) {
        logger.error("Error whilst sending request to clear counter cache ", err);
    }
}

/**
 * Add (create) a counter with an initial value.
 * @param formState
 * @param formData The form data with the params used to add the counter to the cache.
 */
export async function addCounterRequest(formState: { message: string; error: boolean }, formData: FormData) {
    const cacheId = formData.get('cacheId')
    const key = formData.get('key')
    const value = Number(formData.get('value'))
    logger.info("Add counter request for cache with id: " + cacheId + ", on key: " + key + " with value: " + value)
    try {
        const rawResponse = await fetch(AERON_CACHE_API + '/counters/' + cacheId, {
                method: 'POST',
                headers,
                body: JSON.stringify({key, value}),
                cache: "no-cache"
            },
        );
        const content = await rawResponse.json();
        logger.info("Got response from sending request to add counter:", content)

        if (rawResponse.status != 200) {
            return {message: "Problem adding counter to cache: " + content.operationStatus, error: true};
        }

        updateTag("Counters-" + cacheId)
        revalidatePath(await getCacheAPIURI() + "/counters-stats")

        return {message: "Successfully added counter", error: false};
    } catch (err) {
        logger.error("Error whilst sending request to add counter ", err);
        return {message: "Error trying to add counter", error: true};
    }
}

/**
 * Remove a counter from a counter cache.
 * @param props
 */
export async function removeCounterRequest(props: { cacheId: number, key: string }) {
    logger.info("Remove counter request for cache with id " + props.cacheId + " on key " + props.key)
    try {
        const rawResponse = await fetch(AERON_CACHE_API + '/counters/' + props.cacheId + "/" + props.key, {
                method: 'DELETE',
                headers,
                cache: "no-cache"
            },
        );
        const content = await rawResponse.json();
        logger.info("Got response from sending request to remove counter on key " + props.key + ", response:" + content)
        updateTag("Counters-" + props.cacheId)
        revalidatePath(await getCacheAPIURI() + "/counters-stats")
        return content
    } catch (err) {
        logger.error("Error whilst sending request to remove counter ", err);
    }
}

/**
 * Increment a counter by the supplied amount.
 * @param props
 */
export async function incrementCounterRequest(props: { cacheId: number, key: string, amount: number }) {
    logger.info("Increment counter request for cache " + props.cacheId + " on key " + props.key + " by " + props.amount)
    try {
        const rawResponse = await fetch(AERON_CACHE_API + '/counters/increment/' + props.cacheId, {
                method: 'POST',
                headers,
                body: JSON.stringify({key: props.key, amount: props.amount}),
                cache: "no-cache"
            },
        );
        const content = await rawResponse.json();
        logger.info("Got response from sending request to increment counter ", content)
        updateTag("Counters-" + props.cacheId)
        revalidatePath(await getCacheAPIURI() + "/counters-stats")
        return content
    } catch (err) {
        logger.error("Error whilst sending request to increment counter ", err);
    }
}

/**
 * Decrement a counter by the supplied amount.
 * @param props
 */
export async function decrementCounterRequest(props: { cacheId: number, key: string, amount: number }) {
    logger.info("Decrement counter request for cache " + props.cacheId + " on key " + props.key + " by " + props.amount)
    try {
        const rawResponse = await fetch(AERON_CACHE_API + '/counters/decrement/' + props.cacheId, {
                method: 'POST',
                headers,
                body: JSON.stringify({key: props.key, amount: props.amount}),
                cache: "no-cache"
            },
        );
        const content = await rawResponse.json();
        logger.info("Got response from sending request to decrement counter ", content)
        updateTag("Counters-" + props.cacheId)
        revalidatePath(await getCacheAPIURI() + "/counters-stats")
        return content
    } catch (err) {
        logger.error("Error whilst sending request to decrement counter ", err);
    }
}

/**
 * Set a counter to the supplied value.
 * @param props
 */
export async function setCounterRequest(props: { cacheId: number, key: string, value: number }) {
    logger.info("Set counter request for cache " + props.cacheId + " on key " + props.key + " to " + props.value)
    try {
        const rawResponse = await fetch(AERON_CACHE_API + '/counters/set/' + props.cacheId, {
                method: 'POST',
                headers,
                body: JSON.stringify({key: props.key, value: props.value}),
                cache: "no-cache"
            },
        );
        const content = await rawResponse.json();
        logger.info("Got response from sending request to set counter ", content)
        updateTag("Counters-" + props.cacheId)
        revalidatePath(await getCacheAPIURI() + "/counters-stats")
        return content
    } catch (err) {
        logger.error("Error whilst sending request to set counter ", err);
    }
}

/**
 * Cancel a pending TTL removal timer.
 *
 * A timer represents a scheduled removal of a single key from a cache or
 * counter cache. Cancelling it maps to the backend's cancel-removal endpoint,
 * choosing the cache or counters route based on the timer's type.
 * @param props The timer to cancel.
 */
export async function cancelTimerRequest(props: { timerType: string, cacheId: string, key: string }) {
    const prefix = props.timerType === "COUNTER" ? "/counters/" : "/cache/"
    const url = AERON_CACHE_API + prefix + props.cacheId + "/" + encodeURIComponent(props.key) + "/cancel-removal"
    logger.info("Cancel timer request (" + props.timerType + ") for cache " + props.cacheId + " on key " + props.key)
    try {
        const rawResponse = await fetch(url, {
            method: 'POST',
            headers,
            cache: "no-cache"
        });
        const content = await rawResponse.json();
        logger.info("Got response from sending request to cancel timer ", content)
        updateTag("Cache-" + props.cacheId)
        updateTag("Counters-" + props.cacheId)
        revalidatePath("/")
        return content
    } catch (err) {
        logger.error("Error whilst sending request to cancel timer ", err);
    }
}

/**
 * Request a snapshot.
 */
export async function snapshotCacheRequest() {
    logger.info("Snapshot request")
    try {
        const rawResponse = await fetch(AERON_CACHE_API + '/snapshot', {
            method: 'POST',
            headers,
            cache: "no-cache"
        });

        if (rawResponse.status != 200) {
            return {message: "Problem requesting snapshot", error: true};
        }

        return {message: "Successfully requested snapshot", error: false};
    } catch (err) {
        logger.error("Error whilst requesting snapshot ", err);
        return {message: "Error requesting snapshot", error: true};
    }
}

/**
 * Trigger a snapshot and then purge old log recording segments to reclaim disk, in one request.
 * Returns the purge result message and the bytes reclaimed when successful.
 */
export async function snapshotAndPurgeRequest() {
    logger.info("Snapshot and purge request")
    try {
        const rawResponse = await fetch(AERON_CACHE_API + '/snapshot-and-purge', {
            method: 'POST',
            headers,
            cache: "no-cache"
        });

        const body = await rawResponse.json().catch(() => null);
        const nodes = body?.nodes as NodePurgeResult[] | undefined;

        if (rawResponse.status != 200 || (body && body.success === false)) {
            return {
                message: body?.message ?? "Problem requesting snapshot and purge",
                reclaimedBytes: body?.reclaimedBytes as number | undefined,
                nodes,
                error: true
            };
        }

        return {
            message: body?.message ?? "Successfully requested snapshot and purge",
            reclaimedBytes: body?.reclaimedBytes as number | undefined,
            nodes,
            error: false
        };
    } catch (err) {
        logger.error("Error whilst requesting snapshot and purge ", err);
        return {message: "Error requesting snapshot and purge", error: true};
    }
}
