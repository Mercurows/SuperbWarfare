import java.time.Instant

plugins {
    eclipse
    idea
    id("net.neoforged.moddev.legacyforge") version "2.0.91"
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
    id("com.google.devtools.ksp") version "2.0.0-1.0.24"
}

fun getGitCommitHash(): String {
    return runCatching {
        providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText.get().trim()
    }.getOrElse { "unknown" }
}

version = "${project.property("mod_version")}-mc${project.property("minecraft_version")}-${getGitCommitHash()}"
group = "com.atsuishio.superbwarfare"

base {
    archivesName.set(project.property("mod_id").toString())
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

// 测试用插件 mod（loader test）。它作为"展开目录"参与 dev 加载：
// ModDevGradle 把 mods { } 的每个 source set 转成环境变量
// MOD_CLASSES 中的 <modid>%%<绝对路径> 条目，由 FML 的 ExplodedDirectoryLocator
// 当作普通 mod 文件加载 —— 改 localmod/ 下的 JSON 后无需打包 jar 即可生效。
// 注意：run/mods/ 只接受 .jar，目录形式的 mod 必须走这条路径。
val loaderTest: SourceSet by sourceSets.creating {
    java.srcDir("localmod/sbwloadertest/java")
    kotlin.srcDir("localmod/sbwloadertest/kotlin")
    resources.srcDir("localmod/sbwloadertest/resources")
}

legacyForge {
    version = "${project.property("minecraft_version")}-${project.property("forge_version")}"
    parchment {
        minecraftVersion = project.property("minecraft_version").toString()
        mappingsVersion = "2023.08.13"
    }

    mods {
        create(project.property("mod_id").toString()) {
            sourceSet(sourceSets.main.get())
        }
        // 名字需与 localmod/sbwloadertest/resources/META-INF/mods.toml 里的 modId 一致。
        create("sbwloadertest") {
            sourceSet(loaderTest)
        }
    }

    runs {
        configureEach {
            // 需要使用 JetBrains 的 JBR 作为运行时才能发挥作用
            jvmArguments.addAll(
                "-XX:+IgnoreUnrecognizedVMOptions",
                "-XX:+AllowEnhancedClassRedefinition"
            )
            gameDirectory = project.file("run")
            systemProperty("forge.logging.markers", "REGISTRIES")
            systemProperty("forge.logging.console.level", "info")
            systemProperty("geckolib.disable_examples", "true")
            systemProperty("mixin.dumpTargetOnFailure", "true")
        }

        create("client") {
            client()
            systemProperty("forge.enabledGameTestNamespaces", project.property("mod_id").toString())
        }

        create("server") {
            server()
            systemProperty("forge.enabledGameTestNamespaces", project.property("mod_id").toString())
        }

        create("data") {
            data()
            programArguments.addAll(
                "--mod",
                "superbwarfare",
                "--all",
                "--output",
                file("src/generated/resources/").absolutePath,
                "--existing",
                file("src/main/resources/").absolutePath
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
    mavenCentral()
    maven {
        url = uri("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
    }
    maven {
        url = uri("https://maven.createmod.net")
    }
    flatDir {
        dir("libs")
    }
    maven {
        url = uri("https://api.modrinth.com/maven")
        content {
            includeGroup("maven.modrinth")
        }
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
        name = "Kotlin for Forge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
    }
    maven {
        name = "Valkyrien Skies"
        url = uri("https://maven.valkyrienskies.org")
        content {
            includeGroup("org.valkyrienskies.core")
        }
    }
    maven {
        url = uri("https://jitpack.io")
        content {
            includeGroup("com.github.mcmodderanchor")
        }
    }
    maven {
        url = uri("https://maven.sighs.cc/repository/maven-public/")
    }
}

fun DependencyHandler.jijImplement(dependency: String, maxVersion: String? = null) {
    val (_, _, version) = dependency.split(":")

    val firstVersion = version.split(".")
        .takeWhile { Regex("""^\d+$""").matches(it) }
        .joinToString(".")

    val maximumVersion = maxVersion ?: run {
        val versions = firstVersion.split(".")
        val firstVersionNumber = versions[0].toIntOrNull()
        val firstVersionString = if (firstVersionNumber != null) {
            (firstVersionNumber + 1).toString()
        } else {
            versions[0]
        }

        return@run "$firstVersionString." + versions.drop(1).joinToString(".") { "0" }
    }

    // ModDevGradle 的 mod 配置不传递依赖；所需前置库在 dependencies 中显式声明。
    modImplementation(dependency)
    // 构建使用原来的固定版本，JarJar 元数据仍保留兼容范围，避免扫描范围内的新版。
    configurations.named("jarJar") {
        resolutionStrategy.force(dependency)
    }
    jarJar(dependency) {
        isTransitive = false
        version {
            strictly("[$firstVersion,$maximumVersion)")
            prefer(version)
        }
    }
}

dependencies {
    ksp(project(":ksp"))
    compileOnly(project(":ksp"))

    implementation("thedarkcolour:kotlinforforge:4.11.0")

    // loaderTest 夹具要 import @TestLoaderTarget 注解类：只给编译期可见，
    // 运行期由 superbwarfare 本体提供（它在游戏的 module path 上）。
    // 夹具默认用 lowcodefml，不碰 Forge API；要自己写 @Mod 的插件才需要额外
    // legacyForge.addModdingDependenciesTo(loaderTest)。
    add(loaderTest.compileOnlyConfigurationName, sourceSets.main.get().output)

    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")

    jijImplement("top.theillusivec4.curios:curios-forge:5.14.1+1.20.1")

    jijImplement("software.bernie.geckolib:geckolib-forge-1.20.1:4.4.6")
    // GeckoLib 已通过 JarJar 提供 mclib；独立运行时副本会造成同名 JPMS 模块冲突。
    compileOnly("com.eliotlash.mclib:mclib:20")

    // 从ywzj毛来的Rhino
    val rhino = "org.ywzj:rhino:1.8.1-SNAPSHOT"
    implementation(rhino)
    add("additionalRuntimeClasspath", rhino)
    configurations.named("jarJar") {
        resolutionStrategy.force(rhino)
    }
    jarJar(rhino) {
        version {
            strictly("[1.8.0,2.0.0)")
            prefer("1.8.1-SNAPSHOT")
        }
    }

    // SBM
    jijImplement("com.github.mcmodderanchor:simplebedrockmodel:2.6.0-forge-mc1.20.1")
    compileOnly("com.maydaymemory:mae:1.1.2") {
        exclude("com.google.code.findbugs", "jsr305")
        exclude("it.unimi.dsi", "fastutil")
        exclude("org.joml", "joml")
    }

    compileOnly("io.github.llamalad7:mixinextras-common:0.3.6")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.6")
    jijImplement("io.github.llamalad7:mixinextras-forge:0.3.6")

    // Ponder
    jijImplement("net.createmod.ponder:Ponder-Forge-${project.property("minecraft_version")}:${project.property("ponder_version")}")

    // 飞轮
    jijImplement("dev.engine-room.flywheel:flywheel-forge-${project.property("minecraft_version")}:${project.property("flywheel_version")}")

    // TODO 我需要掌中明月 我需要掌中明月 我需要掌中明月 我需要掌中明月 我需要掌中明月 但是这个JIJ写法不对
//    jijImplement("curse.maven:handheld-moon-1398036:7300858")

    // AUI
    jijImplement("com.sighs:ApricityUI-forge-1.20.1:1.2.7")

    // 可选 mod 依赖

    modCompileOnly("curse.maven:controlify-forgified-1377533:7719971")

    // 加速渲染
    modCompileOnly("curse.maven:accelerated-rendering-1314021:7342975")

    // JEI相关
    // compile against the JEI API but do not include it at runtime
    modCompileOnly("mezz.jei:jei-${project.property("minecraft_version")}-common-api:${project.property("jei_version")}")
    modCompileOnly("mezz.jei:jei-${project.property("minecraft_version")}-forge-api:${project.property("jei_version")}")
    // at runtime, use the full JEI jar for Forge
    modRuntimeOnly("mezz.jei:jei-${project.property("minecraft_version")}-forge:${project.property("jei_version")}")

    // 帕秋莉手册
    modCompileOnly("vazkii.patchouli:Patchouli:1.20.1-84-FORGE:api")
    modRuntimeOnly("vazkii.patchouli:Patchouli:1.20.1-84-FORGE")

    // Cloth Config相关
    jijImplement("me.shedaniel.cloth:cloth-config-forge:${project.property("cloth_config_version")}")

    // Jade相关
    modImplementation("curse.maven:jade-324717:${project.property("jade_version")}")

    // 冷汗
    modImplementation("curse.maven:cold-sweat-506194:6503192")

    // 真实相机
    modCompileOnly("curse.maven:real-camera-851574:${project.property("real_camera_id")}")

    // 网络音乐机
    modImplementation("curse.maven:net-music-978569:6838602")

    // 车万女仆
    modImplementation("curse.maven:touhou-little-maid-355044:7510714")

    // KubeJS
    modImplementation("curse.maven:kubejs-238086:5853326")
    modImplementation("curse.maven:architectury-api-419699:5137938")
    modImplementation("curse.maven:rhino-416294:6186971")

    // 测试用mod
    // 这俩是仅客户端mod
    if (!project.gradle.startParameter.taskNames.any { it.contains("runServer") }) {
        modRuntimeOnly("curse.maven:oculus-581495:6020952")
        modRuntimeOnly("curse.maven:embeddium-908741:5681725")
        modRuntimeOnly("curse.maven:controlify-forgified-1377533:7719971")
        modRuntimeOnly("maven.modrinth:yacl:3.6.6+1.20.1-forge")
    }

    modImplementation("curse.maven:timeless-and-classics-zero-1028108:6518539")
    modImplementation("curse.maven:create-328085:7178761")
    modImplementation("curse.maven:mmmmmmmmmmmm-225738:6237015")
    modImplementation("curse.maven:selene-499980:6249659")
//    modImplementation("curse.maven:limitless-vehicle-1446269:7675116")
    modImplementation("curse.maven:create-power-loader-936020:6549987")

    // vs
    compileOnly("org.valkyrienskies.core:api:1.1.0+") {
        exclude("org.joml", "joml")
    }
    modImplementation("curse.maven:valkyrien-skies-258371:7906689")
    modImplementation("curse.maven:eureka-ships-654384:7979379")

    // better combat相关
    modImplementation("curse.maven:better-combat-by-daedelus-639842:5625757")
    modImplementation("curse.maven:playeranimator-658587:4587214")
}

mixin {
    add(sourceSets.main.get(), "mixins.superbwarfare.refmap.json")
    config("mixins.superbwarfare.json")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
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
        expand(replaceProperties)
    }
}

tasks.named<Jar>("jar") {
    from("COPYING", "COPYING.LESSER")

    manifest {
        attributes(
            "Specification-Title" to project.property("mod_id"),
            "Specification-Vendor" to project.property("mod_authors"),
            "Specification-Version" to "1",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to project.property("mod_authors"),
            "Implementation-Timestamp" to Instant.now().toString(),
            "MixinConfigs" to "mixins.superbwarfare.json"
        )
    }
}

java {
    withSourcesJar()
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

// 保证测试插件 mod 的 classes / processResources 输出在启动前就位
// （ModDevGradle 会把 source set 的输出目录整体塞进 MOD_CLASSES）。
tasks.matching { it.name in listOf("runClient", "runServer", "runData") }
    .configureEach {
        dependsOn(loaderTest.classesTaskName, loaderTest.processResourcesTaskName)
    }
