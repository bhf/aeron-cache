import {DashboardStats} from "@/app/@dashboard/dashboard/DashboardStats";
import {getCacheAPIURI} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";
import {JSX, Suspense} from "react";
import {Skeleton} from "@/components/ui/skeleton";

const logger = getLogger("MainDash")

const headers = {
    'Accept': 'application/json',
    'Content-Type': 'application/json'
}

async function DashboardCards() {
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
        <div className="flex flex-col gap-4 py-4 md:gap-6 md:py-6">
            <DashboardStats {...content}/>
        </div>
    )
}

/**
 * A basic loading skeleton for the dashboard.
 * @constructor
 */
const SkeletonLoading: () => JSX.Element = () => (
    <div className="flex items-center">
        <Skeleton className="h-12 w-12 rounded-full"/>
        <div className="space-x-5 flex pt-5">
            <Skeleton className="h-30 w-[190px] shadow-lg border"/>
            <Skeleton className="h-30 w-[190px] shadow-lg border"/>
            <Skeleton className="h-30 w-[190px] shadow-lg border"/>
            <Skeleton className="h-30 w-[190px] shadow-lg border"/>
        </div>
    </div>
)

export default async function Page() {
    return (
        <div>
            <div className="flex flex-1 flex-col">
                <div className="@container/main flex flex-1 flex-col gap-2">
                    <Suspense fallback={<SkeletonLoading/>}>
                        <DashboardCards/>
                    </Suspense>
                </div>
            </div>
        </div>
    )
}