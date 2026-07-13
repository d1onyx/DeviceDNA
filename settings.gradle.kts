pluginManagement {
    repositories {
        maven {
            url = rootDir.resolve("gradle/vendor-m2").toURI()
            content { includeGroupByRegex("com\\.devstdvad.*") }
        }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = rootDir.resolve("gradle/vendor-m2").toURI()
            content { includeGroupByRegex("com\\.devstdvad.*") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "DeviceDNA"
include(":android")
include(":shared")
