plugins {
    id("java")
}


dependencies {
    implementation(project(":cache-spi"))
    implementation(project(":cache-common"))
    implementation(libs.jackson.core)
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.util.zip=ALL-UNNAMED")
}
