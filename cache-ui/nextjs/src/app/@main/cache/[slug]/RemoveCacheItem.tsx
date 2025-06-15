"use client"
import {ConfirmingDialog} from "@/components/cache-actions/ConfirmingDialog";
import {removeItemFromCacheRequest} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";
import {toast} from "sonner";

const logger = getLogger("RemoveItem")

interface RemoveCacheItemProps {
    cacheId: number
    itemKey: string
}

/**
 * Raise an alert that the item
 * couldn't be removed.
 */
function toasterAlert() {
    const props = {title: "Error sending remove item request", description: "Couldn't remove item", actionLabel: "OK"};

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
    const props = {title: "Success", description: "Removed item", actionLabel: "OK"};

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
    const props = {title: "Error", description: "Failed to remove item: " + reason, actionLabel: "OK"};

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

        const content = await removeItemFromCacheRequest({cacheId, key})
        logger.info("Got response from sending request to remove item on key " + key + ", response:" + content)

        if (content.operationStatus === "SUCCESS") {
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