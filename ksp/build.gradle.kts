plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("com.google.devtools.ksp") version "2.1.20-2.0.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.1.20-2.0.1")
}