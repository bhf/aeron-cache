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

    // Run parameters are overridable via -P properties so different workflows can trade run time
    // against statistical rigour. The defaults below match a full local run; the per-PR CI job
    // passes shorter times for a quick regression signal, while the manually triggered job passes
    // longer times and more forks for publishable numbers.
    fun jmhProp(name: String, default: String) = project.findProperty(name)?.toString() ?: default

    iterations.set(jmhProp("jmhIterations", "1").toInt())
    timeOnIteration.set(jmhProp("jmhTime", "10s"))
    warmupIterations.set(jmhProp("jmhWarmupIterations", "1").toInt())
    warmup.set(jmhProp("jmhWarmup", "10s"))
    fork.set(jmhProp("jmhForks", "1").toInt())

    // Optional benchmark name filters (comma separated regexes) used to shard the run across
    // parallel CI jobs, e.g. -PjmhIncludes=".*Counter.*" or -PjmhExcludes=".*Counter.*".
    (project.findProperty("jmhIncludes") as String?)?.let { value ->
        includes.set(value.split(",").map(String::trim).filter(String::isNotEmpty))
    }
    (project.findProperty("jmhExcludes") as String?)?.let { value ->
        excludes.set(value.split(",").map(String::trim).filter(String::isNotEmpty))
    }

    jvmArgs.set(listOf(
        "-Xms4g",
        "-Xmx4g",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=build/reports/jmh/heapdump.hprof"
    ))
}