import {SnapshotInfo} from "@/components/snapshot-summary/SnapshotInfo";
import TriggerSnapshot from "@/components/snapshot-summary/TriggerSnapshot";
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card";
import {getSnapshotInfoPerBackend} from "@/lib/actions";
import {ServerIcon} from "lucide-react";
import {JSX, Suspense} from "react";
import {Skeleton} from "@/components/ui/skeleton";

export const dynamic = 'force-dynamic'

/**
 * A card shown in place of the snapshot info for a backend the aggregator could not reach.
 */
function BackendUnavailable(props: { label?: string, error: string | null }) {
    return (
        <Card className="@container/card" data-testid="snapshot-backend-unavailable">
            <CardHeader>
                <CardDescription>
                    Cluster Snapshot
                    {props.label && (
                        <span className="ml-2 inline-flex items-center gap-1 text-xs" data-testid="snapshot-backend">
                            <ServerIcon className="size-3"/>{props.label}
                        </span>
                    )}
                </CardDescription>
                <CardTitle className="text-base font-medium text-destructive">
                    Snapshot info unavailable{props.error ? ": " + props.error : ""}
                </CardTitle>
            </CardHeader>
        </Card>
    )
}

/**
 * Fetch and render the latest cluster snapshot info and archive disk usage, one card per cache
 * backend when the UI fronts the fan-out aggregator (single card otherwise).
 * @constructor
 */
async function SnapshotPanel() {

    const backends = await getSnapshotInfoPerBackend();

    if (backends.length === 0) {
        return (
            <p className="text-sm text-muted-foreground" data-testid="snapshot-none">
                Snapshot info unavailable.
            </p>
        );
    }

    // Only label cards by backend when there is more than one, or when a real gateway is named (the
    // single-gateway shape is normalised to one synthetic backend with a null gateway).
    const labelled = backends.length > 1 || backends[0].gateway !== null;

    return (
        <div className="space-y-4" data-testid="snapshot-backends">
            {backends.map((backend, index) => {
                const key = backend.gateway ?? String(index);
                const label = labelled ? (backend.gateway ?? `Backend ${index + 1}`) : undefined;
                if (!backend.ok || !backend.value) {
                    return <BackendUnavailable key={key} label={label} error={backend.error}/>;
                }
                return <SnapshotInfo key={key} {...backend.value} backendLabel={label}/>;
            })}
        </div>
    );
}

/**
 * A basic loading skeleton for the snapshot details.
 * @constructor
 */
const LoadingSkeleton: () => JSX.Element = () => (
    <div className="space-y-2">
        <Skeleton className="h-10 w-full"/>
        <Skeleton className="h-20 w-full"/>
        <Skeleton className="h-20 w-full"/>
    </div>
)

/**
 * The snapshot view shown in the right-hand panel (where cache items normally appear): controls to
 * take a cluster snapshot or snapshot-and-purge, plus the latest snapshot info and archive disk usage.
 * @constructor
 */
export default async function Page() {

    return (
        <div className={"pl-3 pr-6"}>
            <div className={"text-2xl pb-4"}>
                <p>Cluster Snapshot</p>
            </div>

            <div className={"pb-1 space-y-6"}>
                <Card className="shadow-lg">
                    <CardHeader>
                        <CardTitle>Take Snapshot</CardTitle>
                        <CardDescription>
                            Take a cluster snapshot, or snapshot and purge old log segments to reclaim disk
                        </CardDescription>
                    </CardHeader>
                    <CardContent>
                        <TriggerSnapshot/>
                    </CardContent>
                </Card>

                <Suspense fallback={<LoadingSkeleton/>}>
                    <SnapshotPanel/>
                </Suspense>
            </div>
        </div>
    )
}
