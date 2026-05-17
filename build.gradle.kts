plugins {
    idea
    id("java-library")
    id("maven-publish")
    id("net.neoforged.moddev") version "2.0.80"
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20"

    id("com.google.devtools.ksp") version "2.1.20-2.0.1"
}

tasks.named<Wrapper>("wrapper") {
    // Define wrapper values here so as to not have to always do so when updating gradlew.properties.
    // Switching this to Wrapper.DistributionType.ALL will download the full gradle sources that comes with
    // documentation attached on cursor hover of gradle classes and methods. However, this comes with increased
    // file size for Gradle. If you do switch this to ALL, run the Gradle wrapper task twice afterwards.
    // (Verify by checking gradle/wrapper/gradle-wrapper.properties to see if distributionUrl now points to `-all`)
    distributionType = Wrapper.DistributionType.BIN
}

version = "${project.property("mod_version")}-mc${project.property("minecraft_version")}"
group = "com.atsuishio.superbwarfare"

repositories {
    mavenLocal()
    mavenCentral()
    flatDir {
        dir("libs")
    }
    maven {
        url = uri("https://maven.theillusivec4.top/")
        content {
            includeGroup("top.theillusivec4.curios")
        }
    }
    maven {
        name = "GeckoLib"
        url = uri("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
        content {
            includeGroupByRegex("software\\.bernie.*")
            includeGroup("com.eliotlash.mclib")
        }
    }
    maven {
        name = "Jared's maven"
        url = uri("https://maven.blamejared.com/")
        content {
            includeGroup("mezz.jei")
            includeGroup("vazkii.patchouli")
        }
    }
    maven {
        url = uri("https://maven.shedaniel.me/")
        content {
            includeGroup("me.shedaniel.cloth")
        }
    }
    maven {
        url = uri("https://cursemaven.com")
        content {
            includeGroup("curse.maven")
        }
    }
    maven {
        url = uri("https://api.modrinth.com/maven")
    }
    maven {
        name = "Kotlin for Forge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
    }
    maven {
        url = uri("https://jitpack.io")
        content {
            includeGroup("com.github.MCModderAnchor")
        }
    }
}

base {
    archivesName.set(project.property("mod_id") as String)
}

// Mojang ships Java 21 to end users starting in 1.20.5, so mods should target Java 21.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

neoForge {
    // Specify the version of NeoForge to use.
    version = project.property("neo_version") as String

    parchment {
        mappingsVersion = project.property("parchment_mappings_version") as String
        minecraftVersion = project.property("parchment_minecraft_version") as String
    }

    // This line is optional. Access Transformers are automatically detected
//    accessTransformers = files("src/main/resources/META-INF/accesstransformer.cfg")

    // Default run configurations.
    // These can be tweaked, removed, or duplicated as needed.
    runs {
        create("client") {
            client()

            // Comma-separated list of namespaces to load gametests from. Empty = all namespaces.
            systemProperty("neoforge.enabledGameTestNamespaces", project.property("mod_id") as String)
        }

        create("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", project.property("mod_id") as String)
        }

        // This run config launches GameTestServer and runs all registered gametests, then exits.
        // By default, the server will crash when no gametests are provided.
        // The gametest system is also enabled by default for other run configs under the /test command.
        create("gameTestServer") {
            type = "gameTestServer"
            systemProperty("neoforge.enabledGameTestNamespaces", project.property("mod_id") as String)
        }

        create("data") {
            data()

            // example of overriding the workingDirectory set in configureEach above, uncomment if you want to use it
            // gameDirectory = project.file('run-data')

            // Specify the modid for data generation, where to output the resulting resource, and where to look for existing resources.
            programArguments.addAll(
                "--mod",
                project.property("mod_id") as String,
                "--all",
                "--output",
                file("src/generated/resources/").absolutePath,
                "--existing",
                file("src/main/resources/").absolutePath
            )
        }

        // applies to all the run configs above
        configureEach {
            jvmArguments = listOf(
                "-XX:+IgnoreUnrecognizedVMOptions",
                "-XX:+AllowEnhancedClassRedefinition"
            )

            // Recommended logging data for a userdev environment
            // The markers can be added/remove as needed separated by commas.
            // "SCAN": For mods scan.
            // "REGISTRIES": For firing of registry events.
            // "REGISTRYDUMP": For getting the contents of all registries.
            systemProperty("forge.logging.markers", "REGISTRIES")

            // Recommended logging level for the console
            // You can set various levels here.
            // Please read: https://stackoverflow.com/questions/2031163/when-to-use-the-different-log-levels
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        // define mod <-> source bindings
        // these are used to tell the game which sources are for which mod
        // mostly optional in a single mod project
        // but multi mod projects should define one per mod
        create(project.property("mod_id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }
}

// Include resources generated by data generators.
sourceSets.main.get().resources {
    srcDir("src/generated/resources")
}

// Sets up a dependency configuration called 'localRuntime'.
// This configuration should be used instead of 'runtimeOnly' to declare
// a dependency that will be present for runtime testing but that is
// "optional", meaning it will not be pulled by dependents of this mod.
configurations {
    create("localRuntime")
    getByName("runtimeClasspath").extendsFrom(getByName("localRuntime"))
}

dependencies {
    ksp(project(":ksp"))
    implementation(project(":ksp"))

//    implementation("org.mozilla:rhino:1.8.0")
//    add("additionalRuntimeClasspath", "org.mozilla:rhino:1.8.0")
//    jarJar(group = "org.mozilla", name = "rhino", version = "[1.8.0,2.0.0)")
    implementation("thedarkcolour:kotlinforforge-neoforge:5.10.0")

    implementation("software.bernie.geckolib:geckolib-neoforge-1.21.1:4.7.5")

    runtimeOnly("top.theillusivec4.curios:curios-neoforge:9.2.0+1.21.1")
    compileOnly("top.theillusivec4.curios:curios-neoforge:9.2.0+1.21.1:api")

    // SBM
    val sbm = implementation(
        group = "com.github.MCModderAnchor",
        name = "SimpleBedrockModel",
        version = "2.3.3-neoforge-mc1.21.1",
    )
    jarJar(sbm) {
        version {
            strictly("[2.0,3.0)")
            prefer("2.3.3")
        }
    }
    compileOnly("com.maydaymemory:mae:1.1.2") {
        exclude("com.google.code.findbugs", "jsr305")
        exclude("it.unimi.dsi", "fastutil")
        exclude("org.joml", "joml")
    }

    // 可选mod依赖
    compileOnly("mezz.jei:jei-1.21.1-common-api:${project.property("jei_version")}")
    compileOnly("mezz.jei:jei-1.21.1-neoforge-api:${project.property("jei_version")}")
    runtimeOnly("mezz.jei:jei-${project.property("minecraft_version")}-neoforge:${project.property("jei_version")}")
    implementation("curse.maven:jade-324717:6291517")

    // 帕秋莉手册
    compileOnly("curse.maven:patchouli-306770:6164617")
    runtimeOnly("curse.maven:patchouli-306770:6164617")

    // Cloth Config相关
    implementation("me.shedaniel.cloth:cloth-config-neoforge:15.0.140")

    // Kubejs
    implementation("curse.maven:kubejs-238086:7278501")
    implementation("curse.maven:rhino-416294:7104526")

//    implementation("curse.maven:cupboard-326652:6078150")
//    implementation("curse.maven:connectivity-470193:6229173")

    // 冷汗
    implementation("curse.maven:cold-sweat-506194:6176789")

    // 真实相机
    compileOnly("curse.maven:real-camera-851574:${project.property("real_camera_id")}")


    // 网络音乐机
    implementation("curse.maven:net-music-978569:6838604")

    // 车万女仆
    implementation("curse.maven:touhou-little-maid-355044:7510722")

    // 测试用mod
    implementation("curse.maven:better-combat-by-daedelus-639842:6532547")
    implementation("curse.maven:playeranimator-658587:6024462")

    implementation("curse.maven:spark-361579:6225208")
//    implementation(fg.deobf("curse.maven:oculus-581495:6020952"))
//    implementation(fg.deobf("curse.maven:embeddium-908741:5681725"))
//    implementation(fg.deobf("curse.maven:timeless-and-classics-zero-1028108:6069384"))
//    implementation(fg.deobf("curse.maven:create-328085:6255513"))
//    implementation(fg.deobf("curse.maven:mmmmmmmmmmmm-225738:6237015"))
//    implementation(fg.deobf("curse.maven:selene-499980:6249659"))
}

// This block of code expands all declared replace properties in the specified resource targets.
// A missing property will result in an error. Properties are expanded using ${} Groovy notation.
val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to project.property("minecraft_version"),
        "minecraft_version_range" to project.property("minecraft_version_range"),
        "neo_version" to project.property("neo_version"),
        "neo_version_range" to project.property("neo_version_range"),
        "loader_version_range" to project.property("loader_version_range"),
        "mod_id" to project.property("mod_id"),
        "mod_name" to project.property("mod_name"),
        "mod_license" to project.property("mod_license"),
        "mod_version" to project.property("mod_version"),
        "mod_authors" to project.property("mod_authors"),
        "mod_description" to project.property("mod_description")
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
    into("build/generated/sources/modMetadata")
}
// Include the output of "generateModMetadata" as an input directory for the build
// this works with both building through Gradle and the IDE.
sourceSets.main.get().resources.srcDir(generateModMetadata)
// To avoid having to run "generateModMetadata" manually, make it run on every project reload
neoForge.ideSyncTask(generateModMetadata)

// Example configuration to allow publishing using the maven-publish plugin
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri("file://${project.projectDir}/repo")
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8" // Use the UTF-8 charset for Java compilation
}

tasks.named("createMinecraftArtifacts") {
    dependsOn(tasks.named("generateModMetadata"))
}

// IDEA no longer automatically downloads sources/javadoc jars for dependencies, so we need to explicitly enable the behavior.
idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

kotlin {
    jvmToolchain(21)
}