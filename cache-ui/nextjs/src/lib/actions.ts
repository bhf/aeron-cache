"use server"

import {getLogger} from "@/lib/loggingUtil";
import {revalidatePath, revalidateTag} from "next/cache";

/**
 * React server actions.
 */

const logger = getLogger("ServerActions")

const headers = {
    'Accept': 'application/json',
    'Content-Type': 'application/json'
};

export async function getCacheAPIURI() {
    return process.env.AERON_CACHE_API
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
        const rawResponse = await fetch(process.env.AERON_CACHE_API + '/cache/', {
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

        // revalidate the endpoint from which we get all available caches
        revalidateTag("AllCaches")
        revalidatePath(await getCacheAPIURI() + "/stats")
        revalidatePath(await getCacheAPIURI() + "/caches")

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
        const rawResponse = await fetch(process.env.AERON_CACHE_API + '/cache/' + cacheId, {
                method: 'DELETE',
                headers,
                cache: "no-cache"
            },
        );
        const content = await rawResponse.json();
        logger.info("Got response from sending request to delete cache ", content)
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
        const rawResponse = await fetch(process.env.AERON_CACHE_API + '/cache/' + cacheId, {
                method: 'PATCH',
                headers,
                cache: "no-cache"
            },
        );
        const content = await rawResponse.json();
        logger.info("Got response from sending request to clear cache ", content)
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
        const rawResponse = await fetch(process.env.AERON_CACHE_API + '/cache/' + cacheId, {
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
        revalidateTag("Cache-" + cacheId)
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
        const rawResponse = await fetch(process.env.AERON_CACHE_API + '/cache/' + props.cacheId + "/" + props.key, {
                method: 'DELETE',
                headers,
                body: JSON.stringify(props),
                cache: "no-cache"
            },
        );
        const content = await rawResponse.json();
        logger.info("Got response from sending request to remove item on key " + props.key + ", response:" + content)
        revalidatePath(await getCacheAPIURI() + "/stats")
        return content
    } catch (err) {
        logger.error("Error whilst sending request to remove item ", err);
    }
}
