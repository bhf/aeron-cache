plugins {
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.jib)
}

application {
    mainClass.set("com.bhf.aeroncache.gateway.application.GatewayApplication")
}

val sbeToolConfig by configurations.creating

val agent = configurations.create("agent")
val extension = configurations.create("extension")

dependencies {
    implementation(project(":cache-client"))
    implementation(project(":cache-common"))
    implementation(project(":cache-messages-http"))
    implementation(project(":cache-spi"))
    runtimeOnly(project(":cache-spi-impl:map-cache:map-cache-core"))
    implementation(libs.aeron)
    implementation(libs.javalin)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(libs.slf4j)
    implementation(libs.jackson.core)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.exporter.otlp)
    implementation(libs.micrometer.prometheus)
    implementation(libs.micrometer.javalin)

    sbeToolConfig(libs.sbetool)
    implementation(libs.sbetool)

    agent("io.opentelemetry.javaagent:opentelemetry-javaagent:2.15.0")
    extension("io.opentelemetry.contrib:opentelemetry-samplers:1.46.0-alpha") {
        isTransitive = false
    }

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
    val schemaFile = layout.projectDirectory.file("src/main/resources/sbe/gateway-schema.xml")
    systemProperty("sbe.output.dir", outputDir.get().asFile.absolutePath)
    args(schemaFile.asFile.absolutePath)
    inputs.file(schemaFile)
    outputs.dir(outputDir)
}

tasks.compileJava {
    dependsOn("generateSbeCodecs")
    options.compilerArgs.add("--enable-preview")
}

tasks.compileTestJava {
    options.compilerArgs.add("--enable-preview")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveFileName.set("aeron-gateway-server-all.jar")
}

val copyAgent = tasks.register<Copy>("copyAgent") {
    from(agent.singleFile)
    into(layout.buildDirectory.dir("agent"))
    rename("opentelemetry-javaagent-.*\\.jar", "opentelemetry-javaagent.jar")
}

val copyExtension = tasks.register<Copy>("copyExtension") {
    from(extension.singleFile)
    into(layout.buildDirectory.dir("agent"))
    rename(".*\\.jar", "opentelemetry-javaagent-extension.jar")
}

apply(plugin = "com.google.cloud.tools.jib")

configure<com.google.cloud.tools.jib.gradle.JibExtension> {
    from {
        image = "docker://eclipse-temurin:25-jre"
    }
    to {
        val reg = project.findProperty("dockerRegistry")?.toString() ?: ""
        image = if (reg.isEmpty()) "aeroncache-gateway" else "$reg/aeroncache-gateway"
        tags = setOf(project.version.toString(), "latest")
    }
    container {
        mainClass = "com.bhf.aeroncache.gateway.application.GatewayApplication"
        jvmFlags = listOf(
            "--enable-preview",
            "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
        )
        ports = listOf("7073", "7075")
    }
    extraDirectories {
        paths {
            path {
                setFrom(layout.buildDirectory.dir("agent"))
                into = "/app/agent"
            }
        }
    }
}

tasks.named("jib") {
    dependsOn(copyAgent, copyExtension)
}

tasks.named("jibDockerBuild") {
    dependsOn(copyAgent, copyExtension)
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
    jvmArgs("--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.util.zip=ALL-UNNAMED")
}

tasks.build {
    dependsOn(copyAgent)
    dependsOn(copyExtension)
}
