import NearCacheManagement from "@/components/near-cache/NearCacheManagement";

export const dynamic = 'force-dynamic'

/**
 * The near cache management view shown in the right-hand panel (where cache items normally appear):
 * pick a gateway and cache kind, see which caches are warm, prime a cache up front, or stop near caching
 * one. Each gateway (http, ws, sse, Aeron) keeps its own near cache, reached over its own HTTP port.
 * @constructor
 */
export default async function Page() {
    return (
        <div className={"pl-3 pr-6"}>
            <div className={"text-2xl pb-4"}>
                <p>Near Cache</p>
            </div>
            <div className={"pb-1"}>
                <NearCacheManagement/>
            </div>
        </div>
    )
}
