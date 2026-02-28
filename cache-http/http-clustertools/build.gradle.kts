plugins {
    application
    alias(libs.plugins.shadow)
}

project.setProperty("mainClassName", "com.bhf.aeroncache.clustertools.application.Main")

dependencies {
    implementation(libs.javalin)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(libs.slf4j)
    implementation(libs.jackson.core)
    implementation(libs.micrometer.prometheus)
    implementation(libs.micrometer.javalin)
    implementation(libs.aeron)
    implementation(project(":cache-common"))
}

tasks.test {
    useJUnitPlatform()
}