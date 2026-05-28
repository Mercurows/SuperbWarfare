# SuperbWarfare 项目架构指南

## 概览

SuperbWarfare 是一个 Minecraft Forge 1.20.1 军事题材模组。Kotlin/Java 混合开发（Kotlin 为主），使用 GeckoLib 做动画（未来要删除）和 Simple Bedrock Model 渲染，Curios 做饰品栏位，Cloth Config 做配置界面，Rhino 做 JS 脚本引擎。

- **Mod ID**: `superbwarfare`
- **包名**: `com.atsuishio.superbwarfare`
- **构建**: ForgeGradle 6.x + Kotlin 2.0.0 + serialization 插件
- **映射**: Parchment `2023.08.13-1.20.1`
- **Java 17**
- **入口类**: `src/main/kotlin/.../Mod.kt` (line 42, `@Mod` 注解)

## 核心概念

### 属性修改链（PMC）

这是整个武器系统的核心。`GunData.get(prop)` 计算最终属性值时，按以下顺序叠加修改：
1. NBT 中 JSON 格式的属性覆写
2. GunItem 级别的修改（`PropertyModifier`）
3. 当前开火模式的修改
4. AmmoConsumer 的修改
5. 所有已装备 Perk 的修改
6. 内置上下限（`GunProp.modifyProperty`）

### NBT 持久化

枪械运行时状态（弹药、Perk、配件、开火模式、热量、换弹状态等）全部存在 ItemStack NBT 中，通过类型化包装类（`IntValue`, `DoubleValue`, `BooleanValue`, `StringEnumValue`）读写。

### 数据驱动设计

枪械和载具的模板数据（`DefaultGunData`, `VehicleData`）从 JSON 文件加载，通过 (`sbw-data`) 提供的 `CustomData` 全局注册表访问。枪械定义靠数据而非硬编码。

## 目录结构

### `src/main/kotlin/.../` (Kotlin，主要代码)

| 包 | 说明                                                                          |
|----|-----------------------------------------------------------------------------|
| `Mod.kt` | 模组入口，注册 Registry、生命周期事件、tick 队列                                             |
| `init/` | 20+ 注册表（ModItems, ModBlocks, ModEntities, ModPerks...）                      |
| `data/` | Kotlin 数据层（ContainerDataManager, WreckageLootData）                          |
| `item/` | Kotlin 物品类（弹药、护甲、容器、饰品、武器、弹射物、杂项）+ `item/gun/GunItem.kt`（枪械基类和 GeckoLib 枪械） |
| `entity/` | 实体系统（见下文实体层级）                                                               |
| `event/` | 10 个事件处理器（客户端的、枪械 tick、玩家事件、生物伤害、鼠标点击等）                                     |
| `client/` | 全部客户端代码（渲染、模型、GUI、粒子、overlay、shader）                                        |
| `network/` | 网络消息 + 注册（message/send/ 客户端→服务端，message/receive/ 服务端→客户端）                   |
| `resource/` | 资源加载（GunResource, VehicleResource）                                          |
| `perk/` | 枪械 Perk 系统（弹药 Perk、伤害 Perk、功能 Perk 三类）                                      |
| `capability/` | Forge Capability（PlayerVariable, 物品/实体能量存储）                                 |
| `config/` | 配置文件定义（ClientConfig, CommonConfig, ServerConfig）                            |
| `compat/` | 模组兼容层（JEI, Jade, KubeJS, Cloth Config, Cold Sweat 等）                        |
| `api/event/` | 自定义 Forge 事件（ShootEvent, PreKillEvent, ProjectileHitEvent 等）                |
| `script/` | Rhino JS 脚本引擎集成                                                             |
| `tools/` | 工具类（HitboxHelper, MathTool, OBB, CustomExplosion, VectorTool 等）             |
| `datagen/` | 15+ 数据生成器（物品模型、配方、战利品表、标签、进度）                                               |
| `block/` | 自定义方块及其方块实体                                                                 |
| `command/` | 服务端命令                                                                       |
| `recipe/` | 自定义配方类型                                                                     |
| `procedures/` | 纪念用彩蛋代码，无意义                                                                 |
| `world/` | 世界数据（TDMSavedData, phys/ 射线追踪扩展）                                            |
| `advancement/` | 自定义进度触发器                                                                    |

### `src/main/java/.../` (Java + 部分 Kotlin 混放)

| 包 | 说明                                                                                              |
|----|-------------------------------------------------------------------------------------------------|
| `data/` | 数据基类（DataMap, IDBasedData, 序列化适配器）                                                              |
| `data/gun/` | 枪械数据类型（GunData.kt 951行, DefaultGunData.kt, GunProp.kt, Ammo.java, ShootPos.kt, FireMode 等）      |
| `data/vehicle/` | VehicleData, VehiclePropertyModifier                                                            |
| `data/launchable/` | ShootData, LaunchableEntityTool                                                                 |
| `data/mob_guns/` | 生物用枪数据                                                                                          |
| `data/drone_attachment/` | 无人机挂载数据                                                                                         |
| `item/gun/` | 全部具体枪械物品类（约45把枪，按类型分目录）                                                                         |
| `item/gun/vehicle/` | VehicleGun.kt（载具武器的抽象基类）                                                                        |
| `resource/gun/` | GunResource, DefaultGunResource, GunAnimation                                                   |
| `entity/vehicle/utils/` | VehicleMiscUtils                                                                                |
| `client/` | Java 客户端代码（GunRendererBuilder, PoseTool, 30+ 枪械 ItemRenderer, 30+ 枪械 ItemModel, layer/vehicle/） |
| `mixins/` | Mixin（Player, Villager, Mob, KeyMapping, LightTexture 等）                                   |
| `compat/netmusic/` | NetMusic 兼容内部持有类（未完成）                                                                           |

## 实体层级

```
Entity (Minecraft)
  ├─ VehicleEntity (base/VehicleEntity.kt:132) — 载具基类
  │    ├─ GeoVehicleEntity (base/GeoVehicleEntity.kt) — GeckoLib 载具（未来要删除）
  │    │    └─ Lav150Entity, Lav25Entity, M1A2Entity, T90aEntity 等
  │    └─ AutoAimableEntity (base/AutoAimableEntity.kt:53) — 自动瞄准
  │         └─ ArtilleryEntity (base/ArtilleryEntity.kt) — 火炮
  │              └─ GeckoArtilleryEntity (base/GeckoArtilleryEntity.kt:10) — 临时桥接
  ├─ Projectile (Minecraft)
  │    └─ ProjectileEntity (projectile/ProjectileEntity.kt:79) — 弹射物基类
  │         └─ CannonShellEntity, MissileProjectile, MortarShellEntity 等 30+ 种
  └─ 生物 (entity/living/) — DPSGenerator, Senpai, Target
```

VehicleEntity 主要能力：能量系统、武器槽位、座位系统、库存、OBB 碰撞检测、残骸状态、引擎、容器。

AutoAimableEntity 额外能力：主人 UUID、目标 UUID、激活状态、自动瞄准+射击解算、激光/射线武器、敌方/弹射物过滤。

ProjectileEntity 特色：OBB 支持的自定义射线追踪、爆头/腿伤检测、穿甲机制、爆炸/火焰/击退、弹孔弹片、曳光 RGB。

## 枪械系统

### GunItem（枪械基类）
`item/gun/GunItem.kt:82` — 抽象类，实现 `PropertyModifier<GunData, DefaultGunData>`

- `reloadTimeBehaviors` / `boltTimeBehaviors` — 换弹/拉栓动画关键时刻回调
- `modifyProperty()` — 子类覆写以添加额外属性加成
- 能量 Capability 支持

### GunGeoItem（GeckoLib 枪械，未来要删除）
`item/gun/GunItem.kt:33` — 继承 GunItem + GeoItem
- 提供动画控制器：idle, edit, bolt, reload, melee, fire, run/sprint
- 依赖 GunResource 数据 + ClientEventHandler 状态驱动

### VehicleGun（载具武器）
`item/gun/vehicle/VehicleGun.kt:65` — 载具武器抽象基类

### 弹药系统
- `AmmoConsumer` — 定义弹药消耗方式（弹药类型/槽位/装填量）
- `AmmoBoxItem` — 弹药盒（按弹药种类分类）
- `CreativeAmmoBoxItem` — 创造模式弹药盒

## 数据流转

```
JSON 数据文件 → CustomData.load() 加载
  ├─ CustomData.GUN_DATA → DefaultGunData (模板)
  ├─ CustomData.GUN_RESOURCE → DefaultGunResource (模型/动画)
  └─ CustomData.VEHICLE_DATA → VehicleData (模板)

ItemStack NBT ↔ GunData.from(stack) 获取运行时实例
  ├─ get(prop) → 经 PMC 链计算最终值
  └─ save() → 回写 NBT
```

## 网络通信

在 `network/NetworkRegistry.kt` 注册（`FMLCommonSetupEvent`）。

- **客户端→服务端** (`message/send/`): FireKey, Reload, SwitchWeapon, Zoom, AdjustMortar, 蓝图研究等
- **服务端→客户端** (`message/receive/`): Shoot 效果, 屏幕震动, 击杀指示器, 运动同步, 实体同步, TDM 同步等
- **DataSlot** (`dataslot/`): 容器能量同步

## 关键技术栈

| 技术 | 用途 |
|------|------|
| GeckoLib 4.4.6 | 动画控制器 + Bedrock 模型渲染 |
| Simple Bedrock Model 2.3.3 | Bedrock 模型加载器 (jar-in-jar) |
| Rhino 1.8.1-SNAPSHOT | JavaScript 脚本引擎 (jar-in-jar) |
| Kotlin for Forge 4.11.0 | Kotlin 语言支持 |
| Curios 5.14.1 | 饰品栏位 |
| Cloth Config | 配置 GUI |
| JEI + Jade + Patchouli | 配方查看 / HUD / 指南书 |
| KubeJS | JS 自定义内容 |

## 编码约定

- 大部分业务逻辑用 Kotlin（~70%），少部分遗留 Java（~30%）仍在 `src/main/java`
- 数据类在 `data/gun/` 混放 Kotlin/Java
- 渲染基类用 Kotlin，每个具体枪/载具的 Renderer 和 Model 各一个文件
- 使用 Kotlin `object` 做注册表持有者（如 `ModItems`, `ModEntities`）
- NBT 属性通过 `IntValue/BooleanValue/DoubleValue/StringEnumValue` 包装
- Perk 通过 `modifyProperty(PMC)` 修改属性
- `@OnlyIn(Dist.CLIENT)` 用于客户端代码
- 自定义事件存 `api/event/`，Forge 原生事件处理器存 `event/`

## 常见任务指引

- **添加新枪械**: 创建 JSON 数据文件 + 具体 GunItem 类（继承 GunGeoItem）+ ItemRenderer + ItemModel
- **添加新载具**: 创建 VehicleResource JSON + VehicleEntity 子类 + Renderer + Model
- **添加新弹药类型**: 创建 AmmoBoxItem + 添加 AmmoConsumer 配置
- **添加新 Perk**: 继承 Perk 基类，注册到 ModPerks，实现 modifyProperty
- **添加网络包**: 创建 Message 类 + 在 NetworkRegistry 注册
- **配置项**: ClientConfig / CommonConfig / ServerConfig 中定义，Cloth Config 生成 GUI

## TODO 数量

项目当前约 47 个 TODO（分布于数据层、渲染层、实体层和事件处理器），代表已知缺漏。
