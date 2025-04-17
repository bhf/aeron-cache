import {DashboardStats} from "@/app/@dashboard/dashboard/dashboard-stats";
import {getCacheAPIURI} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";

const logger = getLogger("MainDash")

const headers = {
    'Accept': 'application/json',
    'Content-Type': 'application/json'
}

export default async function Page() {

    let rawResponse
    try {
        rawResponse = await fetch(await getCacheAPIURI() + '/stats/', {
                method: 'GET',
                headers,
                cache: "no-cache"
            },
        )
    } catch (err) {
        logger.warn("Error whilst sending request to get stats ", err);
        return
    }

    const content = await rawResponse.json();

    return (
        <div>
            <div className="flex flex-1 flex-col">
                <div className="@container/main flex flex-1 flex-col gap-2">
                    <div className="flex flex-col gap-4 py-4 md:w-1/2 md:gap-6 md:py-6">
                        <DashboardStats {...content}/>
                    </div>
                </div>
            </div>
        </div>
    )
}