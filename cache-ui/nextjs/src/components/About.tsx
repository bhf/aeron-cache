"use client"

import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog"
import {Github} from "@deemlol/next-icons";
import {ShieldQuestionIcon, ArrowUpCircleIcon, CheckCircle2Icon, GlobeIcon} from "lucide-react";
import {Button} from "@/components/ui/button";
import {Badge} from "@/components/ui/badge";
import Image from "next/image";
import {useEffect, useState} from "react";
import {HEADER_BUTTON_CLASS} from "@/lib/headerStyles";

interface VersionInfo {
    version: string;
    latestVersion: string | null;
    updateAvailable: boolean;
}

const RELEASES_URL = "https://github.com/bhf/aeron-cache/releases";

/**
 * The running version and, when a newer GitHub release exists, an update notice linking to the
 * releases page. Fetched from /api/version when the dialog mounts; renders nothing until it lands.
 */
function VersionInfoRow() {
    const [info, setInfo] = useState<VersionInfo | null>(null);

    useEffect(() => {
        let cancelled = false;
        fetch("/api/version")
            .then((res) => res.json())
            .then((data: VersionInfo) => {
                if (!cancelled) setInfo(data);
            })
            .catch(() => {
                // Leave the row hidden if the check fails; it is non-essential.
            });
        return () => {
            cancelled = true;
        };
    }, []);

    if (!info) return null;

    return (
        <div className="flex items-center justify-between gap-2 pt-1 text-sm text-gray-600">
            <span className="font-mono">Version {info.version}</span>
            {info.updateAvailable ? (
                <a href={RELEASES_URL} target="_blank" rel="noopener noreferrer">
                    <Badge
                        className="bg-amber-100 text-amber-800 border-amber-300 hover:bg-amber-200"
                        title={`Update available: ${info.latestVersion}`}
                    >
                        <ArrowUpCircleIcon/>
                        Update available: {info.latestVersion}
                    </Badge>
                </a>
            ) : (
                <Badge className="bg-emerald-100 text-emerald-800 border-emerald-300">
                    <CheckCircle2Icon/>
                    Up to date
                </Badge>
            )}
        </div>
    );
}

export default function AboutAeronCache() {
    return (
        <div>
            <Dialog>
                <DialogTrigger asChild>
                    <Button variant="link" className={HEADER_BUTTON_CLASS}><ShieldQuestionIcon/>About</Button>
                </DialogTrigger>
                <DialogContent className="sm:max-w-[425px] pb-15">
                    <DialogHeader>
                        <DialogTitle>About Aeron Cache</DialogTitle>
                        <DialogDescription>
                            <Image className={"pt-5 pb-5"} src={"/zoom.gif"} width={500} height={296} alt={""}/>
                            A key value store built using Aeron, Agrona and SBE with a HTTP, WS and SSE interface.
                        </DialogDescription>
                    </DialogHeader>
                    <VersionInfoRow/>
                    <div
                        className="footer mt-auto absolute bottom-0 w-full gap-10 flex justify-center items-center pb-1 pt-1 bg-linear-to-t from-gray-200 to-gray-100">
                        <a href="https://aeron-cache.pages.dev/" target={"_blank"} rel="noopener noreferrer"
                           className="flex items-center gap-2">
                            <GlobeIcon size={20} color="black"/>
                            Aeron Cache
                        </a>
                        <a href="https://github.com/bhf/aeron-cache" target={"_blank"} rel="noopener noreferrer"
                           className="flex items-center gap-2">
                            <Github size={20} color="black"/>
                            Github
                        </a>
                    </div>

                </DialogContent>
            </Dialog>
        </div>
    )
}
