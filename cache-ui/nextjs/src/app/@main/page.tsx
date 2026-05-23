import {allCachesColumns} from "@/components/cache-summary/AllCachesColumns";
import {AllCachesDataTable} from "@/components/cache-summary/AllCachesTable";
import CreateCacheRequest from "@/components/CreateCache";
import {Card, CardContent, CardDescription, CardHeader, CardTitle,} from "@/components/ui/card"
import {getCacheAPIURI} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";
import {JSX, Suspense} from "react";
import {Skeleton} from "@/components/ui/skeleton";

const logger = getLogger("MainPanel")

export const dynamic = 'force-dynamic'

const headers = {
    'Accept': 'application/json',
    'Content-Type': 'application/json'
}

/**
 * A component to display a table of available caches.
 * @constructor
 */
async function CacheTable() {

    const apiUri = await getCacheAPIURI();

    let rawResponse
    try {
        await fetch(apiUri + '/stats/', {
            method: 'GET',
            headers,
            cache: "no-cache"
        });

        rawResponse = await fetch(apiUri + '/caches/', {
                method: 'GET',
                headers,
                cache: "no-cache"
            },
        )
    } catch (err) {
        logger.warn("Error whilst sending request to clear cache ", err);
        return
    }

    const content = await rawResponse.json();
    logger.info("Got response from get all caches ", content)

    return (
        <AllCachesDataTable columns={allCachesColumns} data={content}/>
    );
}

/**
 * A component to create caches.
 * @constructor
 */
function CreateCache() {
    return (
        <CreateCacheRequest/>
    );
}

const LoadingSkeleton: () => JSX.Element = () => (
    <div className="space-y-2">
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-20 w-full" />
        <Skeleton className="h-20 w-full" />
        <Skeleton className="h-20 w-full" />
    </div>
)

export default async function Page() {

    return (
        <div>
            <div className="pt-4">
                <div className="pb-6 px-6">
                    <Card className={"shadow-lg"}>
                        <CardHeader>
                            <CardTitle>Create Cache</CardTitle>
                            <CardDescription>Create a new cache</CardDescription>
                        </CardHeader>
                        <CardContent>
                            <CreateCache/>
                        </CardContent>
                    </Card>
                </div>
                <div className="pb-6 px-6">
                    <Card className={"shadow-lg"}>
                        <CardHeader>
                            <CardTitle>All Caches</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <Suspense fallback={<LoadingSkeleton/>}>
                                <CacheTable/>
                            </Suspense>
                        </CardContent>
                    </Card>
                </div>
            </div>
        </div>
    )
}