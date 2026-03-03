plugins {
    id("java")
}


dependencies {
    implementation(libs.restassured)
    implementation(libs.json)
    implementation(libs.junit)
    implementation(libs.junit.params)
    implementation(libs.junit.api)

    implementation(project(":cache-cluster"))
    implementation(project(":cache-http:http-server-javalin"))
    implementation(project(":cache-common"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.hamcrest)
    testImplementation(libs.jackson.core)

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
    delete("node0", "node1", "node2")
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