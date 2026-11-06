pluginManagement {
    repositories {
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
        google()
        mavenCentral()
        // Pleos Connect SDK 저장소. ai.pleos.playground 그룹만 여기서 받는다.
        maven {
            url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/")
            content {
                includeGroup("ai.pleos.playground")
            }
        }
    }
}

rootProject.name = "safe_kids"
include(":app")
 