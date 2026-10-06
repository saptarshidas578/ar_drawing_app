// settings.gradle.kts — tells Gradle where to find plugins and libraries
pluginManagement {
    repositories {
        google()          // Android Gradle Plugin, Kotlin plugin
        mavenCentral()    // Most Kotlin/Java libraries
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Force all modules to use these repos (no per-module repo blocks)
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TraceAR"
include(":app")
