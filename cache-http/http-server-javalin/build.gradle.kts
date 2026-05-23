plugins {
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.jib)
}

application {
    mainClass.set("com.bhf.aeroncache.http.application.HttpApplication")
}

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

    agent("io.opentelemetry.javaagent:opentelemetry-javaagent:2.15.0")
    extension("io.opentelemetry.contrib:opentelemetry-samplers:1.46.0-alpha") {
        isTransitive = false
    }
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

tasks.test {
    useJUnitPlatform()
    jvmArgs("--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.util.zip=ALL-UNNAMED")
}

tasks.compileJava{
    options.compilerArgs.add("--enable-preview")
}

tasks.build {
    dependsOn(copyAgent)
    dependsOn(copyExtension)
}

apply(plugin = "com.google.cloud.tools.jib")

configure<com.google.cloud.tools.jib.gradle.JibExtension> {
    from {
        image = "docker://eclipse-temurin:25-jre"
    }
    to {
        val reg = project.findProperty("dockerRegistry")?.toString() ?: ""
        image = if (reg.isEmpty()) "aeroncache-http" else "$reg/aeroncache-http"
        tags = setOf(project.version.toString(), "latest")
    }
    container {
        mainClass = "com.bhf.aeroncache.http.application.HttpApplication"
        jvmFlags = listOf(
            "--enable-preview",
            "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
        )
        ports = listOf("8080")
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
