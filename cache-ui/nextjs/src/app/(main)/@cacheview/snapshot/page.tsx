import {SnapshotInfo} from "@/components/snapshot-summary/SnapshotInfo";
import TriggerSnapshot from "@/components/snapshot-summary/TriggerSnapshot";
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card";
import {getCacheAPIURI} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";
import {JSX, Suspense} from "react";
import {Skeleton} from "@/components/ui/skeleton";

const logger = getLogger("SnapshotPage")

export const dynamic = 'force-dynamic'

const headers = {
    'Accept': 'application/json',
    'Content-Type': 'application/json'
}

/**
 * Fetch and render the latest cluster snapshot info and archive disk usage.
 * @constructor
 */
async function SnapshotPanel() {

    const apiUri = await getCacheAPIURI();

    let rawResponse
    try {
        rawResponse = await fetch(apiUri + '/snapshot-info', {
                method: 'GET',
                headers,
                cache: "no-cache"
            },
        )
    } catch (err) {
        logger.warn("Error whilst sending request to get snapshot info ", err);
        return
    }

    const content = await rawResponse.json();
    logger.info("Got response from get snapshot info ", content)

    return (
        <SnapshotInfo {...content}/>
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
