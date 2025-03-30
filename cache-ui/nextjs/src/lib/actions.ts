"use server"

/**
 * React server actions.
 */

/**
 * Create a cache.
 * @param formData The form data with the params used to create the cache.
 */
export async function createCacheRequest(formData: FormData){
    const cacheId = formData.get('cacheId')
    console.log("Creating cache request with id", cacheId)
    try {
        const rawResponse = await fetch(process.env.AERON_CACHE_API + '/api/v1/cache/' + cacheId, {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({cacheId})
        });
        const content = await rawResponse.json();
        console.log("Got response from sending request to create cache ",content)
    } catch (err) {
        console.error("Error whilst sending request to create cache ",err);
    }
}