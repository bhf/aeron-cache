import Page from "@/app/@cacheview/cache/[slug]/page";

export default async function Default({
                                          params,
                                      }: {
    params: Promise<{ slug: string }>
}) {
    return (
        <Page params={params}/>
    )
}
