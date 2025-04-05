import {allCachesColumns} from "@/components/cache-summary/AllCachesColumns";
import {AllCachesDataTable} from "@/components/cache-summary/AllCachesTable";
import CreateCacheRequest from "@/components/CreateCache";

/**
 * A component to display a table of available caches.
 * @constructor
 */
function CacheTable() {
    let data = [{cacheId: 1, itemCount: 0}]
    return (
        <AllCachesDataTable columns={allCachesColumns} data={data}/>
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
        <h1>Aeron Cache Dashboard</h1>
          <div className="pt-8">
              <div className="pb-2 px-8 md:w-1/2">
                  <CreateCache/>
              </div>
              <div className="pb-2 px-8 md:w-1/2">
                  <CacheTable/>
              </div>
          </div>
      </div>
  )
}