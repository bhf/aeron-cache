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
    return (
        <AlertDialog>
            <AlertDialogTrigger className={"outline px-2 rounded-md text-lg"}>
                {props.buttonText}
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