import {Card, CardDescription, CardHeader, CardTitle,} from "@/components/ui/card"
import {Badge} from "@/components/ui/badge";
import {BinocularsIcon, DatabaseIcon, FireExtinguisherIcon, MicroscopeIcon} from "lucide-react";

interface DashboardStatsProps {
    errorCount: string
    totalItemsCount: string
    totalCachesCount: string
    totalOpsCount: string
}

export function DashboardStats(props: DashboardStatsProps) {

    return (
        <div
            className="*:data-[slot=card]:shadow-lg @xl/main:grid-cols-2 @5xl/main:grid-cols-4 grid grid-cols-1 gap-4 px-4 *:data-[slot=card]:bg-gradient-to-t *:data-[slot=card]:from-primary/5 *:data-[slot=card]:to-card dark:*:data-[slot=card]:bg-card lg:px-6">
            <Card className="@container/card">
                <CardHeader className="relative">
                    <CardDescription>Total Ops</CardDescription>
                    <CardTitle className="@[250px]/card:text-3xl text-2xl font-semibold tabular-nums">
                        {props.totalOpsCount}
                    </CardTitle>
                    <div className="hidden lg:block">
                        <div className="absolute right-4 top-4">
                            <Badge variant="outline" className="flex gap-1 rounded-lg text-xs">
                                <MicroscopeIcon className="size-3"/>
                            </Badge>
                        </div>
                    </div>
                </CardHeader>

            </Card>
            <Card className="@container/card">
                <CardHeader className="relative">
                    <CardDescription>Caches</CardDescription>
                    <CardTitle className="@[250px]/card:text-3xl text-2xl font-semibold tabular-nums">
                        {props.totalCachesCount}
                    </CardTitle>
                    <div className="hidden lg:block">
                        <div className="absolute right-4 top-4">
                            <Badge variant="outline" className="flex gap-1 rounded-lg text-xs">
                                <DatabaseIcon className="size-3"/>
                            </Badge>
                        </div>
                    </div>
                </CardHeader>
            </Card>
            <Card className="@container/card">
                <CardHeader className="relative">
                    <CardDescription>Items</CardDescription>
                    <CardTitle className="@[250px]/card:text-3xl text-2xl font-semibold tabular-nums">
                        {props.totalItemsCount}
                    </CardTitle>
                    <div className="hidden lg:block">
                        <div className="absolute right-4 top-4">
                            <Badge variant="outline" className="flex gap-1 rounded-lg text-xs">
                                <BinocularsIcon className="size-3"/>
                            </Badge>
                        </div>
                    </div>
                </CardHeader>
            </Card>
            <Card className="@container/card">
                <CardHeader className="relative">
                    <CardDescription>Errors</CardDescription>
                    <CardTitle className="@[250px]/card:text-3xl text-2xl font-semibold tabular-nums">
                        {props.errorCount}
                    </CardTitle>
                    <div className="hidden lg:block">
                        <div className="absolute right-4 top-4">
                            <Badge variant="outline" className="flex gap-1 rounded-lg text-xs">
                                <FireExtinguisherIcon className="size-3"/>
                            </Badge>
                        </div>
                    </div>
                </CardHeader>
            </Card>
        </div>
    )
}
