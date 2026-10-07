"use client";

import {Button} from "@/components/ui/button";
import {CameraIcon, RefreshCwIcon, Trash2Icon} from "lucide-react";
import {toast} from "sonner";
import {snapshotAndPurgeRequest, snapshotCacheRequest} from "@/lib/actions";
import {useRouter} from "next/navigation";
import {useState} from "react";

/**
 * Format a byte count into a human readable size (e.g. 1.5 GB).
 */
function formatBytes(bytes: number): string {
    if (!bytes || bytes <= 0) {
        return "0 B"
    }
    const units = ["B", "KB", "MB", "GB", "TB"]
    const exponent = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
    const value = bytes / Math.pow(1024, exponent)
    return `${value.toFixed(exponent === 0 ? 0 : 1)} ${units[exponent]}`
}

/**
 * Buttons to trigger a cluster snapshot, or a snapshot followed by a purge of old log segments
 * (reclaiming disk), with a refresh control. After a successful request the snapshot info is
 * refreshed shortly afterwards (a snapshot takes a moment to persist) so the latest snapshot and
 * any reclaimed disk are reflected.
 */
export default function TriggerSnapshot() {
    const router = useRouter();
    const [pending, setPending] = useState<null | "snapshot" | "purge">(null);

    const refreshAfterDelay = () => {
        setTimeout(() => {
            router.refresh();
            setPending(null);
        }, 3000);
    };

    const handleSnapshot = async () => {
        setPending("snapshot");
        const result = await snapshotCacheRequest();

        if (result.error) {
            toast("Snapshot Request Error", {
                description: result.message,
                action: {label: "OK", onClick: () => {}},
            });
            setPending(null);
            return;
        }

        toast("Snapshot Requested", {
            description: result.message,
            action: {label: "OK", onClick: () => {}},
        });
        refreshAfterDelay();
    };

    const handleSnapshotAndPurge = async () => {
        setPending("purge");
        const result = await snapshotAndPurgeRequest();

        if (result.error) {
            toast("Snapshot & Purge Error", {
                description: result.message,
                action: {label: "OK", onClick: () => {}},
            });
            setPending(null);
            return;
        }

        const reclaimed = result.reclaimedBytes !== undefined
            ? ` (${formatBytes(result.reclaimedBytes)} reclaimed)`
            : "";
        toast("Snapshot & Purge Complete", {
            description: result.message + reclaimed,
            action: {label: "OK", onClick: () => {}},
        });
        refreshAfterDelay();
    };

    return (
        <div className="flex flex-wrap gap-2">
            <Button
                onClick={handleSnapshot}
                disabled={pending !== null}
                data-testid="trigger-snapshot-button">
                <CameraIcon/>{pending === "snapshot" ? "Requesting…" : "Take Snapshot"}
            </Button>
            <Button
                variant="secondary"
                onClick={handleSnapshotAndPurge}
                disabled={pending !== null}
                data-testid="trigger-snapshot-purge-button">
                <Trash2Icon/>{pending === "purge" ? "Purging…" : "Snapshot & Purge"}
            </Button>
            <Button
                variant="outline"
                onClick={() => router.refresh()}
                disabled={pending !== null}
                data-testid="refresh-snapshot-button">
                <RefreshCwIcon/>Refresh
            </Button>
        </div>
    );
}
