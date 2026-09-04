import CounterWebsocketTerminal from "@/app/@cacheview/counters-wss/[slug]/CounterWebsocketTerminal";
import {getWebsocketURL} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";

const logger = getLogger("CounterWSPageClient")

export default async function Page({
                                       params,
                                   }: {
    params: Promise<{ slug: string }>
}) {
    const wsUrl = await getWebsocketURL()
    logger.info("Using WS Url: "+wsUrl)
    return (
        <div>
            <div className={"pl-6"}>
                <CounterWebsocketTerminal url={wsUrl}/>
            </div>
        </div>
    );
};
