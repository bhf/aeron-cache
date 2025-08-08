"use client"

import {useEffect, useState} from "react";
import {useParams} from "next/navigation";
import Terminal from "@/components/cache-view/terminal";
import {getLogger} from "@/lib/loggingUtil";

/**
 * @param props
 */

const logger = getLogger("SSETerminalClient")

export default function SseTerminal(props: { url: any }) {
    const [messages, setMessages] = useState<string[]>([]);

    let sseEventSource: EventSource;
    if (typeof window !== "undefined") {
        const params = useParams();
        logger.info("Connecting to " + props.url + " on cacheId: " + params.slug);
        sseEventSource = new EventSource(`${props.url}/api/sse/v1/cache/` + params.slug);
    }

    useEffect(() => {
        sseEventSource.onmessage = (event) => {
            logger.info("Received message", event.data);
            const key = event.data.requestId
            try {
                const data = JSON.parse(event.data)
                const keyedMsg = JSON.stringify({...data, key: key});
                setMessages((prevMessages) => [...prevMessages, keyedMsg]);
            } catch (e) {
                const keyedMsg = JSON.stringify({data: event.data, key: key});
                setMessages((prevMessages) => [...prevMessages, keyedMsg]);
            }
        };
    }, []);

    useEffect(() => {
        sseEventSource.onerror = () => {
            const msg = "SSE error occurred";
            const key = Date.now()
            const keyedMsg = JSON.stringify({msg, key: key})
            logger.info(msg);
            setMessages((prevMessages) => [...prevMessages, keyedMsg]);
        };
    }, []);

    useEffect(() => {
        sseEventSource.onopen = () => {
            const msg = "SSE connection opened"
            const key = Date.now()
            const keyedMsg = JSON.stringify({msg, key: key})
            logger.info(msg);
            setMessages((prevMessages) => [...prevMessages, keyedMsg]);
        };
    }, []);

    return (
        <div>
            <Terminal messages={messages}></Terminal>
        </div>
    );

}