import {Status, StatusIndicator, StatusLabel} from "@/components/ui/shadcn-io/status";

interface TerminalProps {
    messages?: string[],
    isConnected?: boolean,
    errorMsg?: string | null
}

export default function Terminal({messages, isConnected, errorMsg}: TerminalProps) {

    let responses
    if (messages) {
        responses = messages.map((message, index) => {
                if (messages.length < 14 || index >= messages.length - 14)
                    return (
                        <div key={index}>
                            <span className="text-[#9cdcfe]">$ </span>
                            <span>{message}</span>
                        </div>
                    )
            }
        )
    }

    let statusComponent
    if (isConnected) {
        statusComponent = <Status
            className="gap-4 rounded-full px-6 py-2 text-sm"
            status="online"
            variant="outline">

            <StatusIndicator/>
            <StatusLabel className="font-mono">Connected</StatusLabel>
        </Status>
    } else {
        statusComponent = <Status
            className="gap-4 rounded-full px-6 py-2 text-sm"
            status="offline"
            variant="outline">

            <StatusIndicator/>
            <StatusLabel className="font-mono">Offline</StatusLabel>
        </Status>
    }

    return (
        <div
            className="flex flex-col h-[800px] w-full max-w-3xl bg-[#1e1e1e] rounded-lg overflow-hidden font-mono text-white">
            <div className="flex-1 overflow-auto p-2">
                <div className="space-y-2">
                    {statusComponent}
                    {responses}
                    <div className="flex items-center gap-2">
                        <span className="text-[#9cdcfe]">user@aeroncache</span>
                        <span className="text-[#ce9178]">~</span>
                        <span className="animate-blink text-[#d4d4d4]">_</span>
                    </div>
                </div>
            </div>
        </div>
    )
}