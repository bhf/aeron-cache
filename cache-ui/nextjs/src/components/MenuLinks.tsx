import AboutAeronCache from "@/components/About";
import Link from "next/link";
import {Button} from "@/components/ui/button";
import {LayersIcon, TrelloIcon} from "lucide-react";
import {getJaegerURL, getPrometheusURL} from "@/lib/actions";
import CacheAdminActions from "@/components/CacheAdminActions";

export default async function MenuLinks() {
    const jaegerURL = await getJaegerURL()
    const prometheuesURL = await getPrometheusURL()
    const adminBaseURI = process.env.AERON_CACHE_API

    console.log("Jaeger url:"+jaegerURL+", Prometheus URL:"+prometheuesURL+", Admin Base URI:"+adminBaseURI);

    return (
        <div className="hidden lg:flex lg:flex-1 lg:justify-end pr-4">
            <AboutAeronCache/>
            <Link href={jaegerURL} target="_blank"><Button
                variant="link"><LayersIcon/>Jaeger</Button></Link>
            <Link href={prometheuesURL} target="_blank"><Button
                variant="link"><TrelloIcon/>Prometheus</Button></Link>
            <CacheAdminActions baseUri = {adminBaseURI}/>
        </div>
    )
}