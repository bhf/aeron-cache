import CounterSseTerminal from "@/app/@cacheview/counters-sse/[slug]/CounterSseTerminal";
import {getSSEURL} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";

const logger = getLogger("CounterSSEPageClient")

export default async function Page({
                                       params,
                                   }: {
    params: Promise<{ slug: string }>
}) {
    const sseUrl = await getSSEURL()
    logger.info("Using SSE Url: "+sseUrl)
    return (
        <div>
            <div className={"pl-6"}>
                <CounterSseTerminal url={sseUrl}/>
            </div>
        </div>
    );
};
