group = "com.bhf.aeroncache"
version = "1.0-SNAPSHOT"

plugins{
    alias(libs.plugins.lombok)
    application
}

allprojects {
    apply { from("$rootDir/gradle/lombok.gradle") }

    repositories {
        mavenCentral()
    }
}

/*val skipIntegrationTests = project.hasProperty("skipIntegrationTests")

subprojects {
    if (path.startsWith(":cache-integration:")) {
        tasks.withType<Test>().configureEach {
            enabled = !skipIntegrationTests
        }
    }
}*/
