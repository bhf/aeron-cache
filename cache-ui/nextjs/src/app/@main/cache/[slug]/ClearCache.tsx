"use client"
import {ConfirmingDialog} from "@/components/cache-actions/ConfirmingDialog";
import {getCacheAPIURI} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";
import {toast} from "sonner";
import {redirect} from "next/navigation";

interface ClearCacheProps {
    cacheId: number
}

const logger = getLogger("ClearCache")

const headers = {
    'Accept': 'application/json',
    'Content-Type': 'application/json'
};

/**
 * Raise an alert that the cache
 * couldn't be cleared.
 */
function toasterAlert() {
    let props = {title: "Error sending clear cache request", description: "Couldn't clear cache", actionLabel: "OK"};

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
    let props = {title: "Success", description: "Cleared cache", actionLabel: "OK"};

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
    let props = {title: "Error", description: "Failed to clear cache: " + reason, actionLabel: "OK"};

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

        let rawResponse
        try {
            rawResponse = await fetch(await getCacheAPIURI() + '/cache/' + cacheId, {
                    method: 'PATCH',
                    headers
                },
            )
        } catch (err) {
            logger.warn("Error whilst sending request to clear cache ", err);
            toasterAlert();
            return
        }

        const content = await rawResponse.json();
        logger.info("Got response from sending request to clear cache ", content)

        if (rawResponse.status === 200) {
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