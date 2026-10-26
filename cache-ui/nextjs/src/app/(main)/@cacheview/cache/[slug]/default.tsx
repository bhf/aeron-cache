import Page from "@/app/(main)/@cacheview/cache/[slug]/page";

export default async function Default({
                                          params,
                                      }: {
    params: Promise<{ slug: string }>
}) {
    return (
        <Page params={params}/>
    )
}
