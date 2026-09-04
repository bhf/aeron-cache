"use client"

import {useParams} from "next/navigation";
import Terminal from "@/components/cache-view/Terminal";
import {getLogger} from "@/lib/loggingUtil";
import useSSE from "@/app/@cacheview/sse/useSSE";

/**
 * An SSE streaming terminal for counter caches.
 * @param props
 */

const logger = getLogger("CounterSSETerminalClient")

export default function CounterSseTerminal(props: { url: string }) {

    const params = useParams();
    const sseUrl = `${props.url}/api/sse/v1/counter/` + params.slug
    logger.info("Connecting to " + sseUrl + " on cacheId: " + params.slug);
    const { isConnected, messages, error } = useSSE(sseUrl)

    return (
        <div>
            <Terminal messages={messages} isConnected={isConnected} errorMsg={error}></Terminal>
        </div>
    );

}
