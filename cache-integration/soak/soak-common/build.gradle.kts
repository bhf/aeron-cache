plugins {
    id("java-library")
}

// Shared soak-test support library: the gateway client lifecycle harness, the recording listener, the
// run report and property helpers reused by every soak suite (core-cache, streaming, ...). Mirrors how
// integration-common holds the reusable integration-test bodies. Consumers depend on this from their test
// source set; the dependencies below are `api` so they reach the consumer's test classpath transitively.
dependencies {
    api(platform(libs.junit.bom))
    api(libs.junit)
    api(libs.awaitility)

    api(libs.aeron)
    api(libs.log4j.api)
    api(libs.log4j.core)

    api(project(":cache-common"))
    api(project(":cache-aeron-gateway:aeron-gateway-client"))
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("--enable-preview")
}
