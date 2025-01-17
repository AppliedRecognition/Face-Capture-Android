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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        maven("https://s01.oss.sonatype.org/content/groups/staging/")
        google()
        mavenCentral()
    }
}

rootProject.name = "FaceCapture"
include(":app")
include(":face-capture")
include(":ver-id")
