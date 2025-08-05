import {Card, CardContent, CardHeader, CardTitle} from "@/components/ui/card";
import {SearchCodeIcon, ZapIcon} from "lucide-react";

export default async function Page() {

    return (
        <div>
            <div className="pt-4">
                <div className="pb-6 px-6">
                    <Card className={"shadow-lg"}>
                        <CardHeader>
                            <CardTitle>Welcome to Aeron Cache</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <div className="mb-2">
                                <div>
                                    <div className="flex flex-row space-x-2 mb-2">
                                            No caches? Go ahead and create one using the "Create Cache" button!
                                    </div>
                                </div>
                                <div>
                                    <div className="flex flex-row space-x-2 mb-2">
                                        <div>
                                            <SearchCodeIcon size={20}/>
                                        </div>
                                        <div>
                                            Click to view a cache.
                                        </div>
                                    </div>
                                </div>
                                <div>
                                    <div className="flex flex-row space-x-2">
                                        <div>
                                            <ZapIcon size={20}/>
                                        </div>
                                        <div>
                                            Click to stream via websockets.
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </CardContent>
                    </Card>
                </div>
            </div>
        </div>
    )
}