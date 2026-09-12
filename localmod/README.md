# localmod/ —— 插件 mod 测试夹具

这里放「由**其他 mod 的 jar** 提供注册描述、由 Superb Warfare 代为注册」的测试夹具。
它同时是给第三方看的**协议参考实现**。

| 路径               | 说明                                                                                                                                         |
|------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| `sbwloadertest/` | 当前夹具（modId = `sbwloadertest`），声明注册 `sbwloadertest:test`，**没有 `@Mod`、没有 Forge 依赖**；带 Java class 与 Kotlin object 两个 `@TestLoaderTarget` 入口样例 |
| `sbwtest.jar`    | 历史遗留：早期 `lowcodefml` 方案的实验产物，已不参与加载（本地文件，已被 `.gitignore` 的 `*.jar` 忽略）                                                                     |

加载端实现在 `src/main/kotlin/com/atsuishio/superbwarfare/init/LoaderTest.kt`，
由 `Mod.kt` 的 `TestLoader.register(bus)` 接入。**这是试验实现**：跑通后再决定是否迁到独立的
`plugin` 包、以及是否对外发布 slim 的注解/API jar。

---

## 一、它是怎么被加载的

`sbwloadertest` **不是**打好的 jar，而是 `localmod/sbwloadertest/` 下的**展开目录**：

| 目录 | 作用 |
| --- | --- |
| `resources/` | 就是 jar 的根：`META-INF/`、`assets/`、`data/` |
| `java/` | 插件自己的 Java 类 |
| `kotlin/` | 插件自己的 Kotlin 类 |

接线在根目录 `build.gradle.kts`：

```kotlin
val loaderTest: SourceSet by sourceSets.creating {
    java.srcDir("localmod/sbwloadertest/java")
    kotlin.srcDir("localmod/sbwloadertest/kotlin")
    resources.srcDir("localmod/sbwloadertest/resources")
}

minecraft {
    runs {
        all {
            mods {
                create(project.property("mod_id").toString()) { source(sourceSets.main.get()) }
                create("sbwloadertest") { source(loaderTest) }
            }
        }
    }
}

dependencies {
    // 夹具要 import @TestLoaderTarget 注解类：只给编译期，运行期由本体提供
    add(loaderTest.compileOnlyConfigurationName, sourceSets.main.get().output)
}
```

（默认 `modLoader = "lowcodefml"`，所以只要这一行 `compileOnly` 就够 —— 夹具不 import
任何 Forge 类。改成 javafml 写 `@Mod` 时才需要再挂上 `minecraft` 配置，见第二节。）

ForgeGradle 6 的 `RunConfigGenerator#mapModClassesToGradle` 会把 `mods { }` 里每个条目
展开成 **`MOD_CLASSES` 环境变量**（不是 system property）里的 `<modId>%%<绝对路径>` 条目，
同一 modId 可以有多条、用 `File.pathSeparator`（Windows 上是 `;`）分隔；
FML 的 `CommonLaunchHandler#getModClasses()` 按 modId 分组，再由
`ExplodedDirectoryLocator` 当作**普通 mod 文件**加载。生成的值形如：

```text
MOD_CLASSES=sbwloadertest%%F:\...\build\resources\loaderTest;
            sbwloadertest%%F:\...\build\classes\java\loaderTest;
            sbwloadertest%%F:\...\build\classes\kotlin\loaderTest;
            sbwloadertest%%F:\...\build\generated\ksp\loaderTest\classes;
            superbwarfare%%F:\...\build\resources\main;
            superbwarfare%%F:\...\build\classes\java\main;
            superbwarfare%%F:\...\build\classes\kotlin\main;
            superbwarfare%%F:\...\build\generated\ksp\main\classes
```

（实际是一行、`;` 分隔，这里为可读性换行。条目顺序 = `mods { }` 容器按 modId 排序 + 每个
source set 的 resources / java / kotlin / ksp 输出目录。）

也就是说每个 source set 的 **resources 输出目录和 classes 输出目录是独立的条目**，
所以插件那一侧的 `classes` 和 `processResources` 都必须先跑完 —— 见
`build.gradle.kts` 末尾对 `runClient` / `runServer` / `runData` 的 `dependsOn`。
（`RunConfig#getAllSources()` 只会带上 `classesTaskName`，`processResources` 要自己接。）

> ⚠️ **`run/mods/` 只接受 `.jar`。** `ModsFolderLocator#scanCandidates()` 的筛选条件是
> `toLowerCase(文件名).endsWith(".jar")`，目录会被静默过滤掉。开发期用文件夹只有
> `MOD_CLASSES` 这一条途径，也就是上面的 `mods { }` 配置。
>
> 另：1.20.1 的 Forge **没有** `-Dfml.modFolders`（那是 MDG / 新版 FML 的写法），
> 别照着 1.21 分支的文档去 `build/moddev` 或者 VM args 里找它。

改完 JSON 直接 `gradlew runClient` 即可，**不需要打包**。启动后应有：

- Mods 列表里出现 `SBW Loader Test`
- 日志里出现这四行（运行期字符串统一英文，避免 GBK 控制台把中文转义成乱码）：
  - `[sbw-loader] loaded entrypoint com.sbwloadertest.LoaderTargetClass (mod=sbwloadertest)`
  - `[sbw-loader] loaded entrypoint com.sbwloadertest.LoaderTargetKClass (mod=sbwloadertest)`
  - `[sbw-loader] discovered plugin sbwloadertest: 1 item declaration(s), 2 entrypoint class(es)`
  - `[sbw-loader] registered sbwloadertest:test (declared by mod sbwloadertest)`
- 拿得到「测试物品」：配方书里用 `superbwarfare:beast` 合成，或 `/give @s sbwloadertest:test`

> 上面四行是在 `gradlew runServer`（1.20 / Forge 47.2.0 / dev userdev）里实测到的。
> 顺带一提，本分支的 `runServer` 目前会在这之后崩在
> `SuperStarShooterItem.<clinit>` 引用 `net.minecraft.client.model.HumanoidModel`
> （`RuntimeDistCleaner: invalid dist DEDICATED_SERVER`）+ Moonlight 的
> `Block to items map was null`，那是**本分支已有的问题**，与 loader 无关；
> 想完整跑一遍还是用 `runClient`。

---

## 二、目录契约（给第三方）

| 路径                           | 必需 | 说明                                                                                                         |
|------------------------------|----|------------------------------------------------------------------------------------------------------------|
| `META-INF/mods.toml`         | ✅  | 默认 `modLoader = "lowcodefml"`（**不需要 `@Mod`、不需要写一行 Java**）；要接 Forge 事件时才换 `javafml` + `@Mod("<modId>")`（见下） |
| `META-INF/sbw/registry.json` | ✅  | 注册描述，**只有 Superb Warfare 会读它**                                                                             |
| `assets/<modid>/…`           | 建议 | 模型/贴图/语言，按正常 mod 放，资源包机制自动合并                                                                               |
| `data/<modid>/…`             | 可选 | 配方/标签/战利品；**1.20.1 下配方目录是 `recipes/`**                                                                     |

### ⚠️ 选 `modLoader`：默认 lowcodefml，要事件总线才换 javafml

本体的 `@TestLoaderTarget` 扫描**和 `modLoader` 没有任何关系**：FML 的
`Scanner#scan()` 先无条件把所有 `.class` 的注解/类信息塞进 `ModFileScanData`，
之后才调用语言加载器的 `getFileVisitor()` —— 而后者只是**读**这份数据
（javafml 就是靠它填 `getTargets()`）。所以两种 loader 下
`IModFile#getScanResult()` 的内容完全一样，`Class.forName` + 实例化也照常：

```java
// Forge 1.20.1 —— IModFileScanData 的填充与 loader 无关
fileToScan.scanFile(p -> fileVisitor(p, result));   // ① 先扫所有 class（Scanner.java:32）
final List<IModLanguageProvider> loaders = fileToScan.getLoaders();
loaders.forEach(loader -> loader.getFileVisitor().accept(result));  // ② loader 只是读 result
```

|                           | `lowcodefml`（默认，推荐）                                                   | `javafml`                 |
|---------------------------|-----------------------------------------------------------------------|---------------------------|
| `@Mod` 入口类                | **不需要**                                                               | **必需**，1.20.1 上缺了直接 fatal |
| 编译期依赖                     | 只 `compileOnly` 本体的 output 就够（注解类）                                    | 还要看得见 Forge API（`@Mod`）   |
| mod 事件总线                  | 没有（`LowCodeModContainer#acceptEvent` 是空实现，也不注入 `@EventBusSubscriber`） | 有                         |
| 代码型入口 `@TestLoaderTarget` | ✅ 可用                                                                  | ✅ 可用                      |
| `registry.json` 代注册       | ✅ 可用                                                                  | ✅ 可用                      |

**为什么 javafml 在 1.20.1 上不能零代码**：`ModLoader#buildMods` 只遍历
`ModFileScanData#getTargets()`（由 javafml 从 `@Mod` 注解读出来）来建 mod 容器，
再拿它的数量跟 `mods.toml` 里的 modId 列表对账，对不上就 fatal：

```text
File <path> constructed 0 mods: [], but had 1 mods specified: [sbwloadertest]
The following classes are missing, but are reported in the mods.toml: [sbwloadertest]
net.minecraftforge.fml.ModLoadingException: The Mod File <path> has mods that were not found
```

而 `lowcodefml` 的 `LowCodeModLanguageProvider#getFileVisitor` 直接拿
`scanResult.getIModInfoData()` 里的 modId 建 `LowCodeModContainer`，根本不看 `@Mod`。
所以**默认用 lowcodefml**；哪天插件想订阅 Forge 事件（或想要自己的 mod 事件总线），
再自己改成：

```toml
modLoader = "javafml"       # + loaderVersion = "[47,)"
```

```java
@Mod("sbwloadertest")
@TestLoaderTarget           // 两个注解可以共存
public class LoaderTargetClass {
}
```

并给该 source set 补上 Forge 的编译期 API，例如
`configurations.named(loaderTest.compileOnlyConfigurationName) { extendsFrom(configurations["minecraft"]) }`。
（NeoForge 1.21 那边 `buildMods` 改成按 `mods.toml` 建容器、`@Mod` 只用来收集入口类，
javafml 也可以零代码 —— 这是两个分支之间唯一一处"协议级"差异。）

### 三条硬规则

1. **命名空间由加载器从 `IModFile` 推导，JSON 里不写命名空间。**
   `"Items": { "test": … }` 里的 key 就是物品路径 → 注册成 `sbwloadertest:test`。
   key 里出现 `:` 一律报错——这样 A mod 无法借这个通道往 B 的命名空间塞东西。
2. **注册数据放 `META-INF/`，不要放 `assets/`。** `assets/`/`data/` 是资源包与数据包的域，
   会被合并/覆盖；`META-INF/` 不会被游戏资源系统看见，也不可能被覆盖。
3. **不要用数据包分发注册信息。** 加载器只从 mod 文件里读（`IModFile#findResource`）。
   若允许数据包注入，服务端的注册表会与客户端不一致，直接崩。

一个 jar 只应声明一个 mod：声明多个时，加载器取第一个 modId 作为命名空间。

另外，`mods.toml` 里对本体的依赖**别写死成 `[0.8.10,)`**：Maven 版本序里
`0.8.10-snapshot < 0.8.10`，而本体的 `mod_version` 正是 `0.8.10-snapshot`，会被判为不满足并
直接崩在 pre-loading（`Mod sbwloadertest only supports superbwarfare 0.8.10 or above`）。
开发期用 `[0.8,)` 这类宽范围。

---

## 三、`registry.json` 字段

解析由 `LoaderTest.kt` 里的 `LoaderInfo` / `ItemRegisterInfo`（`@Serializable`，kotlinx.serialization）
负责，字段名就是 `@SerialName` 的值，**PascalCase**。`Items` 是**对象**而非数组：
**物品 id 就是 key**，唯一性由 JSON 对象语义天然保证，value 里不再重复写 id。

```json
{
  "FormatVersion": 1,
  "Items": {
    "test": { "Rarity": "common" }
  }
}
```

| 字段 | 必需 | 默认 | 说明 / 落点 |
| --- | --- | --- | --- |
| `FormatVersion` | ✅ | — | 协议版本，当前只支持 `1`，不匹配直接报错 |
| `Items` | ✅ | — | 物品声明对象：key = 物品路径，value = 声明（允许为空对象 `{}`） |
| `Items.<key>` | ✅ | — | 纯路径，**不允许 `:`**；须是合法资源路径（小写 a-z、0-9、`_`、`-`、`.`） |
| `Items.<key>.Rarity` | ✅ | — | `Item.Properties#rarity`，可选 `common`/`uncommon`/`rare`/`epic` |
| `Items.<key>.MaxStackSize` | ❌ | `64` | `Item.Properties#stacksTo`，取值 1..99 |
| `Items.<key>.FireResistant` | ❌ | `false` | `Item.Properties#fireResistant` |
| `Items.<key>.Durability` | ❌ | 无 | `Item.Properties#durability`，≥ 1 |

声明顺序 = 文件里的书写顺序（kotlinx 解码成 `LinkedHashMap`），注册顺序与之相同。
未识别的字段会被忽略（`Json { ignoreUnknownKeys = true }`），便于协议向前演进；
**缺必填字段**或类型不对会抛 `SerializationException`（含 `MissingFieldException`），
被包装成 `[sbw-loader] invalid <path> of mod <id>: cannot be decoded: …`。
字段集刻意取 **1.20.1 / 1.21.1 的交集**：同一份 JSON 在两个分支都能用，
只是加载器内部落到不同 API（1.20.1 没有 DataComponent，落点是 `Item.Properties` 的 builder 方法）。

### 失败时的行为（有意分成两档）

- **描述文件本身有问题**（`FormatVersion` 不支持、key 带命名空间或不是合法路径、`Rarity` 非法、
  JSON 语法错/缺字段）→ **构造期直接抛异常**。宁可启动即报错，也不要静默少注册几个物品，
  否则后面只会变成莫名其妙的 "Unknown item" 。
- **入口类加载失败**（类不存在、构造抛异常、缺依赖）→ 记 error 日志并跳过该入口。
  一个坏插件不应该炸掉整个启动。
- **物品 id 与所属 mod 自己注册的同名物品冲突** → 记 error 并**让给所属 mod**，不抢所有权。

---

## 四、代码型入口：`@TestLoaderTarget`

`LoaderTest.kt` 里的发现逻辑同时做两件事，其中注解扫描走的是 **FML 自己那套数据**：

```kotlin
// 1.21.1 (NeoForge) 有 getAnnotatedBy；1.20.1 的 ModFileScanData 只有 getAnnotations()，
// 所以要自己按 annotationType + targetType 过滤，clazz() 拿到的是 ASM 的 Type。
file.scanResult                                  // IModFile#getScanResult()
    .annotations
    .filter { it.annotationType() == TARGET_ANNOTATION && it.targetType() == ElementType.TYPE }
    .map { it.clazz().className }                // 只拿类名字符串
```

也就是说：**扫描阶段只看 class 文件里的注解，不加载类**（FML 找 `@Mod` 用的就是这个
`ModFileScanData`），筛出目标之后才 `Class.forName(name, true, <游戏类加载器>)`。
夹具里的样例：

```kotlin
package com.sbwloadertest

import com.atsuishio.superbwarfare.init.TestLoaderTarget

@TestLoaderTarget
object LoaderTargetKClass
```

加载器对实例化的约定：先找 Kotlin `object` 的 `INSTANCE` 静态字段，找不到再退回无参构造。
加载成功的实例会放进 `TestLoader.loadedEntrypoints`（按命名空间分组），
后续做「高级自定义功能」时可以直接取用。

> 注解目前放在 `com.atsuishio.superbwarfare.init`，第三方要 `compileOnly` 依赖本体的
> 输出才能用。正式对外时建议抽一个只含注解/接口、不依赖 Minecraft 的 slim API jar。

---

## 五、1.21 分支的差异（本仓库当前工作分支 = `1.20`：Forge 47.2.0 / MC 1.20.1）

| 项                 | 1.20.1 (Forge 47.2，**本分支**)                         | 1.21.1 (NeoForge 21.1)                |
|-------------------|-----------------------------------------------------|---------------------------------------|
| 构建插件              | ForgeGradle 6 (`net.minecraftforge.gradle`)         | ModDevGradle (`net.neoforged.moddev`) |
| dev 展开目录接线        | `MOD_CLASSES`（环境变量）                                 | `-Dfml.modFolders`（system property）   |
| 目录 mod 定位器        | `ExplodedDirectoryLocator`                          | `UserdevLocator`                      |
| 元数据文件             | `META-INF/mods.toml`                                | `META-INF/neoforge.mods.toml`         |
| 依赖字段              | `mandatory = true/false`                            | `type = "required"/"optional"`        |
| 依赖里的 loader       | `modId = "forge"`                                   | `modId = "neoforge"`                  |
| 可用的语言加载器          | javafml / lowcodefml / kotlinforforge               | javafml / lowcodefml                  |
| 零代码 mod（无 `@Mod`） | 必须用 `lowcodefml`                                    | `javafml` 也行                          |
| mod 容器怎么建         | **按 `@Mod` 类**（javafml）；lowcodefml 按 `mods.toml` 条目 | 按 `mods.toml` 条目（`@Mod` 可选）           |
| 配方目录              | `data/<ns>/recipes/`                                | `data/<ns>/recipe/`                   |
| 配方 result         | `{"item": "...", "count": n}`                       | `{"id": "...", "count": n}`           |
| 物品属性              | `Item.Properties`                                   | DataComponent                         |
| 入口注解扫描            | `ModFileScanData#getAnnotations()` 自己过滤             | `ModFileScanData#getAnnotatedBy(...)` |
| 映射                | 正式 SRG / 开发 Mojang，**有 reobf**                      | official（无 reobf）                     |

**结论**：`lowcodefml` 和 `javafml` 在两个分支都可用，所以插件元数据统一用
`modLoader = "lowcodefml"`（零代码）。`registry.json` 这类**纯 JSON 数据**在两个分支之间
完全可移植；`@TestLoaderTarget` 扫描也与 loader 无关（见第二节）。唯一的取舍是
**事件总线**：要接 Forge 事件就得换 `javafml` + `@Mod`，而 1.20.1 的 javafml 会因此
强制要求每个插件带一个 `@Mod` 入口类 —— 这也是"默认 lowcodefml"的原因。
另外带代码的插件在 1.20.1 上必须按环境分别构建
（正式 jar 经 ForgeGradle reobf 成 SRG，dev 环境是 Mojang 名，二者不能混用）。
读取侧的 API 形态一致：`IModFile#findResource` / `#getScanResult()` / `ModFileScanData`。

---

## 六、打包注意（第三方）

- 放 `src/main/resources/META-INF/sbw/registry.json` 即自动进 jar；若用 datagen 生成到
  `src/generated/resources`，记得把该目录加进 `sourceSets.main.resources.srcDir(…)`。
- 别让 `processResources` 的 `expand`/`filter` 通配扫到这些 JSON——`${…}` 会被替换，
  或缺属性直接构建失败。
- zip 路径**区分大小写**（`META-INF` ≠ `meta-inf`），Windows 上开发时容易漏。
- 别改 `MANIFEST.MF` 的 `TYPE` 属性：要带代码、要被扫描的插件保持默认（MOD），
  写成 `DATA_PACK`/`RESOURCEPACK` 可能不进 classpath。
- 可以被 JarJar 嵌套的场景请单独验证 `findResource` 是否仍能找到文件。
- 1.20.1 出正式包时，本体会被 `reobfJar` 重映射成 SRG；带代码的第三方插件不能直接复用
  dev 环境的编译产物，需要另行针对 SRG 构建（纯 JSON 插件不受影响）。
