import {allCachesColumns} from "@/components/cache-summary/AllCachesColumns";
import {AllCachesDataTable} from "@/components/cache-summary/AllCachesTable";
import CreateCacheRequest from "@/components/CreateCache";
import {Card, CardContent, CardDescription, CardHeader, CardTitle,} from "@/components/ui/card"
import {DashboardStats} from "@/app/dashboard/dashboard-stats";
import {getCacheAPIURI} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";

const logger = getLogger("MainDash")

const headers = {
    'Accept': 'application/json',
    'Content-Type': 'application/json'
}

/**
 * A component to display a table of available caches.
 * @constructor
 */
async function CacheTable() {

    const data = [{cacheId: 1, itemCount: 0}, {cacheId: 2, itemCount: 0}, {cacheId: 2, itemCount: 0}, {
        cacheId: 2,
        itemCount: 0
    }, {cacheId: 2, itemCount: 0}, {cacheId: 2, itemCount: 0}, {cacheId: 2, itemCount: 0}, {
        cacheId: 2,
        itemCount: 0
    }, {cacheId: 2, itemCount: 0}, {cacheId: 2, itemCount: 0}, {cacheId: 2, itemCount: 0}];


    let rawResponse
    try {
        rawResponse = await fetch(await getCacheAPIURI() + '/caches/', {
                method: 'GET',
                headers
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

export default async function Page() {

    const dashStats = {
        errorCount: "3",
        totalItemsCount: "1,344",
        totalCachesCount: "12",
        totalOpsCount: "1,234,567"
    }

    let rawResponse
    try {
        rawResponse = await fetch(await getCacheAPIURI() + '/stats/', {
                method: 'GET',
                headers
            },
        )
    } catch (err) {
        logger.warn("Error whilst sending request to clear cache ", err);
        return
    }

    const content = await rawResponse.json();


    return (
        <div>
            <div className="flex flex-1 flex-col">
                <div className="@container/main flex flex-1 flex-col gap-2">
                    <div className="flex flex-col gap-4 py-4 md:w-1/2 md:gap-6 md:py-6">
                        <DashboardStats {...content}/>
                    </div>
                </div>
            </div>
            <div className="pt-4">
                <div className="pb-6 px-6 md:w-1/2">
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
                <div className="pb-6 px-6 md:w-1/2">
                    <Card className={"shadow-lg"}>
                        <CardHeader>
                            <CardTitle>All Caches</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <CacheTable/>
                        </CardContent>
                    </Card>
                </div>

            </div>
        </div>
    )
}