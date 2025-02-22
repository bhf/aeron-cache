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