"use client"
import {ConfirmingDialog} from "@/components/cache-actions/ConfirmingDialog";
import {getCacheAPIURI} from "@/lib/actions";

interface ClearCacheProps {
    cacheId: number
}

const headers = {
    'Accept': 'application/json',
    'Content-Type': 'application/json'
};

/**
 * A component to clear a cache.
 * @constructor
 */
export function ClearCache(props: ClearCacheProps) {
    const sendClearCacheRequest = async () => {
        console.log("Called clearCache for cache " + props.cacheId);
        const cacheId = props.cacheId
        console.log("Clear cache request with id ", cacheId)
        try {
            const rawResponse = await fetch(await getCacheAPIURI() + '/cache/' + cacheId, {
                    method: 'DELETE',
                    headers
                },
            );
            const content = await rawResponse.json();
            console.log("Got response from sending request to clear cache ", content)
        } catch (err) {
            console.error("Error whilst sending request to clear cache ", err);
        }

    }

    return (
        <ConfirmingDialog title={"Clearing Cache: " + props.cacheId}
                          message={"Are you sure you want to clear this cache?"}
                          cancelText={"Cancel"} actionText={"Continue"} action={sendClearCacheRequest}
                          buttonText={"Clear"}/>
    );
}