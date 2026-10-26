"use client"

import React, {useState} from "react";
import {Input} from "@/components/ui/input";
import {Button} from "@/components/ui/button";
import {decrementCounterRequest, incrementCounterRequest, setCounterRequest} from "@/lib/actions";
import {toast} from "sonner";
import {getLogger} from "@/lib/loggingUtil";
import {CounterOpResponse} from "@/lib/types";

const logger = getLogger("CounterRowActions")

interface CounterRowActionsProps {
    cacheId: number
    itemKey: string
}

/**
 * Inline controls for operating on a single counter: increment and
 * decrement by an amount, or set to a specific value.
 * @constructor
 */
export function CounterRowActions(props: CounterRowActionsProps) {

    const [amount, setAmount] = useState<number>(1);
    const [newValue, setNewValue] = useState<number>(0);
    const [busy, setBusy] = useState(false);

    function toastResult(action: string, content: CounterOpResponse | undefined) {
        if (content && content.operationStatus === "SUCCESS") {
            toast.success("Success", {
                description: action + " succeeded, value is now " + content.value,
                action: {label: "OK", onClick: () => logger.info("Ack alert")},
            })
        } else {
            toast.error("Error", {
                description: "Failed to " + action + ": " + (content ? content.operationStatus : "no response"),
                action: {label: "OK", onClick: () => logger.info("Ack alert")},
            })
        }
    }

    const onIncrement = async () => {
        setBusy(true)
        try {
            logger.info("Increment counter " + props.itemKey + " by " + amount)
            const content = await incrementCounterRequest({cacheId: props.cacheId, key: props.itemKey, amount})
            toastResult("increment", content)
        } finally {
            setBusy(false)
        }
    }

    const onDecrement = async () => {
        setBusy(true)
        try {
            logger.info("Decrement counter " + props.itemKey + " by " + amount)
            const content = await decrementCounterRequest({cacheId: props.cacheId, key: props.itemKey, amount})
            toastResult("decrement", content)
        } finally {
            setBusy(false)
        }
    }

    const onSet = async () => {
        setBusy(true)
        try {
            logger.info("Set counter " + props.itemKey + " to " + newValue)
            const content = await setCounterRequest({cacheId: props.cacheId, key: props.itemKey, value: newValue})
            toastResult("set", content)
        } finally {
            setBusy(false)
        }
    }

    return (
        <div className={"flex flex-row items-center gap-2"}>
            <Input
                type="number"
                value={amount}
                onChange={(e) => setAmount(Number(e.target.value))}
                className={"w-20"}
                aria-label={"Amount for " + props.itemKey}
                data-testid={`counter-amount-input-${props.itemKey}`}
            />
            <Button
                type="button"
                size="sm"
                onClick={onIncrement}
                disabled={busy}
                data-testid={`counter-increment-button-${props.itemKey}`}
            >
                +
            </Button>
            <Button
                type="button"
                size="sm"
                variant="outline"
                onClick={onDecrement}
                disabled={busy}
                data-testid={`counter-decrement-button-${props.itemKey}`}
            >
                −
            </Button>
            <Input
                type="number"
                value={newValue}
                onChange={(e) => setNewValue(Number(e.target.value))}
                className={"w-20"}
                aria-label={"Set value for " + props.itemKey}
                data-testid={`counter-set-input-${props.itemKey}`}
            />
            <Button
                type="button"
                size="sm"
                variant="secondary"
                onClick={onSet}
                disabled={busy}
                data-testid={`counter-set-button-${props.itemKey}`}
            >
                Set
            </Button>
        </div>
    );
}
