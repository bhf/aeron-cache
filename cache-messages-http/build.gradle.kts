plugins {
    id("java")
    alias(libs.plugins.sbegenerator)
}


dependencies {
    implementation(project(":cache-messages-sbe"))
}

tasks.test {
    useJUnitPlatform()
}
