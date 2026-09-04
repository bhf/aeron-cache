"use client"
import {ConfirmingDialog} from "@/components/cache-actions/ConfirmingDialog";
import {removeCounterRequest} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";
import {toast} from "sonner";

const logger = getLogger("RemoveCounter")

interface RemoveCounterItemProps {
    cacheId: number
    itemKey: string
}

/**
 * Raise a toast that the action occurred successfully.
 */
function toastSuccess() {
    const props = {title: "Success", description: "Removed counter", actionLabel: "OK"};

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
    const props = {title: "Error", description: "Failed to remove counter: " + reason, actionLabel: "OK"};

    toast.error(props.title, {
        description: props.description,
        action: {
            label: props.actionLabel,
            onClick: () => console.log(""),
        },
    })
}

/**
 * A component to remove counters from a counter cache.
 * @constructor
 */
export function RemoveCounterItem(props: RemoveCounterItemProps) {
    const sendRemoveCounterRequest = async () => {
        logger.info("Called remove counter for cache: " + props.cacheId + ", on key: " + props.itemKey);
        const cacheId = props.cacheId
        const key = props.itemKey

        const content = await removeCounterRequest({cacheId, key})
        logger.info("Got response from sending request to remove counter on key " + key + ", response:" + content)

        if (content.operationStatus === "SUCCESS") {
            toastSuccess()
        } else {
            toastFailure(content.operationStatus)
        }
    }

    return (
        <ConfirmingDialog title={"Removing On Key: " + props.itemKey}
                          message={"Are you sure you want to remove this counter from cache " + props.cacheId + "?"}
                          cancelText={"Cancel"} actionText={"Continue"} action={sendRemoveCounterRequest}
                          buttonText={"Remove"}/>
    );
}
