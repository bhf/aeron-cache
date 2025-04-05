"use client"
import {ConfirmingDialog} from "@/components/cache-actions/ConfirmingDialog";
import {getCacheAPIURI} from "@/lib/actions";

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
        console.log("Called deleteCache for cache " + props.cacheId);
        const cacheId = props.cacheId
        console.log("Delete cache request with id", cacheId)
        try {
            const rawResponse = await fetch(await getCacheAPIURI() + '/cache/' + cacheId, {
                    method: 'DELETE',
                    headers
                },
            );
            const content = await rawResponse.json();
            console.log("Got response from sending request to delete cache ", content)
        } catch (err) {
            console.error("Error whilst sending request to delete cache ", err);
        }
    }

    return (
        <ConfirmingDialog title={"Deleting Cache: " + props.cacheId}
                          message={"Are you sure you want to delete this cache?"}
                          cancelText={"Cancel"} actionText={"Continue"} action={sendDeleteCacheRequest}
                          buttonText={"Delete"}/>
    );
}