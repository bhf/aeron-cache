"use client";

import {addCounterRequest} from "@/lib/actions";
import Form from "next/form";
import {Input} from "@/components/ui/input";
import {Button} from "@/components/ui/button";
import React, {useState} from "react";
import {toast} from "sonner";
import {getLogger} from "@/lib/loggingUtil";

interface AddCounterProps {
    cacheId: string;
}

const logger = getLogger("AddCounter");

/**
 * A client side component which uses Next Forms to add a counter
 * with an initial value to a counter cache.
 * @constructor
 */
export default function AddCounterRequest(props: AddCounterProps) {
    const [formSubmitted, setFormSubmitted] = useState(false);
    const [hasToastShown, setHasToastShown] = useState(false);
    const initialFormState = {message: "", error: false};
    const [formState] = useState(initialFormState);

    /**
     * Raise an alert that the counter couldn't be added.
     */
    async function toasterAlert() {
        if (!hasToastShown) {
            await setHasToastShown(true);
            await setFormSubmitted(false);
            const props = {
                title: "Error sending add counter request",
                description: "Couldn't add counter",
                actionLabel: "OK",
            };
            toast(props.title, {
                description: props.description,
                action: {
                    label: props.actionLabel,
                    onClick: () => {
                        logger.info("Ack alert");
                        setHasToastShown(false);
                    },
                },
            });
        }
        setHasToastShown(false);

        return <></>;
    }

    /**
     * Toast success that the counter has been added.
     */
    async function toastSuccess() {
        if (!hasToastShown) {
            await setHasToastShown(true);
            await setFormSubmitted(false);
            const props = {
                title: "Success",
                description: "Added counter",
                actionLabel: "OK",
            };
            toast.success(props.title, {
                description: props.description,
                action: {
                    label: props.actionLabel,
                    onClick: () => {
                        logger.info("Ack alert");
                        setHasToastShown(false);
                    },
                },
            });
        }
        setHasToastShown(false);

        return <></>;
    }

    async function addCounter(formData: FormData) {
        await setHasToastShown(false);
        await setFormSubmitted(true);
        return sendRequest(formData);
    }

    async function sendRequest(formData: FormData) {
        const res = await addCounterRequest(formState, formData);
        logger.info("Got response from adding counter: ", res);
        if (res.error) {
            logger.warn("Got error trying to add counter: ", res.message);
            await toasterAlert();
        } else {
            await toastSuccess();
        }
    }

    return (
        <Form action={addCounter}>
            <div className="grid gap-4 pt-2">
                <div className="grid gap-2">
                    <Input
                        id="key"
                        type="string"
                        placeholder="Counter Key"
                        name="key"
                        required
                        data-testid="add-counter-key-input"
                    />
                    <Input
                        id="value"
                        type="number"
                        placeholder="Initial Value"
                        name="value"
                        required
                        data-testid="add-counter-value-input"
                    />
                    <Input
                        id="cacheId"
                        type="hidden"
                        name="cacheId"
                        value={props.cacheId}
                    />
                </div>
                <Button type="submit" disabled={formSubmitted} data-testid="add-counter-button">
                    Add
                </Button>
            </div>
        </Form>
    );
}
