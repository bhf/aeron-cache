plugins {
    application
    alias(libs.plugins.jmh)
}

dependencies {
    implementation(libs.aeron)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(project(":cache-messages"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit)
    testImplementation(libs.junit.params)
}

tasks.test {
    useJUnitPlatform()
}

jmh {
    warmupIterations = 1
    iterations = 1
    fork = 1
}