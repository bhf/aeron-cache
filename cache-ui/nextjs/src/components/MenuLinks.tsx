import AboutAeronCache from "@/components/About";
import Link from "next/link";
import {Button} from "@/components/ui/button";
import {LayersIcon, KanbanIcon} from "lucide-react";
import {getJaegerURL, getPrometheusURL, isUrlAccessible} from "@/lib/actions";
import CacheAdminActions from "@/components/CacheAdminActions";

export default async function MenuLinks() {
    const jaegerURL = await getJaegerURL()
    const prometheuesURL = await getPrometheusURL()

    const [isJaegerAccessible, isPrometheusAccessible] = await Promise.all([
        isUrlAccessible(jaegerURL),
        isUrlAccessible(prometheuesURL)
    ]);

    return (
        <div className="hidden lg:flex lg:flex-1 lg:justify-end pr-4">
            <AboutAeronCache/>
            {isJaegerAccessible && (
                <Link href={jaegerURL} target="_blank" data-testid="menu-link-jaeger">
                    <Button variant="link"><LayersIcon/>Jaeger</Button>
                </Link>
            )}
            {isPrometheusAccessible && (
                <Link href={prometheuesURL} target="_blank" data-testid="menu-link-prometheus">
                    <Button variant="link"><KanbanIcon/>Prometheus</Button>
                </Link>
            )}
            <CacheAdminActions/>
        </div>
    )
}