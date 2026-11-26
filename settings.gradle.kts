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
        // GeckoView يُنشر على مستودع موزيلا، لا على Maven Central
        maven { url = uri("https://maven.mozilla.org/maven2") }
    }
}
rootProject.name = "AI-Hub-GeckoView"
include(":app")
