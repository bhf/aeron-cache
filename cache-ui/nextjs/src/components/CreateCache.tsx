import {createCacheRequest} from "@/lib/actions";
import Form from "next/form";
import {Input} from "@/components/ui/input";
import {Label} from "@/components/ui/label";
import {Button} from "@/components/ui/button";

export default function CreateCacheRequest() {
    return (
        <Form action={createCacheRequest}>
            <div className="grid gap-4">
                <div className="grid gap-2">
                    <Label htmlFor="Cache ID">Create Cache</Label>
                    <Input
                        id="cacheId"
                        type="number"
                        min={1}
                        placeholder="Numeric cache ID"
                        name="cacheId"
                        required
                    />
                </div>
                <Button type="submit">
                    Create Cache
                </Button>
            </div>
        </Form>
    );
}