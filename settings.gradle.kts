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
        google()
        maven(url = "https://maven.aliyun.com/repository/central") {
            content {
                includeGroupByRegex("com\\.tom-roush.*")
                includeGroupByRegex("org\\.apache\\.pdfbox.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "Nocturne"
include(":app")
