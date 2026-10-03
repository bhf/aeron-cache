"use client"

import {Tooltip, TooltipContent, TooltipProvider, TooltipTrigger} from "@/components/ui/tooltip";
import {cn} from "@/lib/utils";

/**
 * Displays a potentially long string truncated to a single line, revealing the
 * full value in a tooltip on hover. The visible width is responsive so the
 * table stays readable on smaller screens.
 * @param text The full text to display.
 * @param className Optional extra classes for the visible (truncated) span.
 * @constructor
 */
export default function TruncatedText({text, className}: { text: string, className?: string }) {
    return (
        <TooltipProvider>
            <Tooltip>
                <TooltipTrigger asChild>
                    <span
                        className={cn(
                            "block max-w-[140px] truncate align-middle md:max-w-[280px]",
                            className
                        )}
                        data-testid="truncated-text"
                    >
                        {text}
                    </span>
                </TooltipTrigger>
                <TooltipContent className="max-w-sm">
                    <p className="break-all whitespace-pre-wrap">{text}</p>
                </TooltipContent>
            </Tooltip>
        </TooltipProvider>
    )
}
