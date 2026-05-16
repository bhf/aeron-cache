"use client";

import {Button} from "@/components/ui/button";
import {CameraIcon, ZapOffIcon} from "lucide-react";
import {toast} from "sonner";

interface CacheAdminActionsProps {
    baseUri?: string
}

export default function CacheAdminActions({baseUri}: CacheAdminActionsProps) {
    const handleSnapshot = async () => {
        try {
            const res = await fetch(baseUri+'/snapshot', {method: 'POST'});
            if (!res.ok) throw new Error("Couldn't take a snapshot");
            const props = {
                title: "Snapshot Requested",
                description: "Snapshot successfully requested",
                actionLabel: "OK"
            };
            toast(props.title, {
                description: props.description,
                action: {
                    label: props.actionLabel,
                    onClick: () => {
                    },
                },
            })
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
        } catch (err) {
            const props = {
                title: "Snapshot Request Error",
                description: "Error requesting snapshot",
                actionLabel: "OK"
            };
            toast(props.title, {
                description: props.description,
                action: {
                    label: props.actionLabel,
                    onClick: () => {
                    },
                },
            })
        }
    };

    const handleShutdown = async () => {
        try {
            const res = await fetch(baseUri+'/shutdown', {method: 'POST'});
            if (!res.ok) throw new Error("Couldn't request shutdown");
            const props = {
                title: "Shutdown Requested",
                description: "Shutdown successfully requested",
                actionLabel: "OK"
            };
            toast(props.title, {
                description: props.description,
                action: {
                    label: props.actionLabel,
                    onClick: () => {
                    },
                },
            })
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
        } catch (err) {
            const props = {
                title: "Shutdown Request Error",
                description: "Error requesting shutdown",
                actionLabel: "OK"
            };
            toast(props.title, {
                description: props.description,
                action: {
                    label: props.actionLabel,
                    onClick: () => {
                    },
                },
            })
        }
    };

    return (
        <>
            <Button variant="link" onClick={handleSnapshot}>
                <CameraIcon/>Snapshot
            </Button>
        </>
    );
}
