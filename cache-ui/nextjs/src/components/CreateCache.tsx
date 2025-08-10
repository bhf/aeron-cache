"use client"

import {createCacheRequest} from "@/lib/actions";
import Form from "next/form";
import {Input} from "@/components/ui/input";
import {Button} from "@/components/ui/button";
import {toast} from "sonner";
import React, {useState} from "react";
import {getLogger} from "@/lib/loggingUtil";

const logger = getLogger("CreateCache")

/**
 * A client side component which uses Next Forms to create a cache.
 * @constructor
 */
export default function CreateCacheRequest() {

    const [formSubmitted, setFormSubmitted] = useState(false);
    const [hasToastShown, setHasToastShown] = useState(false);
    const initialFormState = {message: "", error: false}
    const [formState] = useState(initialFormState)

    /**
     * Raise an alert that the cache
     * couldn't be created.
     */
    function toasterAlert(message: string) {
        if (!hasToastShown) {
            setHasToastShown(true);
            setFormSubmitted(false);
            const props = {title: "Error Creating Cache", description: message, actionLabel: "OK"};
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
     * Toast success that the cache has been created.
     */
    function toastSuccess() {
        if (!hasToastShown) {
            setHasToastShown(true);
            setFormSubmitted(false);
            const props = {title: "Success", description: "Created cache", actionLabel: "OK"};
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

    async function createCache(formData: FormData) {
        await setHasToastShown(false);
        await setFormSubmitted(true);
        return sendRequest(formData);
    }

    async function sendRequest(formData: FormData) {
        const res = await createCacheRequest(formState, formData)
        logger.info("Got response from creating cache: ", res)
        if(res.error){
            logger.warn("Got error trying to create cache: ", res.message)
            toasterAlert(res.message)
        }
        else{
            toastSuccess()
        }
    }

    return (
        <Form action={createCache}>
            <div className="grid gap-4">
                <div className="grid gap-2">
                    <Input
                        id="cacheId"
                        type="string"
                        placeholder="Cache ID"
                        name="cacheId"
                        required
                    />
                </div>
                <Button type="submit" disabled={formSubmitted}>
                    Create Cache
                </Button>
                <div className="grid gap-2">{formState.message}</div>
            </div>
        </Form>
    );
}