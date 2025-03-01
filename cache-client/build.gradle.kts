plugins {
    application
    alias(libs.plugins.shadow)
}

project.setProperty("mainClassName", "com.bhf.aeroncache.application.SampleClientUsage")

dependencies {
    implementation(project(":cache-messages-sbe"))
    implementation(project(":cache-cluster"))
    implementation(project(":cache-common"))
    implementation(libs.aeron)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
}

tasks.test {
    useJUnitPlatform()
}
