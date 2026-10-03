import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult

plugins {
    id("java")
}

dependencies {
    // Shared soak harness (lifecycle, recording listener, report) + its api deps (junit, awaitility,
    // aeron, log4j, cache-common, gateway-client).
    testImplementation(project(":cache-integration:soak:soak-common"))

    // Backend the gateway runs against: the in-process RAFT cluster and the gateway server edge.
    testImplementation(project(":cache-cluster"))
    testImplementation(project(":cache-aeron-gateway:aeron-gateway-server"))

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

    // One in-process cluster + gateway + client bound to fixed endpoints with static singleton state;
    // fork a fresh JVM for the suite so it cannot collide with anything else.
    setForkEvery(1)

    // Everything runs in one JVM; size the heap for the smallest standard GitHub runner (8GB/2CPU) and
    // let a genuine leak OOM sooner. Overridable via -Psoak.maxHeap. Heap dump -> the 14GB runner disk.
    val soakMaxHeap = (project.findProperty("soak.maxHeap") as String?)?.takeIf { it.isNotBlank() } ?: "2g"
    doFirst { layout.buildDirectory.dir("reports/soak").get().asFile.mkdirs() }
    jvmArgs("-Xmx$soakMaxHeap")
    jvmArgs("-XX:+HeapDumpOnOutOfMemoryError")
    jvmArgs("-XX:HeapDumpPath=build/reports/soak")

    // Run parameters overridable via -Psoak.* so the manually triggered workflow can set the run duration
    // and workload shape at dispatch time. Blank values are ignored so defaults in StreamingSoakConfig apply.
    fun soakProp(name: String) = (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() }
    listOf(
        "soak.durationSeconds",
        "soak.seed",
        "soak.mutationsPerRound",
        "soak.hydrationEntries",
        "soak.wholeCacheKeys",
        "soak.opTimeoutSeconds",
        "soak.includeCounters"
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
