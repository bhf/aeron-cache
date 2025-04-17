plugins {
    application
    alias(libs.plugins.shadow)
}

project.setProperty("mainClassName", "com.bhf.aeroncache.http.application.HttpApplication")

val agent = configurations.create("agent")
val extension = configurations.create("extension")

dependencies {
    implementation(project(":cache-client"))
    implementation(project(":cache-common"))
    implementation(project(":cache-messages-http"))
    implementation(project(":cache-messages-sbe"))
    implementation(libs.aeron)
    implementation(libs.javalin)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation("org.slf4j:slf4j-simple:2.0.16")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")

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
}

tasks.compileJava{
    options.compilerArgs.add("--enable-preview")
}

tasks.build {
    dependsOn(copyAgent)
    dependsOn(copyExtension)
}