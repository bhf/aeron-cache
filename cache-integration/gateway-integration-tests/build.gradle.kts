import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult

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

    // Surface each test's outcome in the console/CI log, and fail loudly if none are discovered,
    // so a green build unambiguously proves the e2e tests actually executed.
    failOnNoDiscoveredTests = true
    testLogging {
        events("passed", "skipped", "failed")
    }
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun beforeTest(testDescriptor: TestDescriptor) {}
        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null) {
                logger.lifecycle(
                    "Gateway e2e test summary: ${result.testCount} executed, " +
                        "${result.successfulTestCount} passed, ${result.failedTestCount} failed, " +
                        "${result.skippedTestCount} skipped"
                )
            }
        }
    })

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
