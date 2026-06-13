plugins {
    application
    alias(libs.plugins.jib)
}

application {
    mainClass.set("com.bhf.aeroncache.clustertools.application.ClusterToolsHTTPApplication")
}

dependencies {
    implementation(libs.javalin)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(libs.slf4j)
    implementation(libs.jackson.core)
    implementation(libs.micrometer.prometheus)
    implementation(libs.micrometer.javalin)
    implementation(libs.aeron)
    implementation(project(":cache-common"))
    implementation(project(":cache-messages-http"))
}

tasks.test {
    useJUnitPlatform()
}

apply(plugin = "com.google.cloud.tools.jib")

configure<com.google.cloud.tools.jib.gradle.JibExtension> {
    from {
        image = "docker://eclipse-temurin:25-jre"
    }
    to {
        val reg = project.findProperty("dockerRegistry")?.toString() ?: ""
        image = if (reg.isEmpty()) "aeroncache-http-clustertools" else "$reg/aeroncache-http-clustertools"
        tags = setOf(project.version.toString(), "latest")
    }
    container {
        mainClass = "com.bhf.aeroncache.clustertools.application.ClusterToolsHTTPApplication"
        jvmFlags = listOf(
            "--enable-preview",
            "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
        )
        // 7080 matches the clustertools liveness/readiness probes in aeroncache-cluster values.yaml
        ports = listOf("7080")
    }
}