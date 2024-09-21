group = "com.bhf.aeroncache"
version = "1.0-SNAPSHOT"

plugins{
    alias(libs.plugins.lombok)
}

allprojects {
    apply { from("$rootDir/gradle/lombok.gradle") }

    repositories {
        mavenCentral()
    }
}