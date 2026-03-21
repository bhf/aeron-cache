plugins {
    application
    alias(libs.plugins.jmh)
    alias(libs.plugins.shadow)
}

project.setProperty("mainClassName", "com.bhf.aeroncache.application.BasicPerfTest")

dependencies {
    implementation(project(":cache-messages-sbe"))
    implementation(project(":cache-cluster"))
    implementation(project(":cache-common"))
    implementation(project(":cache-spi"))
    implementation(libs.aeron)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)

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

jmh {
    warmupIterations = 1
    iterations = 5
    fork = 1
}