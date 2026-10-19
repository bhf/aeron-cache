"use client";

import {Button} from "@/components/ui/button";
import {CameraIcon, RefreshCwIcon, ServerIcon, Trash2Icon} from "lucide-react";
import {toast} from "sonner";
import {snapshotAndPurgeRequest, snapshotCacheRequest} from "@/lib/actions";
import {NodePurgeResult} from "@/lib/types";
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
    const [nodeResults, setNodeResults] = useState<NodePurgeResult[] | null>(null);

    const refreshAfterDelay = () => {
        setTimeout(() => {
            router.refresh();
            setPending(null);
        }, 3000);
    };

    const handleSnapshot = async () => {
        setPending("snapshot");
        setNodeResults(null);
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
        setNodeResults(null);
        const result = await snapshotAndPurgeRequest();
        setNodeResults(result.nodes ?? null);

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
        <div className="space-y-4">
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

            {nodeResults && nodeResults.length > 0 && (
                <div className="border-t pt-3" data-testid="purge-node-breakdown">
                    <div className="mb-2 flex items-center gap-1 text-xs text-muted-foreground">
                        <ServerIcon className="size-3"/>
                        Per-node purge results
                    </div>
                    <div className="flex flex-col gap-2">
                        {nodeResults.map((node) => (
                            <div
                                key={node.host}
                                className="flex items-center justify-between gap-2 rounded-md border px-3 py-2 text-sm"
                                data-testid={`purge-node-${node.host}`}>
                                <div className="flex min-w-0 flex-col">
                                    <span className="truncate font-medium" title={node.host}>{node.host}</span>
                                    <span className="truncate text-xs text-muted-foreground" title={node.message}>
                                        {node.message}
                                    </span>
                                </div>
                                <div className="flex shrink-0 items-center gap-3">
                                    <span className="tabular-nums text-xs text-muted-foreground">
                                        {formatBytes(node.reclaimedBytes)}
                                    </span>
                                    <span
                                        className={node.success
                                            ? "text-xs font-medium text-green-600 dark:text-green-500"
                                            : "text-xs font-medium text-destructive"}
                                        data-testid={`purge-node-status-${node.host}`}>
                                        {node.success ? "OK" : "Failed"}
                                    </span>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
}
