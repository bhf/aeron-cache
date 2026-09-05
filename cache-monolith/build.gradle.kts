plugins {
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.jib)
}

application {
    mainClass.set("com.bhf.aeroncache.monolith.application.Main")
}

dependencies {
    implementation(project(":cache-cluster"))
    implementation(project(":cache-common"))
    implementation(project(":cache-http:http-server-javalin"))
    implementation(project(":cache-http:http-clustertools"))
    implementation(project(":cache-ws:ws-server-javalin"))
    implementation(project(":cache-sse:sse-server-jooby"))
    implementation(project(":cache-aeron-gateway:aeron-gateway-server"))
    runtimeOnly(project(":cache-spi-impl:map-cache:map-cache-core"))

    implementation(libs.aeron)
    implementation(libs.jooby.kotlin)
    implementation(libs.jooby.netty)
    implementation(libs.jooby.jackson)

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.compileJava {
    options.compilerArgs.add("--enable-preview")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.bhf.aeroncache.monolith.application.Main"
    }
}

tasks.shadowJar {
    archiveClassifier.set("all")
    manifest {
        attributes["Main-Class"] = "com.bhf.aeroncache.monolith.application.Main"
    }
}

apply(plugin = "com.google.cloud.tools.jib")

configure<com.google.cloud.tools.jib.gradle.JibExtension> {
    from {
        image = "docker://eclipse-temurin:25-jre"
    }
    to {
        val reg = project.findProperty("dockerRegistry")?.toString() ?: ""
        image = if (reg.isEmpty()) "aeroncache-monolith" else "$reg/aeroncache-monolith"
        tags = setOf(project.version.toString(), "latest")
    }
    container {
        mainClass = "com.bhf.aeroncache.monolith.application.Main"
        jvmFlags = listOf(
            "--enable-preview",
            "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
        )
        ports = listOf("8080")
    }
}