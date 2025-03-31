import CreateCacheRequest from "@/components/CreateCache";
import {DataTable} from "@/components/AllCachesTable";
import {liveUsersCols} from "@/components/AllCachesColumns";

/**
 * A component to display a table of available caches.
 * @constructor
 */
function CacheTable() {
    let data = [{cacheId: 1, itemCount: 0}]
    return (
        <DataTable columns={liveUsersCols} data={data}/>
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

/**
 * A page to display a summary of existing Aeron Cache instances and
 * to allow the user to create a cache.
 * @constructor
 */
export default async function Page() {
    return (
        <div>
            <div className="pb-2 px-8 md:w-1/2">
                <CreateCache/>
            </div>
            <div className="pb-2 px-8 md:w-1/2">
                <CacheTable/>
            </div>
        </div>
    );
}