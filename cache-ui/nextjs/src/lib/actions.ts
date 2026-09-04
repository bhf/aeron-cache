"use server"

import {getLogger} from "@/lib/loggingUtil";
import {revalidatePath, updateTag} from "next/cache";

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
    logger.info("Add item request for cache with id: " + cacheId + ", on key: " + key + " with value: " + value)
    try {
        const rawResponse = await fetch(AERON_CACHE_API + '/cache/' + cacheId, {
                method: 'POST',
                headers,
                body: JSON.stringify({key, value}),
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
