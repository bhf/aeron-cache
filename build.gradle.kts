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
val jibOnBuild = project.hasProperty("jibDockerOnBuild")

abstract class TestLock : BuildService<BuildServiceParameters.None>

val testLock = gradle.sharedServices.registerIfAbsent("testLock", TestLock::class.java) {
    maxParallelUsages.set(1)
}

subprojects {
    tasks.withType<Test>().configureEach {
        if (project.path.startsWith(":cache-integration:")) {
            enabled = !skipIntegrationTests
            if (enabled || jibOnBuild) {
                dependsOn(":cache-http:http-server-javalin:jibDockerBuild")
                dependsOn(":cache-near:http-server-near-javalin:jibDockerBuild")
                dependsOn(":cache-sse:sse-server-jooby:jibDockerBuild")
                dependsOn(":cache-ws:ws-server-javalin:jibDockerBuild")
                dependsOn(":cache-cluster:jibDockerBuild")
            }
        }

        if (project.path.startsWith(":cache-integration:clustered:") ||
            project.path.startsWith(":cache-integration:singlenode:")) {
            maxParallelForks = 1
            usesService(testLock)
        }
    }
}