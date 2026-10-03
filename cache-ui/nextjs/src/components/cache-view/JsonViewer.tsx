"use client"

import {Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger} from "@/components/ui/dialog";
import {Tooltip, TooltipContent, TooltipProvider, TooltipTrigger} from "@/components/ui/tooltip";
import {Button} from "@/components/ui/button";
import {BracesIcon} from "lucide-react";
import CopyToClipboard from "@/components/cache-view/CopyToClipboard";

/**
 * Attempts to parse the given string as a JSON object or array. Returns the
 * pretty-printed JSON when it is a structured value (object/array), otherwise
 * null. Bare primitives (e.g. "123", "true", "hello") are intentionally not
 * treated as JSON worth formatting.
 * @param value The raw string value.
 */
export function tryFormatJson(value: string): string | null {
    const trimmed = value?.trim()
    if (!trimmed || (trimmed[0] !== "{" && trimmed[0] !== "[")) {
        return null
    }
    try {
        const parsed = JSON.parse(trimmed)
        if (parsed !== null && typeof parsed === "object") {
            return JSON.stringify(parsed, null, 2)
        }
    } catch {
        // Not valid JSON - fall through.
    }
    return null
}

/**
 * Applies lightweight, dependency-free syntax highlighting to an already
 * pretty-printed JSON string. The input is HTML-escaped first, so the result is
 * safe to inject. Colours are theme-aware (light/dark).
 * @param json The pretty-printed JSON string.
 */
function highlightJson(json: string): string {
    const escaped = json
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")

    return escaped.replace(
        /("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g,
        (match) => {
            let cls = "text-emerald-600 dark:text-emerald-400" // number
            if (/^"/.test(match)) {
                cls = /:$/.test(match)
                    ? "text-sky-700 dark:text-sky-300" // key
                    : "text-amber-700 dark:text-amber-400" // string
            } else if (/true|false/.test(match)) {
                cls = "text-purple-600 dark:text-purple-400" // boolean
            } else if (/null/.test(match)) {
                cls = "text-rose-600 dark:text-rose-400" // null
            }
            return `<span class="${cls}">${match}</span>`
        }
    )
}

/**
 * A button that opens a dialog showing a value's JSON pretty-printed and
 * syntax-highlighted. Renders nothing when the value is not structured JSON.
 * @param value The raw value string.
 * @constructor
 */
export default function JsonViewer({value}: { value: string }) {
    const formatted = tryFormatJson(value)
    if (formatted === null) {
        return null
    }

    return (
        <Dialog>
            <TooltipProvider>
                <Tooltip>
                    <TooltipTrigger asChild>
                        <DialogTrigger asChild>
                            <Button
                                variant="outline"
                                className="outline px-1.5 py-0 h-6 rounded-sm bg-white shadow-sm hover:bg-aeroncache"
                                data-testid="view-json-button"
                            >
                                <BracesIcon size={10}/>
                            </Button>
                        </DialogTrigger>
                    </TooltipTrigger>
                    <TooltipContent>
                        <p>View JSON</p>
                    </TooltipContent>
                </Tooltip>
            </TooltipProvider>
            <DialogContent className="sm:max-w-2xl" data-testid="json-viewer-dialog">
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-2">
                        <BracesIcon size={16}/>
                        JSON Value
                    </DialogTitle>
                    <DialogDescription>
                        Formatted view of the stored JSON value.
                    </DialogDescription>
                </DialogHeader>
                <div className="relative">
                    <div className="absolute top-2 right-2 z-10">
                        <CopyToClipboard value={formatted} tooltip="Copy JSON" element="JSON"/>
                    </div>
                    <pre
                        className="max-h-[60vh] overflow-y-auto rounded-md border bg-muted/40 p-4 text-sm leading-relaxed font-mono whitespace-pre-wrap break-words"
                        data-testid="json-viewer-content"
                        dangerouslySetInnerHTML={{__html: highlightJson(formatted)}}
                    />
                </div>
            </DialogContent>
        </Dialog>
    )
}
