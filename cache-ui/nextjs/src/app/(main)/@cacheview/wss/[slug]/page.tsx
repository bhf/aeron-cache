import WebsocketTerminal from "@/app/(main)/@cacheview/wss/[slug]/WebsocketTerminal";
import {getWebsocketURL} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";

const logger = getLogger("WSPageClient")

export default async function Page({
                                       params,
                                   }: {
    params: Promise<{ slug: string }>
}) {
    const wsUrl = await getWebsocketURL()
    logger.info("Using WS Url: "+wsUrl)
    return (
        <div>
            <div className={"pl-2 pr-4"}>
                <WebsocketTerminal url={wsUrl}/>
            </div>
        </div>
    );
};
