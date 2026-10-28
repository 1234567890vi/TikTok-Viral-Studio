pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven("https://chaquo.com/maven")
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven("https://chaquo.com/maven")
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "TikTokStudio"

include(":app")
