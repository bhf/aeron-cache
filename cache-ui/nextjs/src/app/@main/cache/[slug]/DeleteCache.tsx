"use client"
import {ConfirmingDialog} from "@/components/cache-actions/ConfirmingDialog";
import {deleteCacheRequest, getCacheAPIURI} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";
import {toast} from "sonner";
import {redirect} from "next/navigation";

const logger = getLogger("DeleteCache")

interface DeleteCacheProps {
    cacheId: number
}

/**
 * Raise an alert that the request to delete the cache
 * couldn't be sent.
 */
function alertOnErrorSending() {
    let props = {title: "Error sending delete request", description: "Couldn't delete cache", actionLabel: "OK"};

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
    let props = {title: "Success", description: "Deleted cache", actionLabel: "OK"};

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
    let props = {title: "Error", description: "Failed to delete cache: " + reason, actionLabel: "OK"};

    toast.error(props.title, {
        description: props.description,
        action: {
            label: props.actionLabel,
            onClick: () => console.log(""),
        },
    })
}

/**
 * A component to delete a cache.
 * @constructor
 */
export function DeleteCache(props: DeleteCacheProps) {
    const sendDeleteCacheRequest = async () => {
        logger.info("Called deleteCache for cache " + props.cacheId);
        const cacheId = props.cacheId
        logger.info("Delete cache request with id", cacheId)

        const content = await deleteCacheRequest(cacheId)

        logger.info("Got response from request to delete cache ", content)

        if (content.operationStatus === "SUCCESS") {
            toastSuccess()
            redirect('/')
        } else {
            toastFailure(content.operationStatus)
        }
    }

    return (
        <ConfirmingDialog title={"Deleting Cache: " + props.cacheId}
                          message={"Are you sure you want to delete this cache?"}
                          cancelText={"Cancel"} actionText={"Continue"} action={sendDeleteCacheRequest}
                          buttonText={"Delete"}/>
    );
}