import {CacheItemsDataTable} from "@/components/cache-view/CacheItemsTable";
import {cacheItemColumns} from "@/components/cache-view/CacheItemColumns";
import {DeleteCache} from "@/app/cache/[slug]/DeleteCache";
import {ClearCache} from "@/app/cache/[slug]/ClearCache";
import AddItemRequest from "@/components/AddCacheItem";
import {Card, CardTitle} from "@/components/ui/card";


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
            <div className={"text-2xl pb-1 justify-between space-x-2"} data-testid="deleteClear">
                <p>{"Cache ID: " + cacheId}</p>
                <div className={"py-1 justify-between space-x-2"} data-testid="deleteClear">
                    <DeleteCache cacheId={cacheId}/>
                    <ClearCache cacheId={cacheId}/>
                </div>
            </div>

            <Card className="pt-5 pb-2 px-2 md:w-1/3">
                <div data-testid="addItem">
                    <CardTitle>Add Item</CardTitle>
                    <AddItemRequest cacheId={cacheId}/>
                </div>
            </Card>
            <div className="py-8 px-2 md:w-1/3">
                <CacheItemsTable cacheId={cacheId}/>
            </div>
        </div>
    )
}