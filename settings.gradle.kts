pluginManagement {
    repositories {
        gradlePluginPortal() // If you need Gradle plugins
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google() // Put Google repo first here
        mavenCentral()
    }
}

rootProject.name = "HealthLinker"
include(":app")