plugins {
    application
    alias(libs.plugins.shadow)
}

group = "com.bhf.aeroncache"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.bhf.aeroncache.monolith.application.Main")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":cache-cluster"))
    implementation(project(":cache-http:http-server-javalin"))
    implementation(project(":cache-http:http-clustertools"))
    implementation(project(":cache-ws:ws-server-javalin"))
    implementation(project(":cache-sse:sse-server-jooby"))
    runtimeOnly(project(":cache-spi-impl:map-cache:map-cache-core"))

    implementation(libs.jooby.kotlin)
    implementation(libs.jooby.netty)
    implementation(libs.jooby.jackson)

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}