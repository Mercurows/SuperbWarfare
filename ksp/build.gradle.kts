plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("com.google.devtools.ksp") version "2.0.0-1.0.24"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.0-1.0.24")
}
