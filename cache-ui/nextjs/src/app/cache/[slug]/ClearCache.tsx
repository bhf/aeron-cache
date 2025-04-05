"use client"
import {ConfirmingDialog} from "@/components/cache-actions/ConfirmingDialog";
import {getCacheAPIURI} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";

interface ClearCacheProps {
    cacheId: number
}

const logger = getLogger("ClearCache")

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
        logger.info("Called clearCache for cache " + props.cacheId);
        const cacheId = props.cacheId
        logger.info("Clear cache request with id ", cacheId)
        try {
            const rawResponse = await fetch(await getCacheAPIURI() + '/cache/' + cacheId, {
                    method: 'DELETE',
                    headers
                },
            );
            const content = await rawResponse.json();
            logger.info("Got response from sending request to clear cache ", content)
        } catch (err) {
            logger.error("Error whilst sending request to clear cache ", err);
        }

    }

    return (
        <ConfirmingDialog title={"Clearing Cache: " + props.cacheId}
                          message={"Are you sure you want to clear this cache?"}
                          cancelText={"Cancel"} actionText={"Continue"} action={sendClearCacheRequest}
                          buttonText={"Clear"}/>
    );
}