import {useEffect, useRef, useState} from "react"
import {getLogger} from "@/lib/loggingUtil"

interface UseWebSocketOptions {
    onOpen?: (event: Event) => void
    onMessage?: (event: MessageEvent) => void
    onClose?: (event: CloseEvent) => void
    onError?: (event: Event) => void
    reconnectAttempts?: number
    reconnectInterval?: number
}

const logger = getLogger("UseWebSocket")

export const useWebSocket = (
    wsUrl: string,
    options: UseWebSocketOptions
) => {
    const {
        onOpen,
        onMessage,
        onClose,
        onError,
        reconnectAttempts = 5,
        reconnectInterval = 3000,
    } = options

    const [isConnected, setIsConnected] = useState(false)
    const [isReconnecting, setIsReconnecting] = useState(false)

    const webSocketRef = useRef<WebSocket | null>(null)
    const attemptsRef = useRef(0)

    const connectWebSocket = () => {
        setIsReconnecting(false)
        attemptsRef.current = 0

        const ws = new WebSocket(wsUrl)
        webSocketRef.current = ws

        ws.onopen = (event) => {
            logger.info("Websocket opened")
            setIsConnected(true)
            setIsReconnecting(false)
            if (onOpen) onOpen(event)
        }

        ws.onmessage = (event) => {
            if (onMessage) onMessage(event)
        }

        ws.onclose = (event) => {
            logger.info("Websocket closed")
            setIsConnected(false)
            if (onClose) onClose(event)

            if (attemptsRef.current < reconnectAttempts) {
                setIsReconnecting(true)
                attemptsRef.current++
                setTimeout(connectWebSocket, reconnectInterval)
            }
        }

        ws.onerror = (event) => {
            if (onError) onError(event)
        }
    }

    useEffect(() => {
        connectWebSocket()

        // Cleanup on component unmount
        return () => {
            if (webSocketRef.current) {
                webSocketRef.current.close()
            }
        }
    }, [wsUrl])

    const sendMessage = (message: string) => {
        if (
            webSocketRef.current &&
            webSocketRef.current.readyState === WebSocket.OPEN
        ) {
            webSocketRef.current.send(message)
        } else {
            logger.error("WebSocket is not open. Unable to send message.")
        }
    }

    return {isConnected, isReconnecting, sendMessage}
}