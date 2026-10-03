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

    // The soak harness drives a long, single-writer workload against one in-process cluster + gateway
    // bound to fixed endpoints (7075/7076) with static singleton state. Fork a fresh JVM for the suite
    // so it cannot collide with anything else, matching the gateway e2e module.
    setForkEvery(1)

    // Everything runs in one JVM (a 3-node in-process RAFT cluster + the gateway + the client), so size
    // the heap for the smallest standard GitHub runner (private repos: 8GB RAM / 2 CPU; public: 16GB / 4).
    // A deliberately modest cap also makes a genuine leak OOM *sooner* - a clearer soak signal - and the
    // heap dump (written to the 14GB runner disk) is the artifact for triage. Overridable via -Psoak.maxHeap.
    val soakMaxHeap = (project.findProperty("soak.maxHeap") as String?)?.takeIf { it.isNotBlank() } ?: "2g"
    doFirst { layout.buildDirectory.dir("reports/soak").get().asFile.mkdirs() }
    jvmArgs("-Xmx$soakMaxHeap")
    jvmArgs("-XX:+HeapDumpOnOutOfMemoryError")
    jvmArgs("-XX:HeapDumpPath=build/reports/soak")

    // Run parameters are overridable via -Psoak.* so the manually triggered workflow can set the run
    // duration and workload shape at dispatch time. Anything left unset falls back to the defaults in
    // SoakConfig. Blank values (e.g. an unset "seed" input) are ignored so the default still applies.
    fun soakProp(name: String) = (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() }
    listOf(
        "soak.durationSeconds",
        "soak.seed",
        "soak.kvCacheCount",
        "soak.counterCacheCount",
        "soak.keySpace",
        "soak.valueSizeBytes",
        "soak.verifyEvery",
        "soak.opTimeoutSeconds"
    ).forEach { prop -> soakProp(prop)?.let { systemProperty(prop, it) } }

    // Surface each test's outcome in the console/CI log, and fail loudly if none are discovered, so a
    // green build unambiguously proves the soak actually executed.
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
                    "Soak test summary: ${result.testCount} executed, " +
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
        "soak_core_0",
        "soak_core_1",
        "soak_core_2"
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
