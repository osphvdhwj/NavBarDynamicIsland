pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // For Xposed API dependencies
        maven { url = uri("https://api.lsposed.org/maven") }
    }
}

rootProject.name = "NavBarDynamicIsland"
include(":app")
