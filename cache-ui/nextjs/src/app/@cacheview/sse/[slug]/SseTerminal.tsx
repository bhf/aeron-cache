"use client"

import {useParams} from "next/navigation";
import Terminal from "@/components/cache-view/Terminal";
import {getLogger} from "@/lib/loggingUtil";
import useSSE from "@/app/@cacheview/sse/useSSE";

/**
 * @param props
 */

const logger = getLogger("SSETerminalClient")

export default function SseTerminal(props: { url: string }) {

    const params = useParams();
    const sseUrl = `${props.url}/api/sse/v1/cache/` + params.slug
    logger.info("Connecting to " + sseUrl + " on cacheId: " + params.slug);
    const { isConnected, messages, error } = useSSE(sseUrl)

    return (
        <div>
            <Terminal messages={messages} isConnected={isConnected} errorMsg={error}></Terminal>
        </div>
    );

}