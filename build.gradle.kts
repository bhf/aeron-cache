plugins{
    alias(libs.plugins.lombok)
    application
}

val appVersion = project.version.toString()
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
                dependsOn(":cache-http:http-clustertools:jibDockerBuild")
                dependsOn(":cache-near:http-server-near-javalin:jibDockerBuild")
                dependsOn(":cache-sse:sse-server-jooby:jibDockerBuild")
                dependsOn(":cache-ws:ws-server-javalin:jibDockerBuild")
                dependsOn(":cache-cluster:jibDockerBuild")
                dependsOn(":cache-monolith:jibDockerBuild")
            }
        }

        if (project.path.startsWith(":cache-integration:clustered:") ||
            project.path.startsWith(":cache-integration:ephemeral:")) {
            maxParallelForks = 1
            usesService(testLock)
        }
    }
}

tasks.register("bumpVersion") {
    description = "Bumps the version in gradle.properties. Use -Ptype=[major|minor|patch] and -Psnapshot=[true|false]"
    group = "versioning"
    doLast {
        val type = project.findProperty("type") as? String ?: "patch"
        val isSnapshot = (project.findProperty("snapshot") as? String)?.toBoolean() ?: true
        
        val propsFile = file("gradle.properties")
        val content = propsFile.readText()
        val versionRegex = Regex("version=([0-9]+)\\.([0-9]+)\\.([0-9]+)(.*)")
        val match = versionRegex.find(content) ?: throw GradleException("Could not find version matching X.Y.Z in gradle.properties")
        
        val (major, minor, patch, oldSuffix) = match.destructured
        
         // If dropping snapshot, we usually don't increment the number if we're just finalizing the current snapshot
        val finalizeOnly = !isSnapshot && oldSuffix.contains("-SNAPSHOT") && type == "patch" && project.findProperty("type") == null
        
        var newMajor = major.toInt()
        var newMinor = minor.toInt()
        var newPatch = patch.toInt()
        var newSuffix = if (isSnapshot) "-SNAPSHOT" else ""

        if (!finalizeOnly) {
            when (type.lowercase()) {
                "major" -> { newMajor++; newMinor = 0; newPatch = 0 }
                "minor" -> { newMinor++; newPatch = 0 }
                "patch" -> newPatch++
                else -> throw GradleException("Unknown release type: $type. Use major, minor, or patch.")
            }
        }
        
        val newVersion = "$newMajor.$newMinor.$newPatch$newSuffix"
        val newContent = content.replace("version=${match.groupValues[1]}.${match.groupValues[2]}.${match.groupValues[3]}$oldSuffix", "version=$newVersion")
        propsFile.writeText(newContent)
        
        println("Bumped version: ${match.groupValues[1]}.${match.groupValues[2]}.${match.groupValues[3]}$oldSuffix -> $newVersion")
    }
}

tasks.register("release") {
    description = "Bumps version, commits, pushes, and creates a formal GitHub release to trigger CI publishing"
    group = "versioning"
    dependsOn("bumpVersion")
    
    doLast {
        val propsFile = file("gradle.properties")
        val content = propsFile.readText()
        val versionRegex = Regex("version=(.*)")
        val match = versionRegex.find(content) ?: throw GradleException("Could not find version in gradle.properties")
        val newVersion = match.groupValues[1].trim()
        val tagName = "v$newVersion"
        
        println("==> Staging gradle.properties...")
        ProcessBuilder("git", "add", "gradle.properties").redirectErrorStream(true).start().waitFor()
        
        println("==> Committing version bump to $newVersion...")
        ProcessBuilder("git", "commit", "-m", "Release $tagName").redirectErrorStream(true).start().waitFor()
        
        println("==> Pushing commit to GitHub...")
        ProcessBuilder("git", "push", "origin", "HEAD").redirectErrorStream(true).start().waitFor()
        
        println("==> Creating formal GitHub Release $tagName...")
        val ghProcess = ProcessBuilder("gh", "release", "create", tagName, "--generate-notes", "--title", "Release $tagName")
            .redirectErrorStream(true)
            .start()
        val ghOutput = ghProcess.inputStream.bufferedReader().readText()
        ghProcess.waitFor()
        if (ghProcess.exitValue() != 0) {
            println(ghOutput)
            throw GradleException("Failed to create GitHub release")
        }
        
        println("==> Successfully released $tagName! The GitHub action should now publish the packages.")
    }
}
