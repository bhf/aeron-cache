plugins {
    id("java")
}

group = "com.bhf.aeroncache"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.aeron)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)

    implementation(project(":cache-spi"))
    implementation(project(":cache-common"))
    implementation(project(":cache-messages-sbe"))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.util.zip=ALL-UNNAMED")
}