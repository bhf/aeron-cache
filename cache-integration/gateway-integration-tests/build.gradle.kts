plugins {
    id("java")
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit)
    testImplementation(libs.junit.params)
    testImplementation(libs.awaitility)

    testImplementation(libs.aeron)
    testImplementation(libs.log4j.api)
    testImplementation(libs.log4j.core)

    testImplementation(project(":cache-cluster"))
    testImplementation(project(":cache-common"))
    testImplementation(project(":cache-aeron-gateway:aeron-gateway-server"))
    testImplementation(project(":cache-aeron-gateway:aeron-gateway-client"))

    // Cache SPI implementation so the gateway's ServiceLoader can find a CacheClientFactory.
    testRuntimeOnly(project(":cache-spi-impl:map-cache:map-cache-core"))
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("--enable-preview")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
    jvmArgs("--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.util.zip=ALL-UNNAMED")
    systemProperty("aeron.dir.delete.on.shutdown", "true")
    systemProperty("aeron.cluster.message.timeout", "30000000000")

    environment(loadTestEnv())
    finalizedBy("cleanTestNodes")
}

tasks.register<Delete>("cleanTestNodes") {
    delete(
        "gateway_e2e_0",
        "gateway_e2e_1",
        "gateway_e2e_2"
    )
}

fun loadTestEnv(): Map<String, String> {
    val resource = sourceSets["test"]
        .resources
        .srcDirs
        .map { it.resolve("test.env") }
        .firstOrNull { it.exists() }
        ?: return emptyMap()

    return resource.readLines()
        .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("//") && it.contains("=") }
        .associate {
            val (key, value) = it.split("=", limit = 2)
            key.trim() to value.trim()
        }
}
