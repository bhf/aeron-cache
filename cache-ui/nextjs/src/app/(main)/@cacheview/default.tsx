import Page from "@/app/(main)/@cacheview/page";

export default async function Default({
                                          params,
                                      }: {
    params: Promise<{ slug: string }>
}) {
    return (
        <Page/>
    )
}
