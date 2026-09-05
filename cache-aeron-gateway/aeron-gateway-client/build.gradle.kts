plugins {
    `java-library`
    application
}

val sbeToolConfig by configurations.creating

dependencies {
    api(project(":cache-common"))
    api(libs.aeron)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)

    sbeToolConfig(libs.sbetool)
    implementation(libs.sbetool)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit)
    testImplementation(libs.junit.params)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockito)
    testImplementation(libs.mockito.junit)
    testImplementation(libs.awaitility)
}

val generatedSbeSourceDir = layout.buildDirectory.dir("generated/sources/sbe/main/java")

sourceSets {
    main {
        java {
            srcDir(generatedSbeSourceDir)
        }
    }
}

tasks.register<JavaExec>("generateSbeCodecs") {
    mainClass.set("uk.co.real_logic.sbe.SbeTool")
    classpath = sbeToolConfig
    val outputDir = generatedSbeSourceDir
    // Single source of truth: reuse the gateway server's wire-protocol schema.
    val schemaFile = layout.projectDirectory.file("../aeron-gateway-server/src/main/resources/sbe/gateway-schema.xml")
    systemProperty("sbe.output.dir", outputDir.get().asFile.absolutePath)
    args(schemaFile.asFile.absolutePath)
    inputs.file(schemaFile)
    outputs.dir(outputDir)
}

tasks.compileJava {
    dependsOn("generateSbeCodecs")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.util.zip=ALL-UNNAMED")
}

application {
    // Local manual test driver: creates a random cache and publishes random key/values on an interval.
    mainClass.set("com.bhf.aeroncache.gateway.client.Main")
    applicationDefaultJvmArgs = listOf(
        "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
        "--add-opens", "java.base/java.util.zip=ALL-UNNAMED"
    )
}
