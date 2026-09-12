# localmod/ —— 插件 mod 测试夹具

这里放「由**其他 mod 的 jar** 提供注册描述、由 Superb Warfare 代为注册」的测试夹具。
它同时是给第三方看的**协议参考实现**。

| 路径 | 说明 |
| --- | --- |
| `sbwloadertest/` | 当前夹具（modId = `sbwloadertest`），声明注册 `sbwloadertest:test`，并带一个代码型入口类的样例 |
| `sbwtest.jar` | 历史遗留：早期 `lowcodefml` 方案的实验产物，已不参与加载（本地文件，已被 `.gitignore` 的 `*.jar` 忽略） |

加载端实现在 `src/main/kotlin/com/atsuishio/superbwarfare/init/LoaderTest.kt`，
由 `Mod.kt` 的 `LoaderTestHarness.register(bus)` 接入。**这是试验实现**：跑通后再决定是否迁到独立的
`plugin` 包、以及是否对外发布 slim 的注解/API jar。

---

## 一、它是怎么被加载的

`sbwloadertest` **不是**打好的 jar，而是 `localmod/sbwloadertest/` 下的**展开目录**：

| 目录 | 作用 |
| --- | --- |
| `resources/` | 就是 jar 的根：`META-INF/`、`assets/`、`data/` |
| `java/` | 插件自己的类（目录名沿用 `java/`，里面是 Kotlin 源码） |

接线在根目录 `build.gradle.kts`：

```kotlin
val loaderTest: SourceSet by sourceSets.creating {
    java.srcDir("localmod/sbwloadertest/java")
    resources.srcDir("localmod/sbwloadertest/resources")
}

neoForge {
    mods {
        create(project.property("mod_id") as String) { sourceSet(sourceSets.main.get()) }
        create("sbwloadertest") { sourceSet(loaderTest) }
    }
}

dependencies {
    // 夹具要 import @LoaderTest 注解类：只给编译期，运行期由本体提供
    add(loaderTest.compileOnlyConfigurationName, sourceSets.main.get().output)
}
```

MDG 会把 `mods { }` 的每个条目转成 `-Dfml.modFolders=…`，NeoForge 侧由 `UserdevLocator`
当作**普通 mod 文件**加载。实测（MDG 2.0.80 + NeoForge 21.1.228）传进 JVM 的值形如：

```text
-Dfml.modFolders=superbwarfare%%F:\...\build\resources\main;sbwloadertest%%F:\...\build\classes\kotlin\loaderTest;sbwloadertest%%F:\...\build\resources\loaderTest
```

即 `<modId>%%<绝对路径>`，条目之间用 `;` 分隔；同一个 modId 可以有多条，会被合并成一个
mod 文件。它是通过 `CommandLineArgumentProvider` 挂到 run 任务上的，**不会**写进
`build/moddev/clientRunVmArgs.txt`，别去那里找。

> ⚠️ **`run/mods/` 只接受 `.jar`。** `ModsFolderLocator` 的筛选条件是
> `Files::isRegularFile` + `toLowerCase().endsWith(".jar")`，目录会被静默过滤掉。
> 开发期用文件夹只有 `fml.modFolders` 这一条途径，也就是上面的 `mods { }` 配置。

改完 JSON 直接 `gradlew runClient` 即可，**不需要打包**。启动后应有：

- Mods 列表里出现 `SBW Loader Test`
- 日志里出现这三行（运行期字符串统一英文，避免 GBK 控制台把中文转义成乱码）：
  - `[sbw-loader] loaded entrypoint com.sbwloadertest.addon.LoaderTargetClass (mod=sbwloadertest)`
  - `[sbw-loader] discovered plugin sbwloadertest: 1 item declaration(s), 1 entrypoint class(es)`
  - `[sbw-loader] registered sbwloadertest:test (declared by mod sbwloadertest)`
- 拿得到「测试物品」：配方书里用 `superbwarfare:beast` 合成，或 `/give @s sbwloadertest:test`

> 想不开客户端就验证？`gradlew runGameTestServer` **不行**：本仓库没有 gametest，
> GameTestServer 会在注册阶段之后、加载世界之前抛 `No test functions were given!`。
> 注册事件本身已经跑完，日志里能看到上面三行——但配方/数据包不会被校验。

---

## 二、目录契约（给第三方）

| 路径 | 必需 | 说明 |
| --- | --- | --- |
| `META-INF/neoforge.mods.toml` | ✅ | `modLoader = "javafml"`；**不写 `@Mod` 入口类也能加载** |
| `META-INF/sbw/registry.json` | ✅ | 注册描述，**只有 Superb Warfare 会读它** |
| `assets/<modid>/…` | 建议 | 模型/贴图/语言，按正常 mod 放，资源包机制自动合并 |
| `data/<modid>/…` | 可选 | 配方/标签/战利品；**1.20.1 下配方目录是 `recipes/`** |

### 三条硬规则

1. **命名空间由加载器从 `IModFile` 推导，JSON 里不写命名空间。**
   `"Items": { "test": … }` 里的 key 就是物品路径 → 注册成 `sbwloadertest:test`。
   key 里出现 `:` 一律报错——这样 A mod 无法借这个通道往 B 的命名空间塞东西。
2. **注册数据放 `META-INF/`，不要放 `assets/`。** `assets/`/`data/` 是资源包与数据包的域，
   会被合并/覆盖；`META-INF/` 不会被游戏资源系统看见，也不可能被覆盖。
3. **不要用数据包分发注册信息。** 加载器只从 mod 文件里读（`IModFile#findResource`）。
   若允许数据包注入，服务端的注册表会与客户端不一致，直接崩。

一个 jar 只应声明一个 mod：声明多个时，加载器取第一个 modId 作为命名空间。

另外，`neoforge.mods.toml` 里对本体的依赖**别写死成 `[0.8.10,)`**：Maven 版本序里
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
只是加载器内部落到不同 API（1.20.1 没有 DataComponent）。

### 失败时的行为（有意分成两档）

- **描述文件本身有问题**（`FormatVersion` 不支持、key 带命名空间或不是合法路径、`Rarity` 非法、
  JSON 语法错/缺字段）→ **构造期直接抛异常**。宁可启动即报错，也不要静默少注册几个物品，
  否则后面只会变成莫名其妙的 "Unknown item" 。
- **入口类加载失败**（类不存在、构造抛异常、缺依赖）→ **记 error 日志并跳过该入口**。
  一个坏插件不应该炸掉整个启动。
- **物品 id 与所属 mod 自己注册的同名物品冲突** → 记 error 并**让给所属 mod**，不抢所有权。

---

## 四、代码型入口：`@LoaderTest`

`LoaderTest.kt` 里的发现逻辑同时做两件事，其中注解扫描走的是 **FML 自己那套数据**：

```kotlin
file.scanResult                                  // IModFile#getScanResult()
    .getAnnotatedBy(LoaderTest::class.java, ElementType.TYPE)
    .map { it.clazz().className }                // 只拿类名字符串
```

也就是说：**扫描阶段只看 class 文件里的注解，不加载类**（FML 找 `@Mod` 用的就是这个
`ModFileScanData`），筛出目标之后才 `Class.forName(name, true, <游戏类加载器>)`。
夹具里的样例：

```kotlin
package com.sbwloadertest.addon

import com.atsuishio.superbwarfare.init.LoaderTest

@LoaderTest
object LoaderTargetClass
```

加载器对实例化的约定：先找 Kotlin `object` 的 `INSTANCE` 静态字段，找不到再退回无参构造。
加载成功的实例会放进 `LoaderTestHarness.loadedEntrypoints`（按命名空间分组），
后续做「高级自定义功能」时可以直接取用。

> 注解目前放在 `com.atsuishio.superbwarfare.init`，第三方要 `compileOnly` 依赖本体的
> 输出才能用。正式对外时建议抽一个只含注解/接口、不依赖 Minecraft 的 slim API jar。

---

## 五、1.20 分支的差异（`1.20` = Forge 47.2.0 / MC 1.20.1）

| 项 | 1.21.1 (NeoForge 21.1) | 1.20.1 (Forge 47.2) |
| --- | --- | --- |
| 元数据文件 | `META-INF/neoforge.mods.toml` | `META-INF/mods.toml` |
| 依赖里的 loader | `modId = "neoforge"` | `modId = "forge"` |
| 可用的语言加载器 | javafml + lowcodefml | **只有 javafml** |
| 配方目录 | `data/<ns>/recipe/` | `data/<ns>/recipes/` |
| 物品属性 | DataComponent | `Item.Properties` |
| 映射 | official（无 reobf） | 正式 SRG / 开发 Mojang，**有 reobf** |

**结论**：`javafml` 是唯一在两个分支都存在的 loader，所以协议统一用 javafml。
纯 JSON 插件在两个分支之间**完全可移植**；带代码的插件在 1.20.1 上必须按环境分别构建
（正式 jar 经 ForgeGradle reobf 成 SRG，dev 环境是 Mojang 名，二者不能混用）。
API 形态一致：`IModFile#findResource` / `#getScanResult()` / `ModFileScanData#getAnnotatedBy`。

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
