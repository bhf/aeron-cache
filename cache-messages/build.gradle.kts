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
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
