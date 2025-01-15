plugins {
    application
    alias(libs.plugins.shadow)
}

project.setProperty("mainClassName", "com.bhf.aeroncache.application.SampleClientUsage")

dependencies {
    implementation(project(":cache-messages"))
    implementation(project(":cache-cluster"))
    implementation(libs.aeron)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
}

tasks.test {
    useJUnitPlatform()
}
