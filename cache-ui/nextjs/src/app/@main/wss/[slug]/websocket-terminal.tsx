"use client"

import {useEffect, useState} from "react";
import {useParams} from "next/navigation";
import Terminal from "@/app/@main/wss/[slug]/terminal";
import {getLogger} from "@/lib/loggingUtil";

/**
 * @param props
 */

const logger = getLogger("WSTerminalClient")

export default function WebsocketTerminal(props: {url: any}) {
    const [messages, setMessages] = useState<string[]>([]);

    let webSocket: WebSocket;
    if (typeof window !== "undefined") {
        const params = useParams();
        logger.info("Connecting to " + props.url + " on cacheId: " + params.slug);
        webSocket = new WebSocket(`${props.url}/api/ws/v1/cache/` + params.slug);
    }

    useEffect(() => {
        webSocket.onmessage = (event) => {
            setMessages((prevMessages) => [...prevMessages, event.data]);
        };
    }, []);

    return (
        <div>
            <Terminal messages={messages}></Terminal>
        </div>
    );
}