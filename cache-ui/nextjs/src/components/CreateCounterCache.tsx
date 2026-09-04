"use client"

import {createCounterCacheRequest} from "@/lib/actions";
import Form from "next/form";
import {Input} from "@/components/ui/input";
import {Button} from "@/components/ui/button";
import {toast} from "sonner";
import React, {useState} from "react";
import {getLogger} from "@/lib/loggingUtil";

const logger = getLogger("CreateCounterCache")

/**
 * A client side component which uses Next Forms to create a counter cache.
 * @constructor
 */
export default function CreateCounterCacheRequest() {

    const [formSubmitted, setFormSubmitted] = useState(false);
    const [hasToastShown, setHasToastShown] = useState(false);
    const initialFormState = {message: "", error: false}
    const [formState] = useState(initialFormState)

    /**
     * Raise an alert that the counter cache
     * couldn't be created.
     */
    function toasterAlert(message: string) {
        if (!hasToastShown) {
            setHasToastShown(true);
            setFormSubmitted(false);
            const props = {title: "Error Creating Counter Cache", description: message, actionLabel: "OK"};
            toast(props.title, {
                description: props.description,
                action: {
                    label: props.actionLabel,
                    onClick: () => {
                        logger.info("Ack alert")
                        setHasToastShown(false)
                    },
                },
            })
        }
        setHasToastShown(false);

    }

    /**
     * Toast success that the counter cache has been created.
     */
    function toastSuccess() {
        if (!hasToastShown) {
            setHasToastShown(true);
            setFormSubmitted(false);
            const props = {title: "Success", description: "Created counter cache", actionLabel: "OK"};
            toast.success(props.title, {
                description: props.description,
                action: {
                    label: props.actionLabel,
                    onClick: () => {
                        logger.info("Ack alert")
                        setHasToastShown(false)
                    },
                },
            })
        }
        setHasToastShown(false);

        return <></>
    }

    async function createCounterCache(formData: FormData) {
        await setHasToastShown(false);
        await setFormSubmitted(true);
        return sendRequest(formData);
    }

    async function sendRequest(formData: FormData) {
        const res = await createCounterCacheRequest(formState, formData)
        logger.info("Got response from creating counter cache: ", res)
        if (res.error) {
            logger.warn("Got error trying to create counter cache: ", res.message)
            toasterAlert(res.message)
        } else {
            toastSuccess()
        }
    }

    return (
        <Form action={createCounterCache}>
            <div className="grid gap-4">
                <div className="grid gap-2">
                    <Input
                        id="cacheId"
                        type="string"
                        placeholder="Counter Cache ID"
                        name="cacheId"
                        data-testid="create-counter-cache-input"
                        required
                    />
                </div>
                <Button type="submit" disabled={formSubmitted} data-testid="create-counter-cache-button">
                    Create Counter Cache
                </Button>
                <div className="grid gap-2">{formState.message}</div>
            </div>
        </Form>
    );
}
