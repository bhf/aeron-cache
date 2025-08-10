import {CacheItemsDataTable} from "@/components/cache-view/CacheItemsTable";
import {cacheItemColumns} from "@/components/cache-view/CacheItemColumns";
import AddItemRequest from "@/components/AddCacheItem";
import {Card, CardTitle} from "@/components/ui/card";
import {getCacheAPIURI} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";
import {JSX, Suspense} from "react";
import {Skeleton} from "@/components/ui/skeleton";
import {DeleteCache} from "@/app/@cacheview/cache/[slug]/DeleteCache";
import {ClearCache} from "@/app/@cacheview/cache/[slug]/ClearCache";

const logger = getLogger("CachePage")

const headers = {
    'Accept': 'application/json',
    'Content-Type': 'application/json'
}

interface CacheItemsTableProps {
    cacheId: string
}

interface CacheItem {
    key: string
    value: string
}

/**
 * A component to display the items in this cache.
 * @constructor
 */
async function CacheItemsTable(props: CacheItemsTableProps) {

    let rawResponse
    try {
        rawResponse = await fetch(await getCacheAPIURI() + '/cache/' + props.cacheId, {
                method: 'GET',
                headers,
                next: { tags: ['Cache-'+props.cacheId] },
                cache: "no-cache"
            },
        )
    } catch (err) {
        logger.warn("Error whilst trying to get cache content ", err);
        return
    }

    const content = await rawResponse.json();
    const data = content.items

    const cleanInput = data.map((item: CacheItem) => {
        return {...item, cacheId: props.cacheId};
    })

    // noinspection TypeScriptValidateTypes
    return (
        <CacheItemsDataTable columns={cacheItemColumns} data={cleanInput}/>
    );
}

/**
 * A basic loading skeleton for the table.
 * @constructor
 */
const SkeletonLoading: () => JSX.Element = () => (
    <div className="flex items-center space-x-4">
        <Skeleton className="h-12 w-12 rounded-full"/>
        <div className="space-y-2">
            <Skeleton className="h-8 w-[500px]"/>
            <Skeleton className="h-8 w-[500px]"/>
            <Skeleton className="h-8 w-[500px]"/>
            <Skeleton className="h-8 w-[500px]"/>
            <Skeleton className="h-8 w-[500px]"/>
            <Skeleton className="h-8 w-[500px]"/>
            <Skeleton className="h-8 w-[500px]"/>
            <Skeleton className="h-8 w-[500px]"/>
        </div>
    </div>
)

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
    const cacheId = slug;

    return (
        <div className={"pl-3 pr-6"}>
            <div className={"text-2xl pb-4 justify-between space-x-2"} data-testid="deleteClear">
                <p>{"Cache ID: " + cacheId}</p>
                <div className={"py-1 justify-between space-x-2"} data-testid="deleteClear">
                    <DeleteCache cacheId={cacheId}/>
                    <ClearCache cacheId={cacheId}/>
                </div>
            </div>

            <div className={"pb-1 justify-between space-y-6"} data-testid="deleteClear">
                <Card className="pt-5 pb-2 px-2 shadow-lg">
                    <div data-testid="addItem">
                        <CardTitle>Add Item</CardTitle>
                        <AddItemRequest cacheId={cacheId}/>
                    </div>
                </Card>
                <Card className="pt-5 pb-2 px-2 shadow-lg">
                    <div data-testid="cacheItems">
                        <Suspense fallback={<SkeletonLoading/>}>
                            <CacheItemsTable cacheId={cacheId}/>
                        </Suspense>
                    </div>
                </Card>
            </div>
        </div>
    )
}