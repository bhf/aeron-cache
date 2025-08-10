"use client"
import {ConfirmingDialog} from "@/components/cache-actions/ConfirmingDialog";
import {clearCacheRequest} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";
import {toast} from "sonner";
import {redirect} from "next/navigation";

interface ClearCacheProps {
    cacheId: string
}

const logger = getLogger("ClearCache")


/**
 * Raise an alert that the cache
 * couldn't be cleared.
 */
function toasterAlert() {
    const props = {title: "Error sending clear cache request", description: "Couldn't clear cache", actionLabel: "OK"};

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
    const props = {title: "Success", description: "Cleared cache", actionLabel: "OK"};

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
    const props = {title: "Error", description: "Failed to clear cache: " + reason, actionLabel: "OK"};

    toast.error(props.title, {
        description: props.description,
        action: {
            label: props.actionLabel,
            onClick: () => console.log(""),
        },
    })
}

/**
 * A component to clear a cache.
 * @constructor
 */
export function ClearCache(props: ClearCacheProps) {
    const sendClearCacheRequest = async () => {
        logger.info("Called clearCache for cache " + props.cacheId);
        const cacheId = props.cacheId
        logger.info("Clear cache request with id ", cacheId)

        const content = await clearCacheRequest(cacheId)
        logger.info("Got response from sending request to clear cache ", content)

        if (content.operationStatus === "SUCCESS") {
            toastSuccess()
            redirect("/cache/"+props.cacheId)
        } else {
            toastFailure(content.operationStatus)
        }
    }

    return (
        <ConfirmingDialog title={"Clearing Cache: " + props.cacheId}
                          message={"Are you sure you want to clear this cache?"}
                          cancelText={"Cancel"} actionText={"Continue"} action={sendClearCacheRequest}
                          buttonText={"Clear"}/>
    );
}