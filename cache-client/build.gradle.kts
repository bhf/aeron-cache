plugins {
    application
    alias(libs.plugins.shadow)
}

project.setProperty("mainClassName", "com.bhf.aeroncache.application.SampleClientUsage")

dependencies {
    implementation(libs.aeron)
    implementation(project(":cache-messages"))
    implementation(project(":cache-cluster"))

}

tasks.test {
    useJUnitPlatform()
}
