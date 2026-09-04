plugins {
    id("java")
}

group = "com.bhf.aeroncache"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(libs.aeron)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)

    implementation(platform(libs.junit.bom))
    implementation(libs.junit)
    implementation(libs.junit.params)
    implementation(libs.junit.platform.launcher)
    implementation(libs.mockito)
    implementation(libs.mockito.junit)

    implementation(project(":cache-common"))
    implementation(project(":cache-client"))
    implementation(project(":cache-cluster"))
    implementation(project(":cache-spi"))
    implementation(project(":cache-spi-impl:map-cache:map-cache-core"))

    testImplementation(project(":cache-common"))
    testImplementation(project(":cache-cluster"))
    testImplementation(project(":cache-spi"))
    testImplementation(project(":cache-spi-impl:map-cache:map-cache-core"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit)
    testImplementation(libs.junit.params)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockito)
    testImplementation(libs.mockito.junit)

}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.util.zip=ALL-UNNAMED")
}