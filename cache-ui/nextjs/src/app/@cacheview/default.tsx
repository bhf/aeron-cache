import Page from "@/app/@cacheview/page";

export default async function Default({
                                          params,
                                      }: {
    params: Promise<{ slug: string }>
}) {
    return (
        <Page/>
    )
}
