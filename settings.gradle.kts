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
    }

    versionCatalogs {
        create("pluginLibs") {
            from(files("gradle/catalogs/plugin.versions.toml"))
        }
        create("testLibs") {
            from(files("gradle/catalogs/test.versions.toml"))
        }
        create("libs") {
            from(files("gradle/catalogs/libs.versions.toml"))
        }
    }
}

rootProject.name = "MyClient"
include(":app")
