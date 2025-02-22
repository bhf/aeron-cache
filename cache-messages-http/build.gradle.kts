plugins {
    id("java")
    alias(libs.plugins.sbegenerator)
}


dependencies {

}

tasks.test {
    useJUnitPlatform()
}
