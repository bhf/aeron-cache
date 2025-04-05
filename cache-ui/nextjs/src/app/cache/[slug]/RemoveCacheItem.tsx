"use client"
import {ConfirmingDialog} from "@/components/cache-actions/ConfirmingDialog";
import {getCacheAPIURI} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";

const logger = getLogger("RemoveItem")

interface RemoveCacheItemProps {
    cacheId: number
    itemKey: string
}

const headers = {
    'Accept': 'application/json',
    'Content-Type': 'application/json'
};

/**
 * A component to remove items from a cache.
 * @constructor
 */
export function RemoveCacheItem(props: RemoveCacheItemProps) {
    const sendRemoveItemRequest = async () => {
        logger.info("Called remove cache item for cache: " + props.cacheId + ", on key: " + props.itemKey);
        const cacheId = props.cacheId
        const key = props.itemKey
        logger.info("Remove item request for cache with id " + cacheId + " on key " + key)
        try {
            const rawResponse = await fetch(await getCacheAPIURI() + '/cache/' + cacheId + "/" + key, {
                    method: 'DELETE',
                    headers,
                    body: JSON.stringify({cacheId, key})
                },
            );
            const content = await rawResponse.json();
            logger.info("Got response from sending request to remove item on key " + key + ", response:" + content)
        } catch (err) {
            logger.error("Error whilst sending request to remove item ", err);
        }
    }

    return (
        <ConfirmingDialog title={"Removing On Key: " + props.itemKey}
                          message={"Are you sure you want to remove this item from cache "+props.cacheId+"?"}
                          cancelText={"Cancel"} actionText={"Continue"} action={sendRemoveItemRequest}
                          buttonText={"Remove"}/>
    );
}