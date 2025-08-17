import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog"
import {Github, Linkedin, Star} from "@deemlol/next-icons";
import {ShieldQuestionIcon} from "lucide-react";
import {Button} from "@/components/ui/button";
import Image from "next/image";

export default function AboutAeronCache() {
    return (
        <div>
            <Dialog>
                <DialogTrigger asChild>
                    <Button variant="link"><ShieldQuestionIcon/>About</Button>
                </DialogTrigger>
                <DialogContent className="sm:max-w-[425px] pb-15">
                    <DialogHeader>
                        <DialogTitle>About Aeron Cache</DialogTitle>
                        <DialogDescription>
                            <Image className={"pt-5 pb-5"} src={"/zoom.gif"} width={500} height={296} alt={""}/>
                            A key value store built using Aeron, Agrona and SBE with a HTTP, WS and SSE interface.
                        </DialogDescription>
                    </DialogHeader>
                    <div
                        className="footer mt-auto absolute bottom-0 w-full space-x-15 flex justify-center items-center pb-1 pt-1 bg-linear-to-t from-gray-200 to-gray-100">
                        <div className="space-x-2 flex justify-center items-center">
                            <a href="https://github.com/bhf/aeron-cache" target={"_blank"}>Github</a>
                            <a href="https://github.com/bhf/aeron-cache" target={"_blank"}><Github size={20}
                                                                                                   color="black"/></a>
                        </div>
                        <div className="space-x-2 flex justify-center items-center">
                            <a href="https://www.linkedin.com/in/sanjeevsarda/" target={"_blank"}>LinkedIn</a>
                            <a href="https://www.linkedin.com/in/sanjeevsarda/" target={"_blank"}><Linkedin size={20}
                                                                                                            color="black"/></a>
                        </div>
                        <div className="space-x-2 flex justify-center items-center">
                            <a href="https://sanjeev.pages.dev/" target={"_blank"}>StayTuned</a>
                            <a href="https://sanjeev.pages.dev/" target={"_blank"}><Star size={20} color="black"/></a>
                        </div>
                    </div>

                </DialogContent>
            </Dialog>
        </div>
    )
}