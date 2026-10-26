import Page from "@/app/(main)/@dashboard/page";

export default async function Default({
                                          params,
                                      }: {
    params: Promise<{ slug: string }>
}) {
    return (
        <Page/>
    )
}
