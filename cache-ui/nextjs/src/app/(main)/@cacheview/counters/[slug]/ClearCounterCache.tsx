"use client"
import {ConfirmingDialog} from "@/components/cache-actions/ConfirmingDialog";
import {clearCounterCacheRequest} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";
import {toast} from "sonner";
import {redirect} from "next/navigation";

interface ClearCounterCacheProps {
    cacheId: string
}

const logger = getLogger("ClearCounterCache")

/**
 * Raise a toast that the action occurred successfully.
 */
function toastSuccess() {
    const props = {title: "Success", description: "Cleared counter cache", actionLabel: "OK"};

    toast.success(props.title, {
        description: props.description,
        action: {label: props.actionLabel, onClick: () => console.log("")},
    })
}

/**
 * Raise a toast that the action failed.
 */
function toastFailure(reason: string) {
    const props = {title: "Error", description: "Failed to clear counter cache: " + reason, actionLabel: "OK"};

    toast.error(props.title, {
        description: props.description,
        action: {label: props.actionLabel, onClick: () => console.log("")},
    })
}

/**
 * A component to clear a counter cache.
 * @constructor
 */
export function ClearCounterCache(props: ClearCounterCacheProps) {
    const sendClearCounterCacheRequest = async () => {
        logger.info("Called clearCounterCache for cache " + props.cacheId);
        const content = await clearCounterCacheRequest(props.cacheId)
        logger.info("Got response from sending request to clear counter cache ", content)

        if (content.operationStatus === "SUCCESS") {
            toastSuccess()
            redirect("/counters/" + props.cacheId)
        } else {
            toastFailure(content.operationStatus)
        }
    }

    return (
        <ConfirmingDialog title={"Clearing Counter Cache: " + props.cacheId}
                          message={"Are you sure you want to clear this counter cache?"}
                          cancelText={"Cancel"} actionText={"Continue"} action={sendClearCounterCacheRequest}
                          buttonText={"Clear"}/>
    );
}
