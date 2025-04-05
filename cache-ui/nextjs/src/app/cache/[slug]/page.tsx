import {CacheItemsDataTable} from "@/components/cache-view/CacheItemsTable";
import {cacheItemColumns} from "@/components/cache-view/CacheItemColumns";
import {DeleteCache} from "@/app/cache/[slug]/DeleteCache";
import {ClearCache} from "@/app/cache/[slug]/ClearCache";
import AddItemRequest from "@/components/AddCacheItem";


interface CacheItemsTableProps {
    cacheId: number
}


/**
 * A component to display the items in this cache.
 * @constructor
 */
function CacheItemsTable(props: CacheItemsTableProps) {
    let data = [{key: "someKey", value: "someValue", cacheId: props.cacheId, itemCount: 0}]
    // noinspection TypeScriptValidateTypes
    return (
        <CacheItemsDataTable columns={cacheItemColumns} data={data}/>
    );
}

/**
 * A page to display information on a specific Aeron Cache instance.
 * @constructor
 */
export default async function Page({
                                       params,
                                   }: {
    params: Promise<{ slug: string }>
}) {

    const {slug} = await params
    const cacheId = Number.parseInt(slug);

    return (
        <div>
            <p className={"text-2xl pb-1"}>{"Cache ID: " + cacheId}</p>
            <div className="pt-5 pb-2 px-2 md:w-1/3" data-testid="addItem">
                <AddItemRequest cacheId={cacheId}/>
            </div>
            <div className={"px-2 justify-between space-x-2 py-1"} data-testid="deleteClear">
                <DeleteCache cacheId={cacheId}/>
                <ClearCache cacheId={cacheId}/>
            </div>
            <div className="py-8 px-2 md:w-1/3">
                <CacheItemsTable cacheId={cacheId}/>
            </div>
        </div>
    )
}