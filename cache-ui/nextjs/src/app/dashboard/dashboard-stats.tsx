import {Card, CardDescription, CardHeader, CardTitle,} from "@/components/ui/card"
import {Badge} from "@/components/ui/badge";
import {
    BinocularsIcon,
    ChartColumn, DatabaseIcon,
    EggFriedIcon,
    EggIcon, EyeIcon,
    FerrisWheel,
    FerrisWheelIcon, FireExtinguisherIcon, MicroscopeIcon,
    ShieldAlert,
    TrendingUpIcon
} from "lucide-react";

export function DashboardStats() {
    return (
        <div
            className="*:data-[slot=card]:shadow-xs @xl/main:grid-cols-2 @5xl/main:grid-cols-4 grid grid-cols-1 gap-4 px-4 *:data-[slot=card]:bg-gradient-to-t *:data-[slot=card]:from-primary/5 *:data-[slot=card]:to-card dark:*:data-[slot=card]:bg-card lg:px-6">
            <Card className="@container/card">
                <CardHeader className="relative">
                    <CardDescription>Cache Ops</CardDescription>
                    <CardTitle className="@[250px]/card:text-3xl text-2xl font-semibold tabular-nums">
                        1,292
                    </CardTitle>
                    <div className="absolute right-4 top-4">
                        <Badge variant="outline" className="flex gap-1 rounded-lg text-xs">
                            <MicroscopeIcon className="size-3"/>
                        </Badge>
                    </div>
                </CardHeader>

            </Card>
            <Card className="@container/card">
                <CardHeader className="relative">
                    <CardDescription>Caches</CardDescription>
                    <CardTitle className="@[250px]/card:text-3xl text-2xl font-semibold tabular-nums">
                        23
                    </CardTitle>
                    <div className="absolute right-4 top-4">
                        <Badge variant="outline" className="flex gap-1 rounded-lg text-xs">
                            <DatabaseIcon className="size-3"/>
                        </Badge>
                    </div>
                </CardHeader>
            </Card>
            <Card className="@container/card">
                <CardHeader className="relative">
                    <CardDescription>Items</CardDescription>
                    <CardTitle className="@[250px]/card:text-3xl text-2xl font-semibold tabular-nums">
                        45,678
                    </CardTitle>
                    <div className="absolute right-4 top-4">
                        <Badge variant="outline" className="flex gap-1 rounded-lg text-xs">
                            <BinocularsIcon className="size-3"/>
                        </Badge>
                    </div>
                </CardHeader>
            </Card>
            <Card className="@container/card">
                <CardHeader className="relative">
                    <CardDescription>Errors</CardDescription>
                    <CardTitle className="@[250px]/card:text-3xl text-2xl font-semibold tabular-nums">
                        3
                    </CardTitle>
                    <div className="absolute right-4 top-4">
                        <Badge variant="outline" className="flex gap-1 rounded-lg text-xs">
                            <FireExtinguisherIcon className="size-3"/>
                        </Badge>
                    </div>
                </CardHeader>
            </Card>
        </div>
    )
}
