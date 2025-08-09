import {useEffect, useRef, useState} from "react"
import {getLogger} from "@/lib/loggingUtil"


const useSSE = (url: string) => {
    const [isConnected, setIsConnected] = useState(false)
    const [messages, setMessages] = useState<any[]>([])
    const [error, setError] = useState<string | null>(null)
    const eventSourceRef = useRef<EventSource | null>(null)
    const reconnectAttemptsRef = useRef(0)
    const maxReconnectAttempts = 5
    const logger = getLogger("SSEHook")

    const connect = () => {
        logger.info("Trying to connect via SSE")
        if (eventSourceRef.current) {
            eventSourceRef.current.close()
        }

        const eventSource = new EventSource(url)
        logger.info("SSE Connecting to URL " + url)
        eventSourceRef.current = eventSource

        eventSource.onopen = () => {
            logger.info("SSE Connected to URL " + url)
            setIsConnected(true)
            setError(null)
            reconnectAttemptsRef.current = 0
        }

        eventSource.onmessage = (event) => {
            logger.debug("Received message on SSE", event.data)
            const key = event.data.requestId
            try {
                const data = JSON.parse(event.data)
                const keyedMsg = JSON.stringify({...data, key: key})
                setMessages((prevMessages) => [...prevMessages, keyedMsg])
            } catch (err) {
                logger.error("Failed to parse message as JSON:", err)
                const keyedMsg = JSON.stringify({data: event.data, key: key})
                setMessages((prevMessages) => [...prevMessages, keyedMsg])
            }
        }

        eventSource.onerror = () => {
            setIsConnected(false)
            setError("Connection lost, attempting to reconnect...")
            eventSource.close()
            handleReconnect()
        }
    }

    const handleReconnect = () => {
        if (reconnectAttemptsRef.current < maxReconnectAttempts) {
            const retryTimeout = 1000 * Math.pow(2, reconnectAttemptsRef.current)
            setTimeout(() => {
                reconnectAttemptsRef.current += 1
                connect()
            }, retryTimeout)
        } else {
            setError("Maximum reconnect attempts reached.")
        }
    }

    useEffect(() => {
        connect()

        return () => {
            eventSourceRef.current?.close()
        }
    }, [url])

    return {isConnected, messages, error}
}

export default useSSE