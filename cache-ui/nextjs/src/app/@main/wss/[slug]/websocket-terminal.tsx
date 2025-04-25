"use client"

import {useEffect, useState} from "react";
import {useParams} from "next/navigation";
import Terminal from "@/app/@main/wss/[slug]/terminal";
import {getLogger} from "@/lib/loggingUtil";

/**
 * @param props
 */

const logger = getLogger("WSTerminalClient")

export default function WebsocketTerminal(props: { url: any }) {
    const [messages, setMessages] = useState<string[]>([]);

    let webSocket: WebSocket;
    if (typeof window !== "undefined") {
        const params = useParams();
        logger.info("Connecting to " + props.url + " on cacheId: " + params.slug);
        webSocket = new WebSocket(`${props.url}/api/ws/v1/cache/` + params.slug);
    }

    useEffect(() => {
        webSocket.onmessage = (event) => {
            logger.info("Received message", event.data);
            const key = event.data.requestId
            const data = JSON.parse(event.data)
            const keyedMsg = JSON.stringify({...data, key: key});
            setMessages((prevMessages) => [...prevMessages, keyedMsg]);
        };
    }, []);

    useEffect(() => {
        webSocket.onerror = (event) => {
            const msg = "Websocket error occurred";
            const key = Date.now()
            const keyedMsg = JSON.stringify({msg, key: key})
            logger.info(msg);
            setMessages((prevMessages) => [...prevMessages, keyedMsg]);
        };
    }, []);

    useEffect(() => {
        webSocket.onopen = (event) => {
            const msg = "Websocket connection opened"
            const key = Date.now()
            const keyedMsg = JSON.stringify({msg, key: key})
            logger.info(msg);
            setMessages((prevMessages) => [...prevMessages, keyedMsg]);
        };
    }, []);

    useEffect(() => {
        webSocket.onclose = (event) => {
            const msg = "Websocket connection closed"
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