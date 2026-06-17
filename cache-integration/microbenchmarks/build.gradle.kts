plugins {
    id("java")
    alias(libs.plugins.jmh)
}

group = "com.bhf.aeroncache"
version = "0.0.31-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.aeron)

    implementation(project(":cache-spi"))
    implementation(project(":cache-common"))
    implementation(project(":cache-cluster"))
    implementation(project(":cache-spi-impl:map-cache:map-cache-core"))
    implementation(project(":cache-spi-impl:map-cache:map-cache-test"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit)
    testImplementation(libs.junit.params)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockito)
    testImplementation(libs.mockito.junit)
}

tasks.test {
    useJUnitPlatform()
}

tasks.jmh {
    profilers.add("gc")
    resultFormat.set("JSON")
    resultsFile.set(layout.buildDirectory.file("reports/jmh/results.json"))
    
    val pMode = project.findProperty("jmhMode")?.toString() ?: "throughput"
    val mappedMode = if (pMode == "latency") "sample" else "thrpt"
    benchmarkMode.set(listOf(mappedMode))
    
    iterations.set(1)
    timeOnIteration.set("10s")
    warmupIterations.set(1)
    warmup.set("10s")

    jvmArgs.set(listOf(
        "-Xms4g",
        "-Xmx4g",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=build/reports/jmh/heapdump.hprof"
    ))
}