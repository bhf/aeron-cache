plugins {
    application
    alias(libs.plugins.jmh)
    alias(libs.plugins.shadow)
}

project.setProperty("mainClassName", "com.bhf.aeroncache.application.ClusterNodeApplication")

dependencies {
    implementation(libs.aeron)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(project(":cache-messages-sbe"))
    implementation(project(":cache-common"))
    implementation("io.opentelemetry:opentelemetry-api:1.49.0")
    implementation("io.opentelemetry:opentelemetry-sdk:1.49.0")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.49.0")

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit)
    testImplementation(libs.junit.params)
    testImplementation(libs.mockito)

}

tasks.test {
    useJUnitPlatform()
}

jmh {
    warmupIterations = 1
    iterations = 1
    fork = 1
}