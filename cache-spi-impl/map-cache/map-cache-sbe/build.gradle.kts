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

tasks.register<JavaExec>("generateSbeCodecs") {
    mainClass.set("uk.co.real_logic.sbe.SbeTool")
    classpath = sbeToolConfig
    systemProperty("sbe.output.dir", "src/main/java")
    args("src/main/resources/sbe/schema.xml")
}

tasks.compileJava {
    //dependsOn("generateSbeCodecs")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.util.zip=ALL-UNNAMED")
}
