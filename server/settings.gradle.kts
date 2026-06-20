pluginManagement {
    repositories {
        // 国内镜像优先，避免直连 maven 中央仓库超时/variant 解析失败
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        mavenCentral()
    }
}

plugins {
    // 自动解析 toolchain:首次构建时若本机无 JDK21,Gradle 会自动下载。
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "lightcare-server"
