"use client"
import {useEffect, useState} from 'react';
import {useParams} from 'next/navigation';
import Terminal from "@/app/@main/wss/[slug]/terminal";


export default function Page({
                                 params,
                             }: {
    params: Promise<{ slug: string }>
}) {

    const [messages, setMessages] = useState<string[]>([]);

    let webSocket: WebSocket;
    if (typeof window !== "undefined") {
        const url = "ws:localhost:7071"
        const params = useParams();
        console.log("Connecting to " + url + " on cacheId: " + params.slug);
        webSocket = new WebSocket(`${url}/api/ws/v1/cache/` + params.slug);
    }

    useEffect(() => {
        webSocket.onmessage = (event) => {
            setMessages((prevMessages) => [...prevMessages, event.data]);
        };
    }, []);

    return (
        <div>
            <div className={"pl-6"}>
                <Terminal messages={messages}></Terminal>
            </div>
        </div>
    );
};
