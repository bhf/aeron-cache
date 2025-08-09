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
                        <div>
                            <span className="text-[#9cdcfe]">$ </span>
                            <span>{message}</span>
                        </div>
                    )
            }
        )
    }

    return (
        <div
            className="flex flex-col h-[800px] w-full max-w-3xl bg-[#1e1e1e] rounded-lg overflow-hidden font-mono text-white">
            <div className="flex-1 overflow-auto p-2">
                <div className="space-y-2">
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