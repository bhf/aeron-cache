"use client";
import {ConfirmingDialog} from "@/components/cache-actions/ConfirmingDialog";
import {deleteCounterCacheRequest} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";
import {toast} from "sonner";
import {redirect} from "next/navigation";

const logger = getLogger("DeleteCounterCache");

interface DeleteCounterCacheProps {
    cacheId: string;
}

/**
 * Raise a toast that the action occurred successfully.
 */
function toastSuccess() {
    const props = {title: "Success", description: "Deleted counter cache", actionLabel: "OK"};

    toast.success(props.title, {
        description: props.description,
        action: {label: props.actionLabel, onClick: () => console.log("")},
    });
}

/**
 * Raise a toast that the action failed.
 */
function toastFailure(reason: string) {
    const props = {title: "Error", description: "Failed to delete counter cache: " + reason, actionLabel: "OK"};

    toast.error(props.title, {
        description: props.description,
        action: {label: props.actionLabel, onClick: () => console.log("")},
    });
}

/**
 * A component to delete a counter cache.
 * @constructor
 */
export function DeleteCounterCache(props: DeleteCounterCacheProps) {
    const sendDeleteCounterCacheRequest = async () => {
        logger.info("Called deleteCounterCache for cache " + props.cacheId);
        const content = await deleteCounterCacheRequest(props.cacheId);
        logger.info("Got response from request to delete counter cache ", content);

        if (content.operationStatus === "SUCCESS") {
            toastSuccess();
            redirect("/");
        } else {
            toastFailure(content.operationStatus);
        }
    };

    return (
        <ConfirmingDialog
            title={"Deleting Counter Cache: " + props.cacheId}
            message={"Are you sure you want to delete this counter cache?"}
            cancelText={"Cancel"}
            actionText={"Continue"}
            action={sendDeleteCounterCacheRequest}
            buttonText={"Delete"}
        />
    );
}
