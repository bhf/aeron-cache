plugins{
    id("pl.allegro.tech.build.axion-release") version "1.18.0"
    alias(libs.plugins.lombok)
    application
}

scmVersion {
    tag {
        prefix.set("v")
    }
}

val appVersion = scmVersion.version
val dockerRegistry = project.findProperty("dockerRegistry")?.toString() ?: ""

allprojects {
    group = "com.bhf.aeroncache"
    version = appVersion
    
    apply { from("$rootDir/gradle/lombok.gradle") }

    repositories {
        mavenCentral()
    }

    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(25))
            }
        }
    }
}

val skipIntegrationTests = project.findProperty("skipIntegrationTests")?.toString()?.replace(";", "")?.toBoolean() ?: false
val jibOnBuild = project.findProperty("jibDockerOnBuild")?.toString()?.replace(";", "")?.toBoolean() ?: false

abstract class TestLock : BuildService<BuildServiceParameters.None>

val testLock = gradle.sharedServices.registerIfAbsent("testLock", TestLock::class.java) {
    maxParallelUsages.set(1)
}

subprojects {
    tasks.withType<Test>().configureEach {
        systemProperty("aeroncache.image.tag", appVersion)
        systemProperty("aeroncache.image.registry", dockerRegistry)

        if (project.path.startsWith(":cache-integration:")) {
            enabled = !skipIntegrationTests
            if (!skipIntegrationTests && jibOnBuild) {
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