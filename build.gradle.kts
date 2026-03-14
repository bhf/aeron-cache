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

val skipIntegrationTests = project.hasProperty("skipIntegrationTests")

subprojects {
    tasks.withType<Test>().configureEach {
        if (project.path.startsWith(":cache-integration:")) {
            enabled = !skipIntegrationTests
            if (enabled) {
                dependsOn(":cache-http:http-server-javalin:jibDockerBuild")
                dependsOn(":cache-sse:sse-server-jooby:jibDockerBuild")
                dependsOn(":cache-ws:ws-server-javalin:jibDockerBuild")
                dependsOn(":cache-cluster:jibDockerBuild")
            }
        }
    }
}