import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult

plugins {
    id("java")
}

dependencies {
    testImplementation(project(":cache-integration:soak:soak-common"))
    testImplementation(project(":cache-cluster"))
    testImplementation(project(":cache-aeron-gateway:aeron-gateway-server"))
    testImplementation(project(":cache-http:http-clustertools"))
    testImplementation(project(":cache-spi"))
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

    setForkEvery(1)

    val soakMaxHeap = (project.findProperty("soak.maxHeap") as String?)?.takeIf { it.isNotBlank() } ?: "2g"
    doFirst { layout.buildDirectory.dir("reports/soak").get().asFile.mkdirs() }
    jvmArgs("-Xmx$soakMaxHeap")
    jvmArgs("-XX:+HeapDumpOnOutOfMemoryError")
    jvmArgs("-XX:HeapDumpPath=build/reports/soak")

    fun soakProp(name: String) = (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() }
    listOf(
        "soak.durationSeconds",
        "soak.seed",
        "soak.mutationsPerRound",
        "soak.wholeCacheKeys",
        "soak.opTimeoutSeconds",
        "soak.includeCounters",
        "soak.includeSynthetic"
    ).forEach { prop -> soakProp(prop)?.let { systemProperty(prop, it) } }

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
                    "Streaming soak test summary: ${result.testCount} executed, " +
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
        "soak_stream_0",
        "soak_stream_1",
        "soak_stream_2"
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
