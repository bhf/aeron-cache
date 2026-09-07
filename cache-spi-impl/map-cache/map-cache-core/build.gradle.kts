plugins {
    id("java")
    alias(libs.plugins.jmh)
}

group = "com.bhf.aeroncache"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(libs.aeron)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(libs.jackson.core)

    implementation(project(":cache-spi"))
    implementation(project(":cache-common"))
    implementation(project(":cache-spi-impl:map-cache:map-cache-sbe"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit)
    testImplementation(libs.junit.params)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockito)
    testImplementation(libs.mockito.junit)
    testImplementation(project(":cache-client"))
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.util.zip=ALL-UNNAMED")
}

jmh {
    warmupIterations = 1
    iterations = 5
    fork = 1
}