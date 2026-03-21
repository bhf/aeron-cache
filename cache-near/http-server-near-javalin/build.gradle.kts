plugins {
    application
    alias(libs.plugins.shadow)
}

project.setProperty("mainClassName", "com.bhf.aeroncache.http.application.NearCacheApplication")

val agent = configurations.create("agent")
val extension = configurations.create("extension")

dependencies {
    implementation(project(":cache-client"))
    implementation(project(":cache-common"))
    implementation(project(":cache-messages-http"))
    implementation(project(":cache-messages-sbe"))
    implementation(project(":cache-ws:ws-server-javalin"))
    implementation(project(":cache-spi"))
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