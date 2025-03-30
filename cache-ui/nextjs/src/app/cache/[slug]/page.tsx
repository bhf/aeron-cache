/**
 * A component to delete a cache.
 * @constructor
 */
function DeleteCache() {
    return (
        <div>
        </div>
    );
}

/**
 * A component to clear a cache.
 * @constructor
 */
function ClearCache() {
    return (
        <div>
        </div>
    );
}

/**
 * A component to add an item.
 * @constructor
 */
function AddItem() {
    return (
        <div>
        </div>
    );
}

/**
 * A component to remove a cache item by it's key.
 * @constructor
 */
function RemoveByKey() {
    return (
        <div>
        </div>
    );
}

/**
 * A component to display the items in this cache.
 * @constructor
 */
function CacheItemsTable() {
    return (
        <div>
        </div>
    );
}

/**
 * A page to display information on a specific Aeron Cache instance.
 * @constructor
 */
export default async function Page() {

    /**
     * Functional stateless component, can be simplified
     * further into a lambda with implicit return.
     *
     * @param value The value to display in the heading.
     * @constructor
     */
    const Headline = ({value}) => {
        return <h1>{value}</h1>;
    };

    return (
        <div>
            <Headline value={"Cache Functionality"}/>
            <h2>Delete this cache</h2>
            <DeleteCache/>
            <h2>Clear this cache</h2>
            <ClearCache/>
            <h2>Add Item</h2>
            <AddItem/>
            <h2>Remove by key</h2>
            <RemoveByKey/>
            <h2>View items in this cache</h2>
            <CacheItemsTable/>
        </div>
    )
}