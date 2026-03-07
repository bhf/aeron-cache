plugins {
    id("java")
}


dependencies {
    testImplementation(libs.restassured)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit)
    testImplementation(libs.junit.params)
    testImplementation(libs.json)
    testImplementation(libs.hamcrest)
    testImplementation(libs.jackson.core)
    testImplementation(libs.awaitility)
    testImplementation(libs.okhttp)
    testImplementation(libs.okhttp.sse)

    testImplementation(project(":cache-cluster"))
    testImplementation(project(":cache-http:http-server-javalin"))
    testImplementation(project(":cache-common"))
    testImplementation(project(":cache-integration:integration-tests-common"))

    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-preview")
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-preview")
}

tasks.test {
    jvmArgs("--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.util.zip=ALL-UNNAMED")
    jvmArgs("--enable-preview")
    systemProperty("aeron.dir.delete.on.shutdown", "true")

    environment(loadTestEnv())
    useJUnitPlatform()
    finalizedBy("cleanTestNodes")
}

tasks.register<Delete>("cleanTestNodes") {
    delete("backend_http_sse_0", "backend_http_sse_1", "backend_http_sse_2")
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