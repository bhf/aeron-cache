import {CacheItemsDataTable} from "@/components/cache-view/CacheItemsTable";
import {counterItemColumns} from "@/components/counters-view/CounterItemColumns";
import AddCounterRequest from "@/components/AddCounter";
import {Card, CardTitle} from "@/components/ui/card";
import {getCacheAPIURI} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";
import {JSX, Suspense} from "react";
import {Skeleton} from "@/components/ui/skeleton";
import {DeleteCounterCache} from "@/app/@cacheview/counters/[slug]/DeleteCounterCache";
import {ClearCounterCache} from "@/app/@cacheview/counters/[slug]/ClearCounterCache";

const logger = getLogger("CounterCachePage")

const headers = {
    'Accept': 'application/json',
    'Content-Type': 'application/json'
}

interface CounterItemsTableProps {
    cacheId: string
}

interface CounterItem {
    key: string
    value: number
}

/**
 * A component to display the counters in this counter cache.
 * @constructor
 */
async function CounterItemsTable(props: CounterItemsTableProps) {

    let rawResponse
    try {
        rawResponse = await fetch(await getCacheAPIURI() + '/counters/' + props.cacheId, {
                method: 'GET',
                headers,
                next: {tags: ['Counters-' + props.cacheId]},
                cache: "no-cache"
            },
        )
    } catch (err) {
        logger.warn("Error whilst trying to get counter cache content ", err);
        return
    }

    const content = await rawResponse.json();
    const data = content.items

    const cleanInput = data.map((item: CounterItem) => {
        return {...item, cacheId: props.cacheId};
    })

    // noinspection TypeScriptValidateTypes
    return (
        <CacheItemsDataTable columns={counterItemColumns} data={cleanInput}/>
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
 * A page to display and operate on the counters within a specific
 * Aeron Cache counter cache.
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
            <div className={"text-2xl pb-4 justify-between space-x-2"} data-testid="deleteClearCounter">
                <p>{"Counter Cache ID: " + cacheId}</p>
                <div className={"py-1 justify-between space-x-2"} data-testid="deleteClearCounter">
                    <DeleteCounterCache cacheId={cacheId}/>
                    <ClearCounterCache cacheId={cacheId}/>
                </div>
            </div>

            <div className={"pb-1 justify-between space-y-6"} data-testid="addCounterSection">
                <Card className="pt-5 pb-2 px-2 shadow-lg">
                    <div data-testid="addCounter">
                        <CardTitle>Add Counter</CardTitle>
                        <AddCounterRequest cacheId={cacheId}/>
                    </div>
                </Card>
                <Card className="pt-5 pb-2 px-2 shadow-lg">
                    <div data-testid="counterItems">
                        <Suspense fallback={<SkeletonLoading/>}>
                            <CounterItemsTable cacheId={cacheId}/>
                        </Suspense>
                    </div>
                </Card>
            </div>
        </div>
    )
}
