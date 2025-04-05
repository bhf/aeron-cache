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
import {RecycleIcon, TrashIcon} from "lucide-react";

interface ConfirmingDialogProps {
    title: string
    message: string
    cancelText: string
    actionText: string
    buttonText: string
    action: () => Promise<any>
}

/**
 *
 * @constructor
 */
export function ConfirmingDialog(props: ConfirmingDialogProps) {

    const isDelete = props.buttonText=="Delete"
    const isClear = props.buttonText=="Clear"
    let actionTriggerElement
    if (isDelete) {
        actionTriggerElement = <TrashIcon className="size-5"/>
    }
    else if (isClear) {
        actionTriggerElement = <RecycleIcon className="size-5"/>
    }


    return (
        <AlertDialog>
            <AlertDialogTrigger className={"outline px-2 rounded-md text-lg"}>
                {isDelete||isClear ? actionTriggerElement : props.buttonText}
            </AlertDialogTrigger>
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