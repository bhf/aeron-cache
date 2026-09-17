"use client"

import {useRouter} from "next/navigation";
import {ConfirmingDialog} from "@/components/cache-actions/ConfirmingDialog";
import {cancelTimerRequest} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";
import {toast} from "sonner";

const logger = getLogger("CancelTimer")

interface CancelTimerProps {
    timerType: string
    cacheId: string
    itemKey: string
}

/**
 * Raise a toast that the timer was cancelled successfully.
 */
function toastSuccess() {
    toast.success("Success", {
        description: "Cancelled timer",
        action: {label: "OK", onClick: () => logger.info("Ack alert")},
    })
}

/**
 * Raise a toast that cancelling the timer failed.
 */
function toastFailure(reason: string) {
    toast.error("Error", {
        description: "Failed to cancel timer: " + reason,
        action: {label: "OK", onClick: () => logger.info("Ack alert")},
    })
}

/**
 * A component to cancel a single pending TTL removal timer.
 * @constructor
 */
export function CancelTimer(props: CancelTimerProps) {
    const router = useRouter()

    const sendCancelTimerRequest = async () => {
        logger.info("Cancel timer (" + props.timerType + ") for cache " + props.cacheId + " on key " + props.itemKey)

        const content = await cancelTimerRequest({
            timerType: props.timerType,
            cacheId: props.cacheId,
            key: props.itemKey,
        })

        if (content && content.operationStatus === "SUCCESS") {
            toastSuccess()
            router.refresh()
        } else {
            toastFailure(content ? content.operationStatus : "no response")
        }
    }

    return (
        <ConfirmingDialog
            title={"Cancelling Timer On Key: " + props.itemKey}
            message={"Are you sure you want to cancel the scheduled removal of key " + props.itemKey + " from cache " + props.cacheId + "?"}
            cancelText={"Dismiss"}
            actionText={"Cancel Timer"}
            action={sendCancelTimerRequest}
            buttonText={"Cancel Timer"}
        />
    );
}
