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
        <div>
        </div>
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
            <h1>All Caches</h1>
            <h2>Create cache</h2>
            <CreateCache/>
            <h2>All Caches</h2>
            <CacheTable/>
        </div>
    );
}