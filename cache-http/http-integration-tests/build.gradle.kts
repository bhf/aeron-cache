plugins {
    id("java")
}


dependencies {
    testImplementation(libs.restassured)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit)
    testImplementation(libs.junit.params)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation("org.json:json:20240303")
    testImplementation("org.hamcrest:hamcrest:2.1")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    testImplementation(project(":cache-common"))
}

tasks.test {
    //useJUnitPlatform()
    jvmArgs("--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.util.zip=ALL-UNNAMED")
}
