pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "MinecraftForge"
            url = uri("https://maven.minecraftforge.net/")
        }
        maven {
            url = uri("https://maven.parchmentmc.org")
        }
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.0.0"
    }
}

include(":ksp")
