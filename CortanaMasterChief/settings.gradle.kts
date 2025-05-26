pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // Repositorio Maven proporcionado en GitHub
        maven {
            url = uri("http://raw.github.com/saki4510t/libcommon/master/repository/")
            url = uri("https://jitpack.io")
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("http://raw.github.com/saki4510t/libcommon/master/repository/")
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "Cortana-MasterChief"
include(":app")
