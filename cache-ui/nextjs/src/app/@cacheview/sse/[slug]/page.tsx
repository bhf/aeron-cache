import SseTerminal from "@/app/@cacheview/sse/[slug]/SseTerminal";
import {getSSEURL} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";

const logger = getLogger("SSEPageClient")

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
                <SseTerminal url={sseUrl}/>
            </div>
        </div>
    );
};
