import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card"
import {Status, StatusIndicator, StatusLabel} from "@/components/ui/shadcn-io/status"
import {SnapshotInfo as SnapshotInfoType} from "@/lib/types"
import {ArchiveIcon, CameraIcon, HardDriveIcon, Trash2Icon} from "lucide-react"
import {JSX} from "react"

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
 * Format an epoch-millis timestamp, or a dash when it is absent.
 */
function formatTimestamp(ts: number): string {
    if (!ts || ts < 0) {
        return "—"
    }
    return new Date(ts).toLocaleString()
}

/**
 * A single labelled value in the snapshot card.
 */
function Field(props: { label: string, value: string | number, icon?: JSX.Element, testid?: string }) {
    return (
        <div className="flex flex-col gap-1">
            <span className="flex items-center gap-1 text-xs text-muted-foreground">
                {props.icon}{props.label}
            </span>
            <span className="text-sm font-medium tabular-nums" data-testid={props.testid}>
                {props.value}
            </span>
        </div>
    )
}

/**
 * Read-only dashboard card showing the latest cluster snapshot and archive disk usage, plus the
 * result of the most recent recording purge. Mirrors {@link DashboardStats} for styling.
 */
export function SnapshotInfo(props: SnapshotInfoType) {
    const present = props.snapshotPresent
    const purge = props.lastPurge

    return (
        <Card className="@container/card" data-testid="snapshot-info">
            <CardHeader className="relative">
                <CardDescription>Cluster Snapshot</CardDescription>
                <CardTitle className="flex items-center gap-2 text-2xl font-semibold">
                    <CameraIcon className="size-5"/>
                    {present ? "Snapshot present" : "No snapshot"}
                </CardTitle>
                <div className="absolute right-4 top-4">
                    <Status
                        status={present ? "online" : "offline"}
                        variant="outline"
                        className="gap-2 rounded-full px-3 py-1 text-xs"
                        data-testid="snapshot-status">
                        <StatusIndicator/>
                        <StatusLabel>{present ? "Ready" : "None"}</StatusLabel>
                    </Status>
                </div>
            </CardHeader>

            <CardContent className="grid grid-cols-2 gap-4 @[400px]/card:grid-cols-4">
                <Field label="Snapshots" value={props.snapshotCount} testid="snapshot-count"/>
                <Field label="Log position"
                       value={present ? props.logPosition.toLocaleString() : "—"}
                       testid="snapshot-log-position"/>
                <Field icon={<HardDriveIcon className="size-3"/>}
                       label="Archive size"
                       value={formatBytes(props.archiveDirBytes)}
                       testid="snapshot-archive-bytes"/>
                <Field label="Taken" value={formatTimestamp(props.timestamp)} testid="snapshot-timestamp"/>
            </CardContent>

            <CardContent className="border-t pt-4">
                <div className="mb-2 flex items-center gap-2 text-xs text-muted-foreground">
                    <Trash2Icon className="size-3"/>
                    Last purge
                </div>
                {purge ? (
                    <div className="grid grid-cols-2 gap-4 @[400px]/card:grid-cols-4">
                        <Field icon={<ArchiveIcon className="size-3"/>}
                               label="Reclaimed"
                               value={formatBytes(purge.reclaimedBytes)}
                               testid="purge-reclaimed-bytes"/>
                        <Field label="Status"
                               value={purge.success ? "Success" : "Failed"}
                               testid="purge-status"/>
                        <Field label="Ran" value={formatTimestamp(purge.timestamp)} testid="purge-timestamp"/>
                        <div className="col-span-2 flex flex-col gap-1 @[400px]/card:col-span-1">
                            <span className="text-xs text-muted-foreground">Detail</span>
                            <span className="truncate text-sm" title={purge.message} data-testid="purge-message">
                                {purge.message}
                            </span>
                        </div>
                    </div>
                ) : (
                    <span className="text-sm text-muted-foreground" data-testid="purge-none">
                        No purge has run yet.
                    </span>
                )}
            </CardContent>
        </Card>
    )
}
