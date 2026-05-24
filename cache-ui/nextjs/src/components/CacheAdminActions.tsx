"use client";

import {Button} from "@/components/ui/button";
import {CameraIcon} from "lucide-react";
import {toast} from "sonner";
import {snapshotCacheRequest} from "@/lib/actions";

export default function CacheAdminActions() {
    const handleSnapshot = async () => {
        const result = await snapshotCacheRequest();

        if (result.error) {
            toast("Snapshot Request Error", {
                description: result.message,
                action: {
                    label: "OK",
                    onClick: () => {
                    },
                },
            })
        } else {
            toast("Snapshot Requested", {
                description: result.message,
                action: {
                    label: "OK",
                    onClick: () => {
                    },
                },
            })
        }
    };

    return (
        <>
            <Button variant="link" onClick={handleSnapshot} data-testid="admin-snapshot-button">
                <CameraIcon/>Snapshot
            </Button>
        </>
    );
}
