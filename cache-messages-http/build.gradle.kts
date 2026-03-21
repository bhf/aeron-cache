plugins {
    id("java")
    alias(libs.plugins.sbegenerator)
}


dependencies {
    implementation(project(":cache-messages-sbe"))
    implementation(project(":cache-spi"))
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.util.zip=ALL-UNNAMED")
}
