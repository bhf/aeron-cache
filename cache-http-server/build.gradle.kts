plugins {
    application
    alias(libs.plugins.shadow)
}

project.setProperty("mainClassName", "com.bhf.aeroncache.application.HttpApplication")

dependencies {
    implementation(project(":cache-client"))
    implementation(libs.aeron)
}

tasks.test {
    useJUnitPlatform()
}
