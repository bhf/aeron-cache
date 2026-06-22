plugins {
    java
}

val sbeToolConfig by configurations.creating

dependencies {
    sbeToolConfig(libs.sbetool)
    implementation(libs.sbetool)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit)
    testImplementation(libs.junit.params)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockito)
}

val generatedSbeSourceDir = layout.buildDirectory.dir("generated/sources/sbe/main/java")

sourceSets {
    main {
        java {
            srcDir(generatedSbeSourceDir)
        }
    }
}

tasks.register<JavaExec>("generateSbeCodecs") {
    mainClass.set("uk.co.real_logic.sbe.SbeTool")
    classpath = sbeToolConfig
    val outputDir = generatedSbeSourceDir
    systemProperty("sbe.output.dir", outputDir.get().asFile.absolutePath)
    args("src/main/resources/sbe/schema.xml", "src/main/resources/sbe/counters-schema.xml")
}

tasks.compileJava {
    dependsOn("generateSbeCodecs")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.util.zip=ALL-UNNAMED")
}
