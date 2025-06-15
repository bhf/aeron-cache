import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
    AlertDialogTrigger,
} from "@/components/ui/alert-dialog"
import {DeleteIcon, RecycleIcon, Trash2Icon} from "lucide-react";
import {Tooltip, TooltipContent, TooltipProvider, TooltipTrigger,} from "@/components/ui/tooltip"

interface ConfirmingDialogProps {
    title: string
    message: string
    cancelText: string
    actionText: string
    buttonText: string
    action: () => Promise<never>
}

/**
 *
 * @constructor
 */
export function ConfirmingDialog(props: ConfirmingDialogProps) {

    const isDelete = props.buttonText=="Delete"
    const isClear = props.buttonText=="Clear"
    const isRemove = props.buttonText=="Remove"

    let actionTriggerElement
    if (isDelete) {
        actionTriggerElement = <Trash2Icon className="size-5"/>
    }
    else if (isClear) {
        actionTriggerElement = <RecycleIcon className="size-5"/>
    }
    else if (isRemove) {
        actionTriggerElement = <DeleteIcon className="size-5"/>
    }

    const isCacheAction = isDelete || isClear || isRemove;

    return (
        <AlertDialog>


            <TooltipProvider>
                <Tooltip>
                    <TooltipTrigger asChild>
                        <AlertDialogTrigger className={"outline px-2 py-1 rounded-sm text-lg bg-white shadow-md hover:bg-aeroncache"}>
                            {isCacheAction ? actionTriggerElement : props.buttonText}
                        </AlertDialogTrigger>
                    </TooltipTrigger>
                    <TooltipContent>
                        <p>{props.buttonText}</p>
                    </TooltipContent>
                </Tooltip>
            </TooltipProvider>



            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>{props.title}</AlertDialogTitle>
                    <AlertDialogDescription>
                        {props.message}
                    </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                    <AlertDialogCancel>{props.cancelText}</AlertDialogCancel>
                    <AlertDialogAction onClick={props.action}>{props.actionText}</AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
}