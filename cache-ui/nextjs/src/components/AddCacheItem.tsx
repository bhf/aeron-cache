import {addItemToCacheRequest} from "@/lib/actions";
import Form from "next/form";
import {Input} from "@/components/ui/input";
import {Label} from "@/components/ui/label";
import {Button} from "@/components/ui/button";

interface AddItemProps {
    cacheId: number
}

export default function AddItemRequest(props: AddItemProps) {
    return (
        <Form action={addItemToCacheRequest}>
            <div className="grid gap-4 pt-2">
                <div className="grid gap-2">
                    <Input
                        id="key"
                        type="string"
                        placeholder="Item Key"
                        name="key"
                        required
                    />
                    <Input
                        id="value"
                        type="string"
                        placeholder="Item Value"
                        name="value"
                        required
                    />
                    <Input
                        id="cacheId"
                        type="hidden"
                        name="cacheId"
                        value={props.cacheId}
                    />
                </div>
                <Button type="submit">
                    Add
                </Button>
            </div>
        </Form>
    );
}