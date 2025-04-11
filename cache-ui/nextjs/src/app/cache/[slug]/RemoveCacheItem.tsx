"use client"
import {ConfirmingDialog} from "@/components/cache-actions/ConfirmingDialog";
import {getCacheAPIURI} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";
import {toast} from "sonner";
import {redirect} from "next/navigation";

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
 * Raise an alert that the item
 * couldn't be removed.
 */
function toasterAlert() {
    let props = {title: "Error sending remove item request", description: "Couldn't remove item", actionLabel: "OK"};

    toast(props.title, {
        description: props.description,
        action: {
            label: props.actionLabel,
            onClick: () => console.log("Undo"),
        },
    })
}

/**
 * Raise a toast that the action occurred successfully.
 */
function toastSuccess() {
    let props = {title: "Success", description: "Removed item", actionLabel: "OK"};

    toast.success(props.title, {
        description: props.description,
        action: {
            label: props.actionLabel,
            onClick: () => console.log(""),
        },
    })
}

/**
 * Raise a toast that the action failed.
 */
function toastFailure(reason: string) {
    let props = {title: "Error", description: "Failed to remove item: " + reason, actionLabel: "OK"};

    toast.error(props.title, {
        description: props.description,
        action: {
            label: props.actionLabel,
            onClick: () => console.log(""),
        },
    })
}

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

        let rawResponse;
        try {
            rawResponse = await fetch(await getCacheAPIURI() + '/cache/' + cacheId + "/" + key, {
                    method: 'DELETE',
                    headers,
                    body: JSON.stringify({cacheId, key})
                },
            );
        } catch (e) {
            logger.warn("Failed to send request to remove cache item", e)
            toasterAlert()
            return
        }

        const content = await rawResponse.json();
        logger.info("Got response from sending request to remove item on key " + key + ", response:" + content)

        if (rawResponse.status === 200) {
            toastSuccess()
        } else {
            toastFailure(content.operationStatus)
        }
    }

    return (
        <ConfirmingDialog title={"Removing On Key: " + props.itemKey}
                          message={"Are you sure you want to remove this item from cache " + props.cacheId + "?"}
                          cancelText={"Cancel"} actionText={"Continue"} action={sendRemoveItemRequest}
                          buttonText={"Remove"}/>
    );
}