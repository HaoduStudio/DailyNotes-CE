pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven {
            url = uri("https://mirrors.tuna.tsinghua.edu.cn/maven/")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        gradlePluginPortal()
        maven {
            url = uri("https://mirrors.tuna.tsinghua.edu.cn/maven/")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "DailyNotes-CE"
include(":app")
