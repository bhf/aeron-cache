import {Tooltip, TooltipContent, TooltipProvider, TooltipTrigger} from "@/components/ui/tooltip";
import {Button} from "@/components/ui/button";
import {ClipboardCopyIcon} from "lucide-react";
import {toast} from "sonner";

export default function CopyToClipboard(props: { value: string, tooltip: string, element: string}) {

    function toastCopied(element: string) {
        const props = {title: "Copied", description: "Copied "+element, actionLabel: "OK"};

        toast.info(props.title, {
            description: props.description,
            position: 'top-center',
            duration: 750,
        })
    }

    return (
        <TooltipProvider>
            <Tooltip>
                <TooltipTrigger asChild>
                    <Button variant="outline" className={"outline px-2 py-1 rounded-sm text-lg bg-white shadow-md hover:bg-aeroncache"} onClick={
                        () => {
                            navigator.clipboard.writeText(props.value)
                            toastCopied(props.element)
                        }
                    }>
                        <ClipboardCopyIcon/>
                    </Button>
                </TooltipTrigger>
                <TooltipContent>
                    <p>{props.tooltip}</p>
                </TooltipContent>
            </Tooltip>
        </TooltipProvider>
    )
}