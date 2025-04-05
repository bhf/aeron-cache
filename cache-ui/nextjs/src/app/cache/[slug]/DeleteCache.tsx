"use client"
import {ConfirmingDialog} from "@/components/cache-actions/ConfirmingDialog";
import {getCacheAPIURI} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";

const logger = getLogger("DeleteCache")

interface DeleteCacheProps {
    cacheId: number
}

const headers = {
    'Accept': 'application/json',
    'Content-Type': 'application/json'
};

/**
 * A component to delete a cache.
 * @constructor
 */
export function DeleteCache(props: DeleteCacheProps) {
    const sendDeleteCacheRequest = async () => {
        logger.info("Called deleteCache for cache " + props.cacheId);
        const cacheId = props.cacheId
        logger.info("Delete cache request with id", cacheId)
        try {
            const rawResponse = await fetch(await getCacheAPIURI() + '/cache/' + cacheId, {
                    method: 'DELETE',
                    headers
                },
            );
            const content = await rawResponse.json();
            logger.info("Got response from sending request to delete cache ", content)
        } catch (err) {
            logger.error("Error whilst sending request to delete cache ", err);
        }
    }

    return (
        <ConfirmingDialog title={"Deleting Cache: " + props.cacheId}
                          message={"Are you sure you want to delete this cache?"}
                          cancelText={"Cancel"} actionText={"Continue"} action={sendDeleteCacheRequest}
                          buttonText={"Delete"}/>
    );
}