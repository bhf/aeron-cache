"use client";

import { addItemToCacheRequest } from "@/lib/actions";
import Form from "next/form";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import React, { useState } from "react";
import { toast } from "sonner";
import { getLogger } from "@/lib/loggingUtil";

interface AddItemProps {
  cacheId: number;
}

const logger = getLogger("AddItem");

export default function AddItemRequest(props: AddItemProps) {
  const [formSubmitted, setFormSubmitted] = useState(false);
  const [hasToastShown, setHasToastShown] = useState(false);
  const initialFormState = { message: "", error: false };
  const [formState] = useState(initialFormState);

  /**
   * Raise an alert that the item couldn't
   * be added to the cache.
   */
  async function toasterAlert() {
    if (!hasToastShown) {
      await setHasToastShown(true);
      await setFormSubmitted(false);
      const props = {
        title: "Error sending add item request",
        description: "Couldn't add item",
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
   * Toast success that the cache has been created.
   */
  async function toastSuccess() {
    if (!hasToastShown) {
      await setHasToastShown(true);
      await setFormSubmitted(false);
      const props = {
        title: "Success",
        description: "Added item",
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

  async function addItem(formData: FormData) {
    await setHasToastShown(false);
    await setFormSubmitted(true);
    return sendRequest(formData);
  }

  async function sendRequest(formData: FormData) {
    const res = await addItemToCacheRequest(formState, formData);
    logger.info("Got response from adding item: ", res);
    if (res.error) {
      logger.warn("Got error trying to add item: ", res.message);
      await toasterAlert();
    } else {
      await toastSuccess();
    }
  }

  return (
    <Form action={addItem}>
      <div className="grid gap-4 pt-2">
        <div className="grid gap-2">
          <Input
            id="key"
            type="string"
            placeholder="Item Key"
            name="key"
            required
          />
          <Input
            id="value"
            type="string"
            placeholder="Item Value"
            name="value"
            required
          />
          <Input
            id="cacheId"
            type="hidden"
            name="cacheId"
            value={props.cacheId}
          />
        </div>
        <Button type="submit" disabled={formSubmitted}>
          Add
        </Button>
      </div>
    </Form>
  );
}
