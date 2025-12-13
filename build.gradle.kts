import java.io.ByteArrayOutputStream
import java.time.Instant

plugins {
    eclipse
    idea
    id("net.minecraftforge.gradle") version "[6.0.16,6.2)"
    id("org.spongepowered.mixin") version "0.7.+"
    id("org.parchmentmc.librarian.forgegradle") version "1.+"
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
}

fun getGitCommitHash(): String {
    return runCatching {
        val stdout = ByteArrayOutputStream()
        project.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            standardOutput = stdout
        }
        stdout.toString().trim()
    }.getOrElse { "unknown" }
}

version = "${project.property("minecraft_version")}-${project.property("mod_version")}-${getGitCommitHash()}"
group = "com.atsushio.superbwarfare"

base {
    archivesName.set(project.property("mod_id").toString())
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

minecraft {
    mappings("parchment", "2023.08.13-1.20.1") // 直接使用括号和逗号
    accessTransformer(file("src/main/resources/META-INF/accesstransformer.cfg"))
    copyIdeResources.set(true)

    runs {
        all {
            // 需要使用 JetBrains 的 JBR 作为运行时才能发挥作用
            jvmArgs(
                "-XX:+IgnoreUnrecognizedVMOptions",
                "-XX:+AllowEnhancedClassRedefinition"
            )
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "info")
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
            property("geckolib.disable_examples", "true")
            mods {
                create(project.property("mod_id").toString()) { // 创建 mod 配置
                    source(sourceSets.main.get())
                }
            }
        }

        create("client") {
            property("forge.enabledGameTestNamespaces", project.property("mod_id").toString())
            property("geckolib.disable_examples", "true")
        }

        create("server") {
            property("forge.enabledGameTestNamespaces", project.property("mod_id").toString())
        }

        create("data") {
            args(
                "--mod",
                "superbwarfare",
                "--all",
                "--output",
                file("src/generated/resources/"),
                "--existing",
                file("src/main/resources/")
            )
        }
    }
}

sourceSets.main.get().resources {
    srcDir("src/generated/resources")
    exclude(".cache/**")
}

repositories {
    mavenLocal()
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
        name = "Kotlin for Forge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
    }
    mavenCentral()
}

//jarJar.enable()

dependencies {
//    implementation("org.mozilla:rhino:1.8.0")
//    minecraftLibrary("org.mozilla:rhino:1.8.0")
//    jarJar(group = "org.mozilla", name = "rhino", version = "[1.8.0,2.0.0)")

    implementation("thedarkcolour:kotlinforforge:4.11.0")

    minecraft("net.minecraftforge:forge:1.20.1-47.2.0")
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")

    runtimeOnly(fg.deobf("top.theillusivec4.curios:curios-forge:5.4.2+1.20.1"))
    compileOnly(fg.deobf("top.theillusivec4.curios:curios-forge:5.4.2+1.20.1:api"))

    implementation(fg.deobf("software.bernie.geckolib:geckolib-forge-1.20.1:4.4.6"))
    implementation(fg.deobf("com.eliotlash.mclib:mclib:20"))

    // 可选 mod 依赖

    // JEI相关
    // compile against the JEI API but do not include it at runtime
    compileOnly(fg.deobf("mezz.jei:jei-${project.property("minecraft_version")}-common-api:${project.property("jei_version")}"))
    compileOnly(fg.deobf("mezz.jei:jei-${project.property("minecraft_version")}-forge-api:${project.property("jei_version")}"))
    // at runtime, use the full JEI jar for Forge
    runtimeOnly(fg.deobf("mezz.jei:jei-${project.property("minecraft_version")}-forge:${project.property("jei_version")}"))

    // 帕秋莉手册
    compileOnly(fg.deobf("vazkii.patchouli:Patchouli:1.20.1-84-FORGE:api"))
    runtimeOnly(fg.deobf("vazkii.patchouli:Patchouli:1.20.1-84-FORGE"))

    // Cloth Config相关
    implementation(fg.deobf("me.shedaniel.cloth:cloth-config-forge:${project.property("cloth_config_version")}"))

    // Jade相关
    implementation(fg.deobf("curse.maven:jade-324717:${project.property("jade_version")}"))

    // 冷汗
    implementation(fg.deobf("curse.maven:cold-sweat-506194:6503192"))

    // 真实相机
    compileOnly(fg.deobf("curse.maven:real-camera-851574:${project.property("real_camera_id")}"))

    // 网络音乐机
    implementation(fg.deobf("curse.maven:net-music-978569:6838602"))

    // 测试用mod
    // 这俩是仅客户端mod
    // implementation fg.deobf("curse.maven:oculus-581495:6020952")
    // implementation fg.deobf("curse.maven:embeddium-908741:5681725")
    implementation(fg.deobf("curse.maven:timeless-and-classics-zero-1028108:6518539"))
    implementation(fg.deobf("curse.maven:create-328085:6255513"))
    implementation(fg.deobf("curse.maven:mmmmmmmmmmmm-225738:6237015"))
    implementation(fg.deobf("curse.maven:selene-499980:6249659"))
    // better combat相关
    implementation(fg.deobf("curse.maven:better-combat-by-daedelus-639842:5625757"))
    implementation(fg.deobf("curse.maven:playeranimator-658587:4587214"))

//    implementation("curse.maven:spark-361579:4587309")
}

mixin {
    add(sourceSets.main.get(), "mixins.superbwarfare.refmap.json")

    config("mixins.superbwarfare.json")

//    debug {
//        verbose = true
//        export = true
//    }
    dumpTargetOnFailure = true

    isQuiet = true
}

tasks.named<ProcessResources>("processResources") {
    val replaceProperties = mapOf(
        "minecraft_version" to project.property("minecraft_version"),
        "minecraft_version_range" to project.property("minecraft_version_range"),
        "forge_version" to project.property("forge_version"),
        "forge_version_range" to project.property("forge_version_range"),
        "loader_version_range" to project.property("loader_version_range"),
        "mod_id" to project.property("mod_id"),
        "mod_name" to project.property("mod_name"),
        "mod_license" to project.property("mod_license"),
        "mod_version" to project.property("mod_version"),
        "mod_authors" to project.property("mod_authors"),
        "mod_description" to project.property("mod_description")
    )
    inputs.properties(replaceProperties)
    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) {
        expand(replaceProperties + mapOf("project" to project))
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Specification-Title" to project.property("mod_id"),
            "Specification-Vendor" to project.property("mod_authors"),
            "Specification-Version" to "1",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to project.property("mod_authors"),
            "Implementation-Timestamp" to Instant.now().toString()
        )
    }
    finalizedBy("reobfJar")
}

java {
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// 让 idea 主动下载前置库的源码和 Javadoc
// 新版本 idea 默认不会下载这两个，这虽然加快了构建速度，但是不方便调试
idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

kotlin {
    jvmToolchain(17)
}