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
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit)
    testImplementation(libs.junit.params)
    testImplementation(libs.mockito)
}

tasks.test {
    useJUnitPlatform()
}
