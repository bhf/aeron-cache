plugins {
    id("java")
    alias(libs.plugins.sbegenerator)
}

val schema = "schema.xml"

sbeGenerator {
    src {
        dir = "src/main/resources/sbe"
        includes = listOf(schema)
    }

    javaCodecsDir = "src/main/java"
}


group = "com.bhf"
version = "1.0-SNAPSHOT"


dependencies {
    implementation(libs.sbetool)
    implementation(project(":cache-spi"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit)
    testImplementation(libs.junit.params)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockito)
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.util.zip=ALL-UNNAMED")
}
