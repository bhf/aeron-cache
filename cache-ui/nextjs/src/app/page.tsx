import {allCachesColumns} from "@/components/cache-summary/AllCachesColumns";
import {AllCachesDataTable} from "@/components/cache-summary/AllCachesTable";
import CreateCacheRequest from "@/components/CreateCache";
import {Card, CardContent, CardDescription, CardHeader, CardTitle,} from "@/components/ui/card"
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

    let rawResponse
    try {
        rawResponse = await fetch(await getCacheAPIURI() + '/caches/', {
                method: 'GET',
                headers,
                next: { tags: ['AllCaches'] }
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

    return (
        <div>
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