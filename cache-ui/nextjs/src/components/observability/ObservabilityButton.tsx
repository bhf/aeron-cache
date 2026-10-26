"use client"

import {Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger} from "@/components/ui/dialog"
import {Button} from "@/components/ui/button"
import {ActivityIcon, ExternalLinkIcon} from "lucide-react"
import ObservabilityDashboard from "@/components/observability/ObservabilityDashboard"

/**
 * Menu button that opens the observability dashboard in a modal, with a neighbouring icon that opens
 * it in a new tab. The dashboard only polls while the dialog is open, because the dialog content is
 * unmounted when it closes.
 */
export default function ObservabilityButton() {
    return (
        <div className="flex items-start">
            <Dialog>
                <DialogTrigger asChild>
                    <Button variant="link" data-testid="menu-link-observability"><ActivityIcon/>Observability</Button>
                </DialogTrigger>
                <DialogContent className="w-[95vw] max-w-[1400px] sm:max-w-[1400px] max-h-[90vh] overflow-y-auto">
                    <DialogHeader>
                        <DialogTitle>Observability</DialogTitle>
                        <DialogDescription>
                            Cache statistics and Aeron transport, cluster and archive counters
                        </DialogDescription>
                    </DialogHeader>
                    <ObservabilityDashboard/>
                </DialogContent>
            </Dialog>
            <a href="/observability" target="_blank" rel="noopener noreferrer"
               title="Open observability in a new tab" aria-label="Open observability in a new tab"
               data-testid="observability-open-new-tab"
               className="-ml-3 flex h-9 items-center px-2 text-primary hover:opacity-70">
                <ExternalLinkIcon className="size-4"/>
            </a>
        </div>
    )
}
