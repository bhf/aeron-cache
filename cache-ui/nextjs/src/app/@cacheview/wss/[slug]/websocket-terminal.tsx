"use client"

import {useState} from "react";
import {useParams} from "next/navigation";
import Terminal from "@/components/cache-view/terminal";
import {getLogger} from "@/lib/loggingUtil";
import {useWebSocket} from "@/app/@cacheview/wss/useWebsocket";

/**
 * @param props
 */

const logger = getLogger("WSTerminalClient")

export default function WebsocketTerminal(props: { url: any }) {

    const [messages, setMessages] = useState<string[]>([]);
    const params = useParams();
    const wsUrl = `${props.url}/api/ws/v1/cache/` + params.slug

    const {isConnected} = useWebSocket(wsUrl, {
        onOpen: () => {
            const msg = "Websocket connection opened"
            const key = Date.now()
            const keyedMsg = JSON.stringify({msg, key: key})
            logger.info(msg)
            setMessages((prevMessages) => [...prevMessages, keyedMsg])
        },
        onError: err => {
            const msg = "Websocket error occurred: "+JSON.stringify(err)
            const key = Date.now()
            const keyedMsg = JSON.stringify({msg, key: key})
            logger.info(msg);
            setMessages((prevMessages) => [...prevMessages, keyedMsg])
        },
        onClose: event => {
            const msg = "Websocket connection closed: "+JSON.stringify(event)
            const key = Date.now()
            const keyedMsg = JSON.stringify({msg, key: key})
            logger.info(msg);
            setMessages((prevMessages) => [...prevMessages, keyedMsg])
        },
        onMessage: event => {
            logger.info("Received message", event.data);
            const key = event.data.requestId
            const data = JSON.parse(event.data)
            const keyedMsg = JSON.stringify({...data, key: key});
            setMessages((prevMessages) => [...prevMessages, keyedMsg]);
        }
    })

    return (
        <div>
            <Terminal messages={messages} isConnected></Terminal>
        </div>
    );
}