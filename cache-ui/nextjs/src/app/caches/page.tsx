import CreateCacheRequest from "@/components/CreateCache";

/**
 * A component to display a table of available caches.
 * @constructor
 */
function CacheTable() {
    return (
        <div>
        </div>
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
            <CreateCache/>
            <h2>All Caches</h2>
            <CacheTable/>
        </div>
    );
}