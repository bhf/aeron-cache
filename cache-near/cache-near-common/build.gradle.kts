plugins {
    `java-library`
}

dependencies {
    api(project(":cache-messages-http"))
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit)
    testImplementation(libs.junit.params)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
