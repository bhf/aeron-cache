import {allCachesColumns} from "@/components/cache-summary/AllCachesColumns";
import {AllCachesDataTable} from "@/components/cache-summary/AllCachesTable";
import CreateCacheRequest from "@/components/CreateCache";
import {Card, CardContent, CardDescription, CardHeader, CardTitle,} from "@/components/ui/card"
import {DashboardStats} from "@/app/dashboard/dashboard-stats";


/**
 * A component to display a table of available caches.
 * @constructor
 */
function CacheTable() {
    let data = [{cacheId: 1, itemCount: 0}]
    return (
        <AllCachesDataTable columns={allCachesColumns} data={data}/>
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

    return (
        <div>
            <div className="flex flex-1 flex-col">
                <div className="@container/main flex flex-1 flex-col gap-2">
                    <div className="flex flex-col gap-4 py-4 md:w-1/2 md:gap-6 md:py-6">
                        <DashboardStats {...dashStats}/>
                    </div>
                </div>
            </div>
            <div className="pt-4">
                <div className="pb-6 px-6 md:w-1/2">
                    <Card>
                        <CardHeader>
                            <CardTitle>Create Cache</CardTitle>
                            <CardDescription>Create a new cache</CardDescription>
                        </CardHeader>
                        <CardContent>
                            <CreateCache/>
                        </CardContent>
                    </Card>
                </div>
                <div className="pb-2 px-6 md:w-1/2">
                    <CacheTable/>
                </div>
            </div>
        </div>
    )
}