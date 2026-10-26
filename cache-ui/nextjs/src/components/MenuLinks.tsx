import AboutAeronCache from "@/components/About";
import ObservabilityButton from "@/components/observability/ObservabilityButton";
import Link from "next/link";
import {Button} from "@/components/ui/button";
import {LayersIcon, KanbanIcon, CameraIcon} from "lucide-react";
import {HEADER_BUTTON_CLASS} from "@/lib/headerStyles";
import {getJaegerURL, getPrometheusURL, isUrlAccessible} from "@/lib/actions";

export default async function MenuLinks() {
    const jaegerURL = await getJaegerURL()
    const prometheuesURL = await getPrometheusURL()

    const [isJaegerAccessible, isPrometheusAccessible] = await Promise.all([
        isUrlAccessible(jaegerURL),
        isUrlAccessible(prometheuesURL)
    ]);

    return (
        <div className="hidden lg:flex lg:flex-1 lg:items-center lg:justify-end">
            <AboutAeronCache/>
            {isJaegerAccessible && (
                <Link href={jaegerURL} target="_blank" data-testid="menu-link-jaeger">
                    <Button variant="link" className={HEADER_BUTTON_CLASS}><LayersIcon/>Jaeger</Button>
                </Link>
            )}
            {isPrometheusAccessible && (
                <Link href={prometheuesURL} target="_blank" data-testid="menu-link-prometheus">
                    <Button variant="link" className={HEADER_BUTTON_CLASS}><KanbanIcon/>Prometheus</Button>
                </Link>
            )}
            <ObservabilityButton/>
            <Link href="/snapshot" data-testid="menu-link-snapshot">
                <Button variant="link" className={HEADER_BUTTON_CLASS}><CameraIcon/>Snapshot</Button>
            </Link>
        </div>
    )
}