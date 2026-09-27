# 枪械近战系统 v7 设计（MeleeActions + MeleeEffect + SubWeapon + G 键主/副武器切换）

> 状态：**一期（近战本体）、二期（配件体系 + 刺刀）与三期（`MeleeEffect` + `SubWeapon`）均已实现；
> 四期（副武器「主/副武器切换」机制）为本版新增的设计，尚未实现**。
> 本文既是设计稿也是落地记录：
> - **§11.1** 一期逐项核对表（完成项标注了真实文件路径）
> - **§11.2** 「正式实现与本文不一致的地方」+ 兼容性确认清单 + 遗留缺口（**实现时按代码为准**，本文相关段落已就地加注）
> - **§11.5** 二期落地记录；**§11.8** 三期落地记录；**§11.9** 三期后续（副武器开火表现）；**§11.10** 四期落地方案
> - **§12.2** / **§12.3** / **§12.4** / **§12.5** / **§12.6** 各期实现期间补充的决策记录
> - **§9** 描述的是**三期**的副武器机制（G = 触发一次射击）；**四期把它改成 G = 主/副武器切换**，
>   两者冲突时**以 §9.8 与 §11.10 为准**，§9 保留为历史记录（§9.4 / §9.6 / §11.8.1-⑩⑪⑭ / §11.9 的相应结论已被取代）。
>
> **v7 相对 v6 的变化（只有一处：副武器的操控方式）**：
> 1. **G 从「触发一次副武器射击」改成「在主武器与副武器之间切换」**——副武器本身就是一把拥有
>    `GunData` 的枪，所以切换之后**让当前操控的 gun 变成副武器**，开火/换弹/瞄准全部走原有链路，
>    代码里**不再有任何"这是副武器所以单独判一遍"的分支**（§9.8.1）；
> 2. **枪械近战恒用主武器**，副武器没有近战（按 G 切回主武器才挥刀；§9.8.2）；
> 3. **副武器的自动装填、`SubWeaponFireMessage`、服务端确认式开火音/开火动画全部删除**
>    （玩家自己按 R，与其他枪完全一致；§9.8.4）；
> 4. **动画与瞄准**：副武器**有自己的枪械资源与动画文件**（`sbw/guns/sub_weapon_gp_25.json`
>    里的 `Animation.Reload: animation.sub_weapon_gp_25.reload` → `animations/bedrock/attachment/sub_weapon_gp_25.animation.json`），
>    换弹时**它自己动**、宿主枪照常播 `idle`，两者互不干扰（**静默回退**：副武器没做这支 clip 时，
>    宿主枪播它自己的换弹动画，等价于三期的观感）。瞄准位形优先取**副武器模型自己的** `iron_view`，
>    没有则回退宿主枪的 `scope_view` / `iron_view`（§9.8.5 / §9.8.6 / §9.8.7）；
> 5. **副武器是独立的一把枪，因此动画也是它自己的**：`GunResource.compute(副武器合成栈)` 天然按
>    物品 id 解析出**副武器自己的资源**（`sbw/guns/<id>.json`），所以换弹动画直接写在它的
>    `Animation.Reload` 里即可。宿主枪在其间继续播 `idle`，两套骨骼**天然不冲突**——
>    **不需要姿态融合，也不需要只做左手**（§9.8.7）；
> 6. **1.20.1 Forge / 1.21.1 NeoForge 双版本兼容**：抽出 `GunStackStorage` 单一适配点，
>    「物品 NBT」与「物品 DataComponent」两个分支只在那一处分开，其余逻辑共用（§9.8.9）。
>
> **v6 相对 v5 的变化**（历史记录；第 3、4 条已被四期取代，见上）：
> 1. **配件物品接口化**：抽出 `AttachmentProvider` 接口，安装/工具提示/命令统一按接口判断；原 `AttachmentItem` 改名 **`BasicAttachmentItem`**（§8.3）；
> 2. **副武器物品本身就是一把枪**：`SubWeaponItem : GunItem, AttachmentProvider`——同一物品 id 同时拥有 `sbw/attachments/<id>.json`（配件定义）与 `sbw/guns/<id>.json`（枪数据），于是 `SubWeaponInfo.Data` 退化为**可选**（默认用物品自身 id）；
> 3. ~~**多个副武器**：不做优先级，**遍历一次逐个触发**（§9.4）~~ → 四期改成"切到枚举顺序里的第一个"（§9.8.2）；
> 4. ~~**副武器空仓时按 G = 尝试装填一次**（定稿，§9.6）~~ → 四期废除，玩家自己按 R（§9.8.4）。
> 设计取向：**不做反作弊复核、不做第三人称动作、不做竞技向精细判定**；HUD/改装界面不在本方案范围。
>
> **三期落地时的两处命名/挂点修订**（按需求方要求，与本文 §8.1/§9.1 的字面写法不同，以代码为准）：
> 槽位枚举是 **`AttachmentType.SUBWEAPON`**（JSON 里写 `"Slot": "SubWeapon"`），
> 不是 `UNDERBARREL`；渲染挂点骨骼是约定骨骼 **`sub_weapon_pos`**（挂点组名 `subweapon_rail`）。
> 换句话说：**副武器不再和"下挂导轨"这个概念绑定**，`SubWeapon` 定义仍然是唯一身份来源。
> （⚠ **骨骼名的准确写法是 `sub_weapon_pos`**，`AttachmentSlots.Bones.SUBWEAPON = "sub_weapon_pos"`；
> 本文 §11.8 / §12.5 里零星写的 `subweapon_pos` 是当时的手误，以代码为准。）

---

## 0. 结论速览

| 议题 | 结论 | 一期实现 |
|---|---|---|
| 判定形状 | 双角度限定的 `Cone` / `Box` / `Capsule` + **方向性扫掠采样**；盒体复用 `OBB.isColliding(obb, aabb)` | ✅ |
| 判定归属 | **只在客户端做**（与现状相同的信任模型） | ✅ |
| 攻击顺序 | `MeleeActions` 循环序列，**触发时锁存下标**；计数存在**客户端按枪隔离的计数器**（不能进 `GunState`，§5.2） | ✅ |
| 覆盖语义 | 按「独立可覆盖的轴」拆顶层字段；字段级缺省继承在代码里做 | ✅ |
| 命中区域 | 打头复用 `Headshot`（1.5）、打腿复用投射物默认 0.5，**不新增全局字段** | ✅（打头缺省继承枪的 `Headshot`，§11.2-⑤） |
| 伤害类型 | 新增 `gun_melee` / `gun_melee_headshot` + 标签 **`#superbwarfare:melee`**（含原版 `minecraft:player_attack`） | ✅ |
| 冷却 | **自定义**：仿 Perk 写在**枪械 NBT**，服务端逐 tick 递减；**绝不用原版物品冷却** | ✅ |
| 近战专属枪 | 显式 **`"Projectile": "@melee"`**；`empty`/`ray` 统一成 `@empty`/`@ray`（旧裸写法兼容） | ✅（缺口见 §11.2-⑫） |
| **MeleeEffect** | `Effects` 数据 + 概率 + 自定义冷却 + 11 个首发行为（§3.8） | ❌ **只有数据与校验，结算未实现**（§11.4-1） |
| **配件物品** | `AttachmentProvider` 接口 + `BasicAttachmentItem`（原 `AttachmentItem`）+ `SubWeaponItem : GunItem`（§8.3） | ❌ 二期 |
| **副武器手持** | 手持副武器物品时**按普通物品处理**：一个谓词 `useAsWeaponInHand()` + 约 40 处手持门禁（含 6 个改视角的 Mixin），数据层与安装链路不受影响（§8.3.1） | ⚠ 只有谓词（一期顺势加进 `GunItem`），门禁替换属三期 |
| **副武器定义** | `AttachmentDefinition.SubWeapon` POJO——**有它就是副武器**，与槽位无关（§9.1） | ❌ 三期 |
| **副武器运行时** | 寄生 `GunData`：合成栈用副武器物品本身 + 共享附件子 tag（§9.3） | ❌ 三期 |
| **G / V 语义** | **V 永远近战（恒用主武器，副武器没有近战）**；**G = 主武器 ↔ 副武器的切换**（§9.8） | ✅ 前半（G = 键位常量 `SUBWEAPON_FIRE`，一期时等同 V，三期时=触发一次射击，**四期改成切换**） |
| **被操控的枪（active gun）** | 客户端 `GunState.ActiveSlot`（写进宿主枪 NBT，**服务端权威**）+ 全仓统一的 `ActiveGun.stackOf/dataOf` 读取入口；**不改玩家主手物品**（§9.8.1） | ❌ 四期 |
| **"能不能当枪操作"的谓词** | 新增 `GunItem.isOperable(stack)`，与 `isHeldWeapon(stack)`（§8.3.1 的手持门禁）**分工**：前者回答"这个栈现在能不能被开火/换弹"，后者回答"这件物品拿在手上算不算枪"。副武器的 `useAsWeaponInHand()` 仍是 `false`，所以不分开就会把副武器整个挡在门外（§9.8.1 的坑） | ❌ 四期 |
| **动作互斥** | `GunActionLock`：开火/换弹/拉栓/近战/切换 任一占用期间其它入口全部拒绝（§9.5） | ✅（`SUB_WEAPON` 语义四期改成「切换中」，§9.8.8） |
| **副武器开火/换弹/瞄准** | **零专属代码**：全部走主武器的 `GunData.shoot` / `tryStartReload` / `zoom` 链路，只是 `GunData` 换成了副武器那一份（§9.8.3） | ❌ 四期（三期是"服务端代打"） |
| **副武器动画** | 副武器**有自己的枪械资源与动画文件**（`GunResource` 按物品 id 解析）：换弹动画就是它资源里的 `Animation.Reload`（`animation.sub_weapon_gp_25.reload`），换弹时它自己动、宿主枪继续播 `idle`，两套骨骼互不干扰（**不需要姿态融合**）；宿主枪侧只保留开火的 `fire_sub_weapon` | ⚠ 三期只有 `fire_sub_weapon`；副武器的 reload 动画需美术产出，**未产出时回退宿主枪的换弹动画** |
| **副武器瞄准位形** | 优先副武器**附件模型自己的** `iron_view`，没有则回退宿主枪的 `scope_view` / `iron_view`（§9.8.6） | ❌ 四期 |
| **双版本（1.20.1 Forge / 1.21.1 NeoForge）** | 抽出 `GunStackStorage` 单一适配点：`CompoundTag`（NBT）↔ `DataComponent` 两个实现，其余逻辑共用（§9.8.9） | ❌ 四期（三期直接读 `stack.tag`） |
| 与 Better Combat 的分野 | 它做玩家模型动画 + 按 combo 条件选动画、判定挂原版；我们做武器骨骼动画 + 数据定义的判定几何体（§3.6） | ✅ |

---

## 1. 现状盘点

> **本节描述的是「一期开工前」的代码状态**（保留了原始行号，便于回溯差异）。
> 一期完成后的链路、以及本节列出的 11 个缺陷的处置结果，见 §11.1（逐项核对表）与 §11.3（行为变化清单）。

### 1.1 链路

| 环节 | 现状 | 位置 |
|---|---|---|
| 触发 | 客户端 tick 轮询：「按住 V」或「meleeOnly 枪按住开火键」；`gunMelee == 0` 才允许下一次 → **长按连续挥击（有意设计，保留）** | `ClientEventHandler.kt:1476`（调用点 `:616`）、`ModKeyMappings.kt:111` |
| 时序 | `gunMelee = MELEE_DURATION`；每 tick 递减；`gunMelee == DURATION - DAMAGE_TIME` 那一帧结算 | `ClientEventHandler.kt:1491`、`:1494`、`:1499` |
| 结算 | 客户端算目标列表 → `MeleeAttackMessage(uuidList)` | `ClientEventHandler.kt:1504` → `MeleeAttackMessage.kt:57` |
| 判定 A | `findMeleeEntity`：单条射线；**命中实体时无条件覆盖方块结果** → 方块 pick 是死代码，**能隔墙打人** | `TraceTool.kt:66` |
| 判定 B | `seekLivingEntities(range, angle/2)`：3D 圆锥 + 球面距离 + `noClip` + 阵营/烟雾过滤 | `SeekTool.kt:503`、`:364`、`:281`、`:401` |
| 距离口径 | `e.position()`（目标**脚底**）到 `attacker.eyePosition` | `SeekTool.kt:462` |
| 排序 | 射线目标**强占 index 0**，其余按「眼→眼」夹角升序 | `ClientEventHandler.kt:1514-1523` |
| 伤害 | 服务端读 `Attributes.ATTACK_DAMAGE`（1.0 + `MELEE_DAMAGE`）× `max((10-i)/10, 0.1)` | `GunItem.kt:204-233`、`MeleeAttackMessage.kt:64` |
| 音效 | 挥击=`PLAYER_ATTACK_SWEEP`、命中=`MELEE_HIT`；已抽出 `MeleeSound` | `ClientEventHandler.kt:1505`、`MeleeAttackMessage.kt:124-134` |
| 动画 | `GunAnimation.Melee` 单字符串；速度按 `MELEE_DURATION` 拉伸 | `GunAnimation.kt:94`、`GeoGunAnimationInstance.kt:242`、`:114` |
| 动画资源 | 客户端资源声明动画名，clip 来自 `Model.Animation`；**`GunResource` 按物品 id 缓存**（TODO） | `DefaultGunResource.kt:97`、`GunResource.kt:16`、`:42` |
| 数据 | 5 个扁平标量 | `DefaultGunData.kt:89`、`GunProp.kt:97` |
| 配件物品类 | `AttachmentItem`（25 行）：`attachmentId` + `definition()` + `getTooltipImage`；全仓仅 **4 处**引用 | `item/attachment/AttachmentItem.kt:11-25` |
| 配件注册 | `ModItems.registerAttachment` 硬编码 `AttachmentItem(...)` | `ModItems.kt:626-628` |
| **安装路径** | **纯 id 驱动**：命令 `data.attachment.set(type, id)`、编辑界面发 `EditMessage`；物品类不参与安装 | `AttachmentCommand.kt:207`、`EditMessage.kt` |
| 配件可存任意子 tag | `Attachment.getOrCreateTag(slot)` 返回枪 NBT 里那个 compound 的**活引用** | `subdata/Attachment.kt:72-85` |
| 弹药槽基建 | `slot → intArray[ammo, virtual]`，在 `gunDataTag.AmmoSlot` 下 | `subdata/AmmoSlot.kt:17-43` |
| 数据解析 | `defaultDataId` 非空时基线从 `CustomData.GUN_DATA` 按 id 解析；为空时按**物品注册 id** 解析 | `GunData.kt:159-169`、`GunData.getDefault()` |
| `GunData` 缓存语义 | `DATA_CACHE` 是 **weakKeys（按栈实例身份）** + `UUID_CACHE` 按 UUID 兜底 adopt | `GunData.kt:1785-1854` |
| 开火入口 | `GunData.shoot(...)` 多重重载 → `item.shoot(...)`；`shootBullet` 读 `parameters.data` | `GunData.kt:994-1019`、`GunItem.kt:726-800` |
| 键位 | 近战 V、开火左键、瞄准右键、换弹 R、开火模式 N…（**G 空闲**） | `ModKeyMappings.kt:19-126` |

**`MeleeDamageTime` 的真实语义**：**从挥击开始算，第几 tick 结算**（不是距结束）。它就是 `HitTime`，只搬到动作级。

### 1.2 需要一并处理的现存缺陷

> **处置结果（一期）**：
> - 已修：**3 / 4 / 5 / 6 / 7 / 8 / 11**（第 11 项按"不用原版冷却"直接删掉那个死条件）。
> - 顺带修掉：**1**（`gunMelee` 全局单例 → 状态按枪隔离）、**2**（近战与开火无互斥 → `GunActionLock`）、**9**（三个字段漏写回 → 已补进 `withOverrides`）。
> - **10**（`meleeOnly()` 隐式判定）已显式化并保留兼容回退，详见 §7.2 与 §11.2-⑬。

| # | 缺陷 | 证据 |
|---|---|---|
| 1 | **`gunMelee` 全局单例且切枪不重置** → 切枪会拿新枪数据误触发一次攻击 | `ClientEventHandler.kt:301`；全仓没有 `gunMelee = 0` 赋值 |
| 2 | **近战与开火没有互斥**：`fireCooldown` 帧驱动、`gunMelee` 纯 tick | `ClientEventHandler.kt:1492` vs `:1696-1701`、`:1725` |
| 3 | **`player.swing` 双端各调一次** → 一次近战触发两次 `onEntitySwing` | `ClientEventHandler.kt:1526`、`MeleeAttackMessage.kt:54` |
| 4 | **`sweepAttack()` 在 `forEachIndexed` 循环体内** | `MeleeAttackMessage.kt:147-149` |
| 5 | **击退音效对每个目标无条件播放** | `MeleeAttackMessage.kt:68-77` |
| 6 | **把受害者的速度设成了攻击者的动量** | `MeleeAttackMessage.kt:118-122` |
| 7 | **`attack()` 在主手不是 GunItem 时也会执行**（健壮性） | `MeleeAttackMessage.kt:42-53` |
| 8 | **伤害衰减靠客户端列表下标** | `MeleeAttackMessage.kt:38`、`:64` |
| 9 | 三个近战字段不在 `withOverrides` 回写清单（读取仍优先 diff，覆盖生效） | `DefaultGunDataOverrides.kt:31-86` |
| 10 | **`meleeOnly()` 靠 `ProjectileAmount <= 0` 隐式判定**（§7） | `GunData.kt:1170` |
| 11 | `handleGunMelee` 里的原版冷却判断是死条件 → 本设计不用原版冷却，**建议删掉** | `ClientEventHandler.kt:1489` |

> 已确认不用管：长按 V 连续挥击（有意设计）、`MELEE`(V) 与 `RELEASE_DECOY`(V) 键位重复。

### 1.3 数据现状

- `MeleeDamage > 0` 的枪：**22 个** json；`MeleeAngle` 有 **19 个文件全写 `100`**；`MeleeRange` 只有 2 处；`MeleeDamageTime`/`MeleeSound` 全仓无配置。
- 近战模式切换唯一一例：`secondary_cataclysm.json:47-54`。

---

## 2. 目标与非目标

### 目标
1. 判定可配（形状/尺寸/距离/水平垂直角/遮挡/扫掠方向）。
2. 连招可配（`MeleeActions` 循环序列，每段独立动画/时长/判定/伤害/音效）。
3. 命中区域：打头/打腿（复用现有倍率）。
4. 额外效果：数据驱动预设 + 内联参数 + 概率 + 自定义冷却。
5. 近战专属枪械：`"Projectile": "@melee"`。
6. 伤害类型与标签：`gun_melee` + `#superbwarfare:melee`。
7. **配件物品接口化**（`AttachmentProvider` / `BasicAttachmentItem`）。
8. **副武器体系**：能力式 `SubWeapon` 定义 + 寄生 GunData + G 键。
9. **动作互斥**：近战/开火/换弹/切换不再互相穿透。
10. 槽位注册表化 + 挂点组基建（本期不启用互斥）。
11. **【四期】主/副武器切换**：G 键把"当前操控的枪"在主武器与副武器之间切换，
    副武器由此获得完整的开火/换弹/瞄准能力；近战恒用主武器（§9.8）。
12. **【四期】双版本（1.20.1 Forge / 1.21.1 NeoForge）物品数据存储适配**：把 NBT 与
    DataComponent 的差异收进一个适配点（§9.8.9）。

### 非目标（明确不做）
- 服务端几何复核 / 反作弊；第三人称动作（用 `swingHand`）；滞后补偿；骨骼级判定体；连招取消窗口；现有 `SwordItem` 近战武器迁移。
- **HUD 与改装界面**：包括副武器弹药显示、改装界面重做，属于后续自行重写的另一块。
- **【四期】不做真正的"换物品"**：`player.mainHandItem` 全程不变，玩家背包/快捷栏里不会凭空出现副武器物品（§9.8.1 的「为什么不换物品」）。
- **【四期】不做副武器专属 HUD/准心**：副武器激活后直接复用现有的弹药条 / 热量条 / 准心（它们读的就是"当前操控的枪"）。
- **【四期】不做 1.21.1 分支的完整移植**：只把版本相关的**存储访问**收进适配点，保证将来移植时不用重写业务逻辑。

---

## 3. 数据模型（近战本体）

### 3.1 为什么拆成多个顶层字段

属性覆盖的合并粒度是**顶层属性整块替换**（`JsonOverrideApplier.kt:50` → `Prop.deserialize` `Prop.kt:36`）。所以：

- ❌ 全部塞进一个 `"Melee": {...}`：刺刀只想换动作表，会把形状/伤害/时长打回默认值。
- ✅ 按「谁会被独立覆盖」拆字段。
- ✅ **字段级缺省继承在代码里做**（`action.hitbox ?: global.hitbox`）。

新增顶层属性都要进 `GunProp.entries` 并补进 `DefaultGunDataOverrides.withOverrides`（顺带补上现在漏掉的三个）。

### 3.2 全局字段（向后兼容）

| 字段 | 类型 | 默认 | 语义 |
|---|---|---|---|
| `MeleeDamage` | Double | 0 | 有值即代表能近战（`GunItem.hasMeleeAttack`）；**近战伤害的唯一出处** |
| `MeleeDuration` | Int | 16 | 单段总 tick；动作没写 `Duration` 时用 |
| `MeleeDamageTime` | Int | 6 | **从挥击开始算，第几 tick 结算**；动作没写 `HitTime` 时用 |
| `MeleeRange` | Double | **2.0** | 近战基础距离：叠加在 `MeleeHitbox.Range` 之上（§11.7.6） |
| `MeleeAngle` | Int | 30 | **只有显式写 `"Type": "Cone"` 时才用**（原来那个"没写 MeleeHitbox 就是圆锥"的兜底已经取消） |
| `MeleeHeadshot` | Double | 2.0（新） | **近战专用**打头倍率；不再复用投射物的 `Headshot` |
| `MeleeLegshot` | Double | 0.5（新） | **近战专用**打腿倍率 |
| `MeleeComboReset` | Int | 15 | 一段结束后多少 tick 内再挥击算连招 |

> 打头/打腿的倍率有独立属性了（二期后续调整），不再借用投射物的 `Headshot`：
> 一把枪的"打身子 15 / 打头 30"和它的"子弹爆头 2 倍"本来就是两回事。
> 个别动作仍可用 `MeleeAction.Headshot`/`Legshot` 单独覆盖。

### 3.3 `MeleeHitbox` —— 判定形状
```jsonc
"MeleeHitbox": {
  "Type": "Box",         // Box | Cone | Capsule（不写就是 Box）
  "Range": 1.2,          // 近战基础距离；不写时由枪的 MeleeRange 决定
  "Width": 1.8,          // Box：左右全宽
  "Height": 1.8,         // Box：上下全高
  "YOffset": -0.2,       // Box/Capsule：相对眼睛的垂直偏移（沿视线的"上"方向）
  "ZFrom": 0.0,          // Box/Capsule：沿视线的起点（负值=身后）
  "Angle": 100,          // Cone：水平总张角（度）
  "Pitch": 180,          // Cone：垂直总张角（度）；180 = 不限
  "Radius": 0.4,         // Capsule：截面半径
  "Occlusion": true      // 是否要求视线通畅
}
```

**三种形状的前向长度是同一个量** —— "近战触及距离"：

```
reach = (Range + MeleeRange) × 动作的 RangeMultiplier + player.getEntityReach()
```

所以盒子**没有 `Length` 字段**（二期后续调整删掉了）：枪的 `MeleeRange`、配件的距离加成、
动作的距离倍率都直接作用在判定体长度上，不用每个形状各写一套尺寸。

| Type | 定义 | 适用 |
|---|---|---|
| `Box` | OBB（中心 = 眼睛 + 局部上偏移 `YOffset` + 视线 × (`ZFrom` + `reach/2`)，半长 = (`Width/2`,`Height/2`,`reach/2`)）∩ 目标 AABB | **默认形状**，通用挥击 |
| `Cone` | 目标 AABB 最近点：距离 ≤ `reach`，`\|Δyaw\| ≤ Angle/2`、`\|Δpitch\| ≤ Pitch/2` | 旧形状，保留给数据包 |
| `Capsule` | 线段（沿视线 `ZFrom → ZFrom+reach`）到目标 AABB 最近距离 ≤ `Radius` | 突刺、枪管戳 |

> **`Box` 的朝向是 yaw + pitch 全姿态**（见 §11.2-㉑）：局部 **+Z** 指向**视线**（含俯仰），
> 局部 **+X** 是水平右方（与 pitch 无关），局部 **+Y** = `look × right`（与视线垂直的"上"）。
> `YOffset` 也沿局部 +Y 偏移，不再写死世界 Y —— 所以抬头劈砍时盒子会跟着抬起来，而不是横在头顶。


`Box` 直接用 `OBB`（`tools/OBB.kt:39`）+ `OBB.isColliding(obb, aabb)`（`:486`）。

> **实现注意（踩过坑，§11.2-⑳）**：`Box` 的"绕 Y 旋转 `yaw`"必须用
> `MeleeQuery.yawPitchQuaternion(yaw, pitch)` ＝ `Quaterniond().rotateY(-yaw).rotateX(+pitch)`。
> MC 的 yaw 与 JOML 的 `rotateY` **手性相反**（`+yaw` 会让判定体与朝向差 180°），
> 且 MC 的 pitch **向下为正**、JOML `rotateX(θ>0)` 也朝下，所以是 `+pitch`。
> 判定与调试渲染**必须共用这一个函数**。

**有意修正**：距离改为「到目标 AABB 最近点」；遮挡由 `Occlusion` 统一；俯仰角可单独限制；角度参照点统一。

> **`Cone` 的 `Pitch` 默认值提醒**：`Pitch` 是**总张角**，`Pitch: 70` 只有 `±35°` 容差，
> 比旧实现（垂直不限）**紧得多**，会出现"怪就在眼前却打不到"。
> 要与旧行为等价请写 **`"Pitch": 180`**（或干脆省略）。详见 §11.2-㉓ 的实测日志。

### 3.4 `MeleeSweep` —— 横扫

```jsonc
"MeleeSweep": { "From": -75, "To": 75, "Steps": 0 }
```

`From`/`To` 相对视线 yaw，**方向由符号决定**；`Steps = 0` 自动（**每 15° 一步，上限 8**）。不写 = 静态判定。

### 3.5 `MeleeActions` —— 动作序列

```jsonc
"MeleeActions": [
  "hit_lr",
  { "Animation": "hit_rl", "DamageMultiplier": 1.25 },
  { "Animation": ["hit_bayonet", "hit"], "Duration": 22, "HitTime": 9,
    "Hitbox": { "Type": "Capsule", "Range": 3.5, "Radius": 0.4 },
    "Effects": ["superbwarfare:heavy_impact"] }
]
```

| 字段 | 类型 | 缺省 | 语义 |
|---|---|---|---|
| `Animation` | `SingleOrList<String>?`（**候选链**，二期改型） | `GunAnimation.Melee[idx % size]` | 本段动画 clip：写字符串=单候选、写列表=按顺序取第一个存在的；**短名会拼成 `animation.<宿主枪 id>.<短名>`**（§11.5.3-①） |
| `Duration` | Int? | `MeleeDuration` | 本段总 tick（动画按它拉伸） |
| `HitTime` | Int? | `MeleeDamageTime` | 从挥击开始算，第几 tick 结算（见下方注） |
| `Hitbox` / `Sweep` | ? | 全局 | 本段判定 |
| `DamageMultiplier` | Double? | 1.0 | **伤害倍率**，乘在枪的 `MeleeDamage` 上（§11.7-②） |
| `RangeMultiplier` | Double? | 1.0 | **距离倍率**，乘在 `Range + MeleeRange` 上（不缩放 `getEntityReach()`） |
| `MaxTargets` / `Falloff` | Int? / Double? | 0（不限）/ 0.1 | 数量与衰减 |
| `SortBy` | enum? | `Angle` | `Angle`/`Distance`/`SweepOrder` |
| `Knockback` / `BypassesArmor` | Double? | 0.0 | 击退 / 穿甲 |
| `Headshot` / `Legshot` | Double? | 枪的 `MeleeHeadshot` / `MeleeLegshot` | 本段命中区域倍率**覆盖**（绝对值，不是倍率的倍率） |
| `Durability` | Int? | 0 | 本段消耗的枪械耐久 |
| `Cooldown` | Int? | 0 | 本段冷却（§3.7 的枪 NBT 冷却表） |
| `Swing` / `Hit` | `SerializedSoundEvent?`（**不是 String**，§11.2-④） | 枪的 `MeleeSound` | 本段音效 |
| `Effects` | `List<MeleeEffectSpec>?` | 空 | 本段额外效果（§3.8；**一期只解析不结算**） |

> **动作表里没有绝对伤害/距离**（二期后续调整）：数值的唯一出处是枪的 `MeleeDamage` / `Range + MeleeRange`，
> 动作只回答"这一段比别的段重多少、伸多远"。配件改数值走 `Modifiers`，动作改手感走倍率，两边不打架。

> **`HitTime` 的判定条件（实现时踩过坑，§11.2-①）**：内部计时器 `meleeTicks` 从 `Duration` 开始、**在每帧开头**递减，
> 所以「出伤那一帧」的条件是 `meleeTicks <= Duration - HitTime`，**不是** `<= HitTime`。
> 写成后者会让出伤晚整整 `Duration - 2*HitTime` tick（`Duration=16, HitTime=6` 时是第 11 tick 才打，动画早演完了）。
> 换算已收进 `ResolvedMeleeAction.hitTickFromStart`，`MeleeClientHandler.tickActiveSwing` 只认它。
> 另外 `HitTime` 会被夹进 `[0, Duration]`（写超界不再变成"永远不出伤"）。

**作用域**：`MeleeActions`/`MeleeHitbox`/`MeleeSweep`/`MeleeComboReset` **是 PMC 属性**；`MeleeAction` 内字段**不参与 PMC**，读取时 `?:` 继承。

### 3.6 与 Better Combat 的分野（避嫌）

| 维度 | Better Combat 那类做法 | 本设计 |
|---|---|---|
| 动谁的骨骼 | 玩家模型的手臂/身体 | **武器模型**（第一人称 bedrock）+ 原版 `swingHand` |
| 动画的角色 | 动画是**驱动**：按 `combo_count` 条件选一段 | 动画是**输出**：下标决定 clip，玩法 tick 决定结算 |
| 判定几何 | 挂原版攻击 | **数据定义判定体** + **方向性扫掠采样** |
| 序列语义 | 递进 combo，超时重置 | **循环序列**，每段独立几何/伤害/时长/音效/效果 |
| 玩法效果 | 动画条目基本只影响表现 | 每段可挂 `Effects` |
| 数据位置 | 客户端武器类别资源 | **武器数据（datapack）** |

**避嫌清单**：不做玩家模型攻击动画、不做 weapon category 资源体系、不做 `attack_animations` 条件列表、不做双持/两手武器。

### 3.7 冷却：仿 Perk，走**枪械 NBT**（绝不用原版物品冷却）

| 项 | 设计 |
|---|---|
| 存储 | `gunDataTag` 下新开子 tag（`"MeleeCooldown"`）：`冷却键 → 剩余 tick` |
| 递减 | **服务端**在 gun tick 内递减（`GunEventHandler.gunTickInternal` 的 `batch{}` 里），归零删键 |
| 判定 | 结算前读一次，`> 0` 则跳过 |
| 样板 | Perk 冷却：`data.perk.reduceCooldown(perk, "HealClipTime")`（`subdata/Perks.kt:217`） |
| 副武器冷却 | 同一张表（键 `sub:<slot>`），或直接用副武器数据的 RPM 周期 |

**为什么不用原版物品冷却**：`ItemCooldowns.addCooldown(item, ticks)` 是物品级冷却，会连带锁住整把枪的原版使用。**也不用全局 `Map<UUID, Int>`**：那会让"换一把同型号的枪"绕过冷却，还要处理登出清理与重启丢失。玩家级限制（若将来需要）另用 `persistentData`（先例 `mobeffect/RadiationMobEffect.kt:100-109`）。

### 3.8 `MeleeEffect` —— 近战额外效果

代码侧注册表 `ModMeleeEffects` + 数据侧预设 `sbw/melee_effects/*.json`。

```jsonc
"Effects": [
  "superbwarfare:heavy_impact",
  { "Effect": "superbwarfare:warhead_stab", "Chance": 0.5 },
  { "Type": "superbwarfare:explosion", "Chance": 0.25, "Radius": 4.0, "Damage": 60 }
]
```

| 字段 | 默认 | 语义 |
|---|---|---|
| `Effect` / `Type` | null | 预设 id / 行为 id（都有则预设 + 覆盖） |
| `Chance` | 1.0 | 只在**服务端** roll（`level.random`） |
| `Trigger` | `Hit` | `Hit`（每目标）/`FirstHit`（每挥击一次）/`Swing`（无论命中）/`Kill` |
| `Cooldown` | 0 | 该效果冷却（§3.7） |
| `Damage` `Radius` `DestroyBlocks` `FireTime` `Knockback` `Lift` `Duration` `Amplifier` `Count` `Sound` `Particle` | — | 由各行为解释 |
| `Extra` | null | 留给未来 |

**首发行为**（签名已核实，全部只在服务端执行）：

| 行为 | 实现 | 服务端性 |
|---|---|---|
| `explosion` | `CustomExplosion.Builder(directSource).damage/radius/position/destroyBlock/fireTime/damageSource/withParticleType().explode()`（`tools/CustomExplosion.kt:501`/`:586`） | `:587` 客户端 return |
| `extra_damage` | `DamageHandler.forceHurt(entity, source, damage)`（`tools/DamageHandler.kt:25`/`:32`） | `:42` 客户端 return |
| `shock` | `target.forceApplyEffect(MobEffectInstance(ModMobEffects.SHOCK.get(), dur, amp), attacker)`（`tools/EffectHandler.kt:10`） | `:22` 客户端 return |
| `potion` | 同上，任意 `MobEffect` | 服务端 |
| `ignite` | `setSecondsOnFire(ticks)` / `remainingFireTicks = N` / `forceApplyEffect(BURN)` | 服务端 |
| `knockback` | `target.knockback(...)` / `push(...)` / `addDeltaMovement(...)`；**上挑（`Lift`）无原语，需自己写 y 分量** | 服务端 |
| `lightning` | **无通用封装**（唯一一处在 `TaserBulletEntity.kt:77` 且绑 Creeper）→ 自己写 `thunderHit` | 服务端 |
| `heal` / `ammo_refund` | `attacker.heal(...)`；`data.ammo.set(...)` + `countBackupAmmo`/`consumeBackupAmmo` | 服务端 |
| `screen_shake` | `ShakeClientMessage.sendToNearbyPlayers(level, x, y, z, radius, time, amplitude)`（`:57`） | 服务端发、客户端执行 |
| `sound` / `particle` | `SoundTool.playDistantSound/playLocalSound`、`ParticleTool.sendParticle/spawnExplosionParticles` | 服务端发、客户端呈现 |

**概率**：三种随机源都是双端各自随机，只有服务端 `level.random` 权威 → `Chance` 只在服务端 roll。
**触发点**：`hurtEnemy` 只在服务端触发；`onEntitySwing` 双端都触发 → 效果只能挂服务端那个。

### 3.9 命中区域（打头 / 打腿）

```kotlin
val hitBoxPos = hitPos.subtract(target.position())
headshot = (target.eyeHeight - 0.25) < hitBoxPos.y < (target.eyeHeight + 0.3)
legshot  = hitBoxPos.y < 0.33 * target.bbHeight
```

复用投射物已验证的阈值（`ProjectileEntity.kt:305-315`、`IAdvancedHitDetection.kt:181-191`）。近战的 `hitPos` = **判定体到目标 AABB 的入射点**（`boundingBox.clip(eyePos, eyePos + look * range)`；已在判定体内时退化为 AABB 中心）。

倍率用**近战专用**的 `MeleeHeadshot` / `MeleeLegshot`（§3.2），不再复用枪的投射物 `Headshot`。

> **打头只认准星正对的那一个目标**（二期后续调整，见 §11.7-④）：
> 横扫会同时打到好几个目标，"入射点落在头部高度"并不等于"瞄着头打"。
> 客户端沿视线打一条射线取出准星目标（被方块挡住 / 超出 `reach` 时没有），
> 在报文里给每个目标带上 `aimed` 标记，服务端只在 `aimed` 为真时才算爆头。
> 打腿仍然是位置判定（所有命中目标都适用）。

### 3.10 完整示例

**A. AK-47：左右横扫循环 + 前方长方体判定**

```jsonc
"MeleeDamage": 15, "MeleeDuration": 16, "MeleeRange": 1.2,
"MeleeHitbox": { "Type": "Box", "Width": 1.8, "Height": 1.8, "YOffset": -0.2, "Occlusion": true },
"MeleeSweep": { "From": -60, "To": 60 },
"MeleeComboReset": 12,
"MeleeActions": [
  { "Animation": "hit_lr", "Sweep": { "From": -60, "To": 60 } },
  { "Animation": "hit_rl", "Sweep": { "From": 60, "To": -60 }, "DamageMultiplier": 1.2 }
]
```

**B. RPG：弹头戳人，概率大爆炸**

```jsonc
"MeleeActions": [
  { "Animation": "hit", "Duration": 24, "HitTime": 10,
    "Hitbox": { "Type": "Box", "Width": 0.9, "Height": 1.2, "YOffset": -0.2 },
    "Sweep": { "From": 0, "To": 0 },
    "RangeMultiplier": 1.3, "Knockback": 0.4, "Cooldown": 40,
    "Effects": [ { "Effect": "superbwarfare:warhead_stab", "Chance": 0.35, "Cooldown": 200 } ] }
]
```

**C. 纯近战枪械**

```jsonc
{
  "Projectile": "@melee",
  "MeleeDamage": 22, "MeleeDuration": 14,
  "MeleeHitbox": { "Type": "Box", "Width": 1.6, "Height": 1.6, "Length": 2.6, "YOffset": -0.3 },
  "MeleeSweep": { "From": -50, "To": 50 },
  "MeleeActions": [ { "Animation": "slash_1" }, { "Animation": "slash_2", "DamageMultiplier": 1.3 } ],
  "Magazine": 0, "Weight": 2.0
}
```

---

## 4. 判定实现

### 4.1 一个独立工具类，只在客户端用

```
MeleeQuery.resolve(eyePos, yaw, pitch, action, hitbox, sweep, candidates) -> List<Hit>
```

放 `tools/` 下；**只在客户端调用**。做成独立纯函数类是为了可单测、可被调试工具复用、改动只动一处。

### 4.2 判定形状保持解析式（建议，非硬约束）

不做服务端复核后理论上可跟随动画骨骼；**仍建议解析式**（可配置、可视化、成本低）。

### 4.3 粗筛 → 精判

①以玩家为原点、`Range + 扫掠外接半径` 构造 AABB，`level.getEntities(player, aabb, filter)`；②逐个形状精判；③过滤沿用 `BASIC_FILTER`（`SeekTool.kt:57`）+ `NOT_IN_SMOKE`（`:75`）+ `IN_SAME_TEAM`（`:140`），并统一"自己骑的载具"排除。

### 4.4 排序、数量与衰减

射线目标不再强占 index 0；`SortBy` 默认 `Angle`；`MaxTargets`/`Falloff` 可配；伤害直接读数据字段。

### 4.5 方向基准：**采用方案 1**（结算 tick 用玩家当前朝向作为 0°）

---

## 5. 时序、连招与动画

### 5.1 玩法时间线归玩法，动画跟着拉伸

```
t=0        触发：锁存 actionIndex，播 swing 音效，meleeTimer = action.Duration
t=HitTime  结算：客户端判定 → 发报文；服务端结算伤害/效果/命中音效
t=Duration meleeTimer 归零，允许下一段（按 MeleeComboReset 决定是否重置连招）
```

> 落地时的**逐 tick 约定**（`MeleeClientHandler.tickActiveSwing`）：
> `meleeTicks` 从 `Duration` 起、**每帧开头**递减，出伤条件是 `meleeTicks <= Duration - HitTime`，
> 即「从挥击开始算第 `HitTime` tick 结算」。等价于旧实现 `gunMelee == MELEE_DURATION - MELEE_DAMAGE_TIME` 那一帧，
> 22 把旧枪的手感因此零变化。踩过的坑见 §11.2-①。

动画：`playbackSpeed = clip.specifiedEndTimeMs / (action.Duration / 20f)`。

**为什么时间线放在 `MeleeAction` 而不是复用 `ActionSteps`**：`GunActionStepExecutor` 读 `data.getDefault().actionSteps`（`:10`），**不经过 PMC**，配件/弹种覆盖不了——而"刺刀换动作"的核心恰恰是覆盖。

> **实现补充：连续挥击必须让动画重播（§11.2-②）**。
> 动画状态机只在**状态切换**那一帧重建 runner，而按住 V 连续挥击时 `resolveState()` 的目标与 `currentState` 一直是 `MELEE`
> ——状态没变，`PLAY_ONCE_HOLD` 的动画就停在上一段的末帧，**只有第一段会播**。
> 所以 `MeleeClientHandler` 每次触发挥击都 `swingSerial++`，`GeoGunAnimationInstance` 按
> `swingSerial > consumedMeleeSerial` 重播（与开火那套 `fireSerial` / `consumedFireSerial` 同一套路），
> 且**序号必须在 runner 判定之前消费**，否则第一段会被重播两次。

### 5.2 「取下标就可以了吧？」——可以，2 个必须处理的点

**① 下标在挥击开始时锁存**（动画状态机只在状态切换那一帧解析名字，`GeoGunAnimationInstance.kt:703`）。

**② 下标不能放进 `GunState`**：`GunState` 全部字段服务端权威，客户端只能 `updateLocal`（不写 NBT、不 bump revision，`GunData.kt:1380-1388`），服务端从不写这个字段 → **任何一次服务端同步都会冲掉客户端计数**。
→ **客户端本地、按枪隔离的连招计数器**（key = 枪 UUID 或 `GunData` 实例；副武器另按 `(枪, 槽位)` 隔离），切枪重置、超时重置；下标随报文发送，服务端只做**越界健壮性**校验。这同时修掉缺陷 1。

> **落地形态**：连招下标、动作锁、本段时长、挥击序号**同住**在一个按枪隔离的 `GunActionLock.State` 里
> （`WeakHashMap<UUID, State>` + `WeakHashMap<GunData, State>` 兜底），见 `client/gun/GunActionLock.kt`。
> `ClientEventHandler.gunMelee` 因此**退化成恒为 0 的 `@Deprecated` 占位字段**，只为旧 GeckoLib 路径编译通过（§11.2-⑮）。

### 5.3 连招重置窗口

`MeleeComboReset`（默认 15 tick）：窗口内再挥击进下一段，超时回到第 0 段。

### 5.4 动画资源侧的变化

| 项 | 现在 | 变化 |
|---|---|---|
| `GunAnimation.Melee` | `String?` | `SingleOrList<String>`（写字符串=单段，旧数据零改动） |
| `MeleeAction.Animation` | `String?`（全名） | `SingleOrList<String>?`（**候选链** + 短名拼接，二期；见 §11.5.3-①） |
| 名字解析 | `animation.melee` | `action.Animation` 逐个候选（拼接后）→ 全落空才用 `melee[idx % size]` |
| 找不到 clip | 静默 return | error 日志 + 回退 `melee[0]`（同一条失败只打一次，不刷屏）；资源加载后校验 |
| 状态 | `MELEE`（`PLAY_ONCE_HOLD`） | 不变，不需要 `MELEE_2/3` |
| 播放速度 | 全局 `MELEE_DURATION` | 本段 `Duration` |
| **连挥重播** | — | 新增 `swingSerial` 机制（§5.1 末尾、§11.2-②） |

**可复刻的先例**：弹匣等级 → 鼓式换弹动画（`GunData.kt:71`/`:84` → `GeoGunAnimationInstance.kt:162-176`）。

> **落地补充**：`找不到 clip` 的 error 日志 + 回退**已实现**（`resolveMeleeName()`：先按顺序试
> `action.Animation` 的每个候选，全落空才回退 `GunAnimation.Melee[0]`；同一条失败只打一次日志）；
> 但"**资源加载后校验**"（在资源 reload 阶段就把所有枪的 melee clip 名核一遍）
> **未实现**，目前只在运行时第一次播放时才会打日志。另要注意：**动作表只能来自 `GunData`（PMC，按 stack）**，
> `GunResource` 是按物品注册 id 缓存的，配件/弹种覆盖看不到它（§8.6 的坑）——
> 也正因为如此，动作表里的动画名只能写**短名**再按宿主 id 拼接（§11.5.3-①）。

### 5.5 第三人称：放弃，用现成的 swingHand

沿用原版 `player.swing`。→ 顺带修缺陷 3：改成**只在服务端 swing**。

> **⚠ 落地时改到了另一边：只在「客户端」swing**（`MeleeClientHandler.resolveHit`）。
> 这样做同样能修掉缺陷 3（双端各调一次 → `onEntitySwing` 触发两次），而且**第三人称动作更跟手**：
> 服务端那条路径要等报文回来才挥手，客户端本地挥手是零延迟的。
> 副作用：专用服务端上（无客户端）不会有挥手动作 —— 与"判定只在客户端做"的信任模型一致，可接受。

---

## 6. 网络、伤害与效果

### 6.1 报文

```
MeleeAttackMessage(
    source: MeleeSource,   // MAIN（主武器近战）/ SUB:<slot>（副武器的近战形态）
    actionIndex: Int,      // 客户端锁存的连招下标（服务端只做越界校验）
    targets: List<UUID>
)

SubWeaponFireMessage(
    slots: List<String>,   // 本次触发的副武器槽位（通常 1 个；§9.4 的遍历结果）
    spread: Double, zoom: Boolean,
    uuid: SerializedUUID?, targetPos: SerializedVector3f?, power: Double = 1.0
)
```

服务端：`isSpectator` → 主手是 GunItem → **动作锁检查** → 解析 `source`/`slots` 对应的 `GunData`（副武器时按 §9.3 装配）→ 结算 / 发射。**不做距离/角度/视线复核**。

> **落地差异（§11.2-⑥⑦）**：
> 1. `targets` 不是 `List<UUID>`，而是 `List<TargetPayload>`，每个元素带 **`hitPos`**（判定体到目标 AABB 的入射点）。
>    原因是**打头/打腿必须由服务端算**（`gun_melee_headshot` 要选对伤害类型），客户端不能再自己算一遍。
> 2. 报文里**没有 `MeleeSource` 枚举**，就是一个 `String`：`"MAIN"` / `"SUB:<slot>"`，默认值 `MeleeAttackMessage.SOURCE_MAIN`。
> 3. 服务端那层"廉价检查"落地为：`data.reloading() || data.bolt.actionTimer.get() > 0` → 直接 return（不结算、不扣耐久）。

### 6.2 伤害类型与标签

1. 新增 `superbwarfare:gun_melee`（爆头 `gun_melee_headshot`）（`ModDamageTypes.kt:17-55` 的写法）。
2. 新增标签 **`#superbwarfare:melee`**（`ModTags.DamageTypes.MELEE`，与 `GUN_DAMAGE` 同套路，`ModTags.kt:206-240`）：`minecraft:player_attack` + `superbwarfare:gun_melee` + `gun_melee_headshot`；数据包/其它模组可自行加入。
3. `DamageTypeTool.isMeleeDamage(source) = source.is(MELEE)`。
4. **必须同步改的判断点**：`LivingEventHandler.kt:227`、`:258`、`:500`、`:534`，`perk/functional/PowerfulAttraction.kt:27`、`:48`、`:64` → 统一成 `isGunDamage(source) || isMeleeDamage(source)`。
5. 原版语义保留：`setLastHurtMob`、`doPostHurtEffects/doPostDamageEffects`、`awardStat`、`crit` 粒子、击杀归属。
6. 打头走 `gun_melee_headshot`；`DamageTypeTool.isHeadshotDamage`（`:25`）加上它。
7. 伤害数值直接读 `MeleeDamage`；`getAttributeModifiers` 的 `ATTACK_DAMAGE` 加成**保留**。
8. 穿甲走 `DamageHandler.forceHurt`。

> **落地差异（§11.2-③④⑤）**：
> - 第 5 条全部保留；但"**击退音效**"与"**无伤害提示音**"按**每次挥击最多各响一次**（旧实现是每个目标都响，缺陷 5）。
> - 第 6 条实现为 **`MeleeQuery.isHeadshot` / `isLegshot`**（同一套阈值，落在 `tools/MeleeQuery.kt`），服务端按报文里的 `hitPos` 判定。
> - 第 7 条：`getAttributeModifiers` 的 `ATTACK_DAMAGE` 加成**保留但不再参与近战结算**（见 §11.2-③ 的说明）。
> - 第 8 条实现为**按 `BypassesArmor` 拆成「护甲段 + 穿甲段」两段伤害**：护甲段 `target.hurt(...)`，
>   穿甲段 `target.forceHurt(...)`。不用一个 `DamageSource` 打两遍，是因为 `hurt()` 自带 10 tick 无敌帧，
>   第二段会被 `damage <= lastHurt` 判掉。

### 6.3 Perk 钩子

保留 `onMeleeSwing`/`onMeleeAttack`；判断条件换成 `#superbwarfare:melee` 标签；补上「本段动作 + 来源（主武器/副武器槽位）」上下文。

---

## 7. 近战专属枪械（`meleeOnly` 的显式化）

### 7.1 问题

`meleeOnly()` 现在是 `ProjectileAmount <= 0 && MeleeDamage > 0`（`GunData.kt:1170`）——「不发射」靠**弹丸数量**隐式表达。

### 7.2 `"Projectile": "@melee"` + `@` 前缀统一（已定稿：**统一加 `@`**）

1. `Projectile` 的取值有两种性质：**引擎保留标记**与**资源 id**，裸词无法从字面区分。
2. 模组其它字段**已是 `@` 约定**（`"@Ammo"`/`"@GunDefault"`/`"@ShotgunAmmo"`/`"30 @RifleAmmo"`）→ 统一后只需记一条：**`@` 开头 = 引擎语义**。
3. 兼容成本近零：解析处 `trim().lowercase().removePrefix("@")` 归一化；全仓只有 5 个文件写了 `ray`（`laser_tower.json:24`、`prism_tank.json:97`、`waveforce_tower.json:22`、`annihilator.json:134`、`repair_tool.json:16`），顺手改 `@ray`。

**为什么不用 `GunType: "Melee"`**：`Projectile` 本就承载模式标记（`GunItem.kt:738-746` 的 `empty`/`ray` 分支）；两者语义**天然互斥**；且弹药层能覆盖 `PROJECTILE`（`AmmoConsumer.kt:209`），"切到近战弹种"天然成立。

**做法**：`GunItem.shootBullet` 加 `@melee` 早退分支；`meleeOnly() = projectileIsMelee() && hasMeleeAttack()`；**兼容回退**保留 `ProjectileAmount<=0 && MeleeDamage>0` 一个周期（DataValidator 提示迁移）。

### 7.3 近战枪械的缺口清单

| 缺口 | 建议 |
|---|---|
| 左键仍走开火消息（`ClickEventHandler.kt:542`） | `@melee` 时左键**直通近战输入** |
| 右键瞄准 | `CanZoom: false` |
| 耐久 | `MeleeAction.Durability` |
| 动画兜底 | 文档写明"近战枪资源只需 `Idle` + `Melee`" |

> 现有 `SwordItem` 近战武器**明确不迁移**。

---

## 8. 配件体系（物品类层次、槽位、挂点组）

### 8.1 槽位注册表化（先做这个）

"槽位"的元数据（渲染分派、编辑界面按钮、焦点骨骼、标签桶、挂点组）现在散在 8 处硬编码，后续要加很多配件类型，先收进一张注册表：

```kotlin
object AttachmentSlots {
    val BY_TYPE = mapOf(
        AttachmentType.BAYONET     to AttachmentSlot(type = BAYONET,     mount = "muzzle_lug",       tagBucket = "bayonet",     focusBone = BAYONET_POS),
        AttachmentType.UNDERBARREL to AttachmentSlot(type = UNDERBARREL, mount = "underbarrel_rail", tagBucket = "underbarrel", focusBone = null),
        // 现有 5 个槽位照搬进注册表
    )
}
```

**渲染骨骼（已定稿）**：约定新增 **一个** `bayonet_pos`（刺刀用）；其它槽位/配件一律走**配件自己的 `AttachmentDefinition.Bone`**（现有机制，barrel 槽就是这么用的，`GeoGunRenderer.kt:744-748`）——下挂榴弹的挂点由它自己的 json 声明。

### 8.2 挂点组（mount group）：**保留基建，本期不互斥**

- 刺刀（枪口卡榫）与下挂榴弹（下导轨）装在不同挂点，**可以共存且同时可用**。
- 两者登记**不同的 `mount`**（`"muzzle_lug"` / `"underbarrel_rail"`）→ **不互斥**。
- 机制保留：将来若两个配件真的抢同一个挂点，把它们登记到同一个 `mount` 名即可自动互斥（检查落点 `Attachment.canInstall` / `installed()`，`Attachment.kt:238`）。
- 配件可用 `AttachmentDefinition.Mount` 覆盖槽位默认值；`AllowSharedMount` 特例默认关。

### 8.3 **配件物品接口化**（v6 新增，已定稿）

现状：`AttachmentItem`（25 行）只有 `attachmentId` + `definition()` + `getTooltipImage`，全仓 **4 处**引用；**安装是纯 id 驱动**（命令 `data.attachment.set(type, id)`、编辑界面发 `EditMessage`），所以改类层次**对存档与数据零影响**。

**目标形态**：

```kotlin
/** 「能作为配件安装的物品」。只声明 id —— definition() 做成扩展函数，避免 Kotlin 接口默认实现在 Java 实现类上失效 */
interface AttachmentProvider {
    val attachmentId: String
}

fun AttachmentProvider.definition(): AttachmentDefinition? = AttachmentDefinition.from(attachmentId)

/** 原 AttachmentItem 改名 */
open class BasicAttachmentItem @JvmOverloads constructor(
    override val attachmentId: String,
    rarity: Rarity = Rarity.COMMON,
) : Item(Properties().rarity(rarity)), AttachmentProvider { /* getTooltipImage 同原实现 */ }

/** 副武器：既是配件，又是一把真枪 */
class SubWeaponItem @JvmOverloads constructor(
    override val attachmentId: String,
    rarity: Rarity = Rarity.COMMON,
) : GunItem(Properties().rarity(rarity)), AttachmentProvider
```

> 接口只声明 `attachmentId`、`definition()` 放扩展函数，是为了绕开仓库里已经记录过的那个坑：Kotlin 接口的默认实现无法被 Java 实现类继承（见 `DataCodec` 的 KDoc，`data/DataCodec.kt:7-13`）。

**注册**（`ModItems.kt:626-628` 改成带工厂）：

```kotlin
private fun registerAttachment(id: String, rarity: Rarity = Rarity.COMMON, factory: (String, Rarity) -> Item = ::BasicAttachmentItem) =
    ATTACHMENTS.register(id) { factory("${Mod.MODID}:$id", rarity) }

fun registerSubWeapon(id: String, rarity: Rarity = Rarity.COMMON) =
    registerAttachment(id, rarity, ::SubWeaponItem)
```

**消费点改为接口判断**（全部 4 处）：

| 位置 | 现在 | 改为 |
|---|---|---|
| `ModItems.kt:627` | `AttachmentItem(...)` | 上面的工厂 |
| `ClientAttachmentImageTooltip.kt:43` | `stack.item as? AttachmentItem` | `stack.item as? AttachmentProvider` |
| `AttachmentCommand.kt:267` | `ForgeRegistries.ITEMS.getValue(id) !is AttachmentItem` | `!is AttachmentProvider` |
| `AttachmentItem.kt` 自身 | — | 改名 `BasicAttachmentItem.kt` |

**为什么要这么做**：副武器必须是 `GunItem`（`GunData.item` 的类型要求），而它同时又要能被当作配件安装 → 用接口表达"配件身份"，比 `is AttachmentItem` 或"给 GunItem 加个 isAttachment 标记"都干净；将来出现"能装到枪上的其它物品"（例如某种消耗品式配件）也不用再改判断。

### 8.3.1 副武器物品手持时必须按普通物品处理（v6 补充）

**问题**：`SubWeaponItem` 继承 `GunItem`，而全仓有约 **150 处** `item is GunItem` / `instanceof GunItem` 判断，其中 **6 个 Mixin 直接改手持表现与视角**——手持副武器会被当成"正在持枪"：

| Mixin | 原本的作用 | 手持副武器时的后果 |
|---|---|---|
| `ItemInHandRendererMixin.java:18` | 主手是枪时把装备/切换动画进度强制为 0 | 副武器没有装备动画 |
| `ItemInHandLayerMixin.java:35` | 主手是枪时**隐藏副手物品** | 手持副武器会让另一只手的东西消失 |
| `CameraMixin.java:117` | 主手是枪且开镜/拉弓时第三人称相机前移 | 手持副武器时相机莫名拉近 |
| `GameRendererMixin.java:40` | 主手是枪时改 FOV（开镜） | 手持副武器被当成开镜 |
| `HumanoidModelMixin.java:132`、`EntityMixin.java:60` | 主手是枪时改游泳姿态 | 手持副武器泳姿异常 |

**方案：一个统一谓词 + 只在"手持边界"做门禁**（不动那 150 处，只动真正与手持相关的约 40 处）：

```kotlin
// GunItem
/** 手持时是否按"枪械"处理（渲染/视角/输入/属性/HUD）。副武器类配件覆盖为 false —— 它只有装在正常枪械上才生效 */
open fun useAsWeaponInHand(): Boolean = true

companion object {
    /** 供 Java / Mixin 调用的静态入口 */
    @JvmStatic fun isHeldWeapon(stack: ItemStack): Boolean = (stack.item as? GunItem)?.useAsWeaponInHand() == true
}
```

`SubWeaponItem` 覆盖 `useAsWeaponInHand() = false`；所有门禁点把 `instanceof GunItem` / `is GunItem` 换成 `GunItem.isHeldWeapon(stack)`。

**必须改的门禁点**：

| 分组                            | 位置                                                                                                                                                                                                                                                                                                                                                                | 说明                                                       |
|-------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------|
| **A. 手持表现与视角**（你说的那批）         | `ItemInHandRendererMixin.java:18`、`ItemInHandLayerMixin.java:35`、`CameraMixin.java:117`、`GameRendererMixin.java:40`、`HumanoidModelMixin.java:132`、`EntityMixin.java:60`                                                                                                                                                                                           | 6 个 Mixin，全部换成静态谓词                                       |
| **B. 客户端输入与手持状态**             | `ClickEventHandler.kt` 11 处（`:75`、`:82`、`:136`、`:177`、`:216`、`:326`、`:334`、`:419`、`:503`、`:572`、`:642`）；`ClientMouseHandler.kt:305`；`ClientEventHandler.kt` 约 25 处（`:723`、`:961`、`:1379`、`:1478`、`:1627`、`:1813`、`:1862`、`:1909`、`:2105`、`:2206`、`:2242`、`:2254`、`:2282`、`:2303`、`:2491`、`:2529`、`:2637`、`:2782`、`:2967`、`:3091`、`:3109`、`:3189`、`:3230`、`:3274`） | 开火/换弹/开镜/挥动/后坐/摇摆/近战（`:1478` 是 `handleGunMelee` 入口）等手持逻辑 |
| **C. HUD / 物品呈现**             | `CrossHairOverlay.kt:82`、`AmmoBarOverlay.kt:85`/`:512`、`HeatBarOverlay.kt:33`、`HandsomeFrameOverlay.kt:35`、`ItemRendererFixOverlay.kt:17`、`ClientGunImageTooltip.kt:86`/`:140`                                                                                                                                                                                    | 准心、弹药条、热量条、枪械 tooltip                                    |
| **D. 物品自身行为**                 | `GunItem.inventoryTick`（`:184`）、`getAttributeModifiers`（`:204-235`）、`getMaxDamage`/`isDamageable`（`:251-257`）、`getTooltipImage`（`:237-239`）、`getItemScreen`（`:1189-1197`）                                                                                                                                                                                         | 见下方逐条                                                    |
| **E. 列表/工具类**（顺手排除，别把它当"一把枪"） | `SbwJEIPlugin.kt:55`、`KillMessageOverlay.kt:323`/`:422`/`:430`、`ReforgingTableMenu.kt`（`:109`/`:161`/`:182`/`:296`/`:345`/`:379`/`:429`/`:472`）、`WeaponEditScreen.kt:41`/`:45`/`:309`、`GunShootGoal.kt`（`gunData()`）、`MobGunData.kt`（`gunData()`/`gunIdOf()`）                                                                                                     | 生物用枪、重铸台、编辑界面要求手持真枪                                      |

**D 组的逐条处理**：

| 位置 | 处理 |
|---|---|
| `GunItem.inventoryTick:184` | `!isHeldWeapon(stack)` 时直接 return——别给"躺在背包里的配件"跑枪械状态机（`data.tick`/换弹/热量）。**注意**：这不影响装在枪上的副武器，它由主武器 tick 顺带 tick（§9.3） |
| `getAttributeModifiers:204-235` | `!isHeldWeapon(stack)` 时只返回 super——不加移速惩罚（按 Weight）、不加近战伤害属性 |
| `getMaxDamage`/`isDamageable:251-257` | 副武器物品**不带耐久条**（`isDamageable = false`） |
| `getTooltipImage:237-239` | 返回**配件 tooltip**（`AttachmentImageComponent`，走 `AttachmentProvider`），而不是枪械 tooltip；实现方式与 `BasicAttachmentItem` 一致 |
| `getItemScreen:1189-1197` | 手持副武器**不打开改装界面**（`canOpenEditScreen` + 调用的 `canEditAttachments` 都要过谓词） |

**不用改的（关键：数据层与安装链路完全不受影响）**：

- `GunData` / PMC / `GunProp`：副武器装在枪上时照样有完整 `GunData`——`SubWeaponRuntime` 直接调 `GunData.shoot(...)`，**不经过任何"手持"判定**；
- `MeleeAttackMessage` 服务端判定（`:42`，看的是**主手那把枪**）；
- 安装链路（纯 id 驱动）与 `AttachmentDefinition` 查找；
- `GunItem.initCapabilities:100-111`：给副武器物品挂能量能力无害（挂在栈上，不是挂在手上），保留即可。

**备选方案（改动面更小，但少了"一物一枪"的直白）**：让 `SubWeaponItem : BasicAttachmentItem`（纯配件物品，**不是** `GunItem`），副武器的合成栈改用一个**共享的隐藏枪物品 + `defaultDataId`**（即 v5 的做法）。这样上面 A–E 的门禁**一个都不需要**；代价是 `SubWeapon.Data` 变成必填，且每个副武器要维护"物品 id + 数据 id"两个 id。当前按你的方案（`SubWeaponItem : GunItem`）执行，上面这份清单就是它的成本。

### 8.4 配件改近战动作（刺刀的形态）

```jsonc
"Override": {
  "MeleeActions": [
    { "Animation": ["hit_bayonet", "hit"], "Duration": 18, "HitTime": 7,
      "Hitbox": { "Type": "Capsule", "Range": 3.2, "Radius": 0.4 }, "Sweep": { "From": 0, "To": 0 },
      "DamageMultiplier": 1.4 }
  ]
}
```

装了就换成刺刀的动作表；形状/扫掠/标量不受影响（§3.1 拆字段的回报）。**刺刀不带 `SubWeapon` 定义**——它不是副武器（§9.1）。

> **动画名必须写短名**：动作表住在会被多把枪共用的枪械数据里，写不了某一把枪的完整 clip 名。
> `"Animation"` 是**候选链**，短名按 `animation.<宿主枪 id>.` 拼接、按顺序取第一个存在的
> （`["hit_bayonet", "hit"]` = 有刺刀动画就用、没有就用枪自己的挥击）。详见 §11.5.3-①。
> 二期实际落地时**没有**用这里的 `MeleeRange`：距离由 `Modifiers` 的 `MeleeRange +1.2`
> 与动作的 `Hitbox.Range` 相加得到，标量统一在 `Modifiers` 里（§11.5.3-②）。

### 8.5 新增槽位的完整改动清单

| 环节 | 位置 |
|---|---|
| 枚举 + 注册表登记 | `AttachmentType.kt:7`、（新）`AttachmentSlots` |
| 物品注册 | `ModItems.registerAttachment`（带工厂后，普通配件与副武器共用） |
| 物品 tag + datagen | `ModTags.kt:105-153`、`ModItemTagProvider.kt:485-696`、`:698-738`（`addAttachmentItems` 只吃 item，不检查类） |
| 枪上渲染（逐槽硬编码） | `GeoGunRenderer.renderAttachments:410-429` + 注册表化 |
| 编辑界面 | `WeaponEditScreen.kt:202-229`、`:312-320`、`:327-348`（**注：改装界面后续自行重写，本期只保证不崩**） |
| 编辑消息 | `EditMessage.kt:43-63` |
| 枪物品开关 + 焦点骨骼 | `GunItem.hasCustomXxx`（`:277-312`）、`attachmentFocusBone:1160-1170` |
| 老 GeckoLib 路径 | `ItemModelHelper.java:11-20`、`:48-55` |
| 语言 / 模型 / 贴图 | `attachment.superbwarfare.slot.<slot>`；`models/bedrock/attachment/*.geo.json` 等 |

### 8.6 必须绕开的坑

| 坑 | 说明 | 位置 |
|---|---|---|
| `GunResource` 按物品 id 缓存 | 动作表必须来自 `GunData`（PMC，按 stack） | `GunResource.kt:16`、`:42` |
| `Attachment.installed()` 校验 `slot == type` | 槽位写错 → 配件完全不参与计算且不报错 | `Attachment.kt:238` |
| `Modifiers` 的 `Prop` 未注册 | **抛 `IllegalArgumentException`** | `PmcProxy.kt:10-13` |
| `Override` 顶层整块替换 | 缺键落回类型默认值 | `Prop.kt:36`、`:82-84` |
| scope zoom 末尾无条件 `set` | 带 `ScopeInfo`/`Zoom` 的配件会覆盖三个 zoom 属性 | `AttachmentDefinition.kt:103-107` |
| 全局钳制永远在最后 | `MELEE_DAMAGE_TIME ≤ MELEE_DURATION-1` 等 | `GunProp.kt:490-503` |

### 8.7 前置修复：override 嵌套解析统一改宽松（一期做）

`JsonOverrideApplier` 解析 override 的嵌套对象用 kotlinx 默认 `Json`（`ignoreUnknownKeys=false`，`Prop.kt:36-39`），数据文件本体走宽松的 `DataLoader.JSON` → 同一字段写在数据文件里能容错，写在 `Override` 里拼错一个键就**整块静默失效**。近战配置会大量写在 override 里，**统一改宽松 + 保留显式日志**。

---

## 9. 副武器（SubWeapon）

> **阅读顺序提示**：本节 §9.1–§9.7 是**三期**的设计与落地记录，其中"G = 触发一次副武器射击"
> 这条主线已被**四期**取代——**G 改成「主武器 ↔ 副武器切换」**，规范见 **§9.8**，落地方案见 **§11.10**。
> 冲突时**以 §9.8 / §11.10 为准**。仍然完全有效的部分：§9.1（能力式定义）、§9.3（寄生 GunData 与
> 五条不变量）、§9.5（`GunActionLock`）、§9.7 的渲染挂点结论。

### 9.1 定义：能力式，而不是槽位式

> **`AttachmentDefinition` 上新增 `SubWeapon` POJO。任何槽位的配件，只要带这个定义，就算副武器。**

- 下挂榴弹 = `Underbarrel` 槽 + `SubWeapon` 定义 → 是副武器；
- 刺刀 = `Bayonet` 槽 + **没有** `SubWeapon` 定义 → 不是副武器，只改主武器近战动作表；
- 未来"上挂霰弹"或"枪托内置发射器"只要加 `SubWeapon` 定义就是副武器，**不需要新槽位类型**。

**两档配件的分工**：

| 配件类型 | 带 `SubWeapon`？ | 干什么 | V 键 | G 键 |
|---|---|---|---|---|
| **改近战类**（刺刀、枪托、握把…） | 否 | 用 `Override` 改主武器近战动作表/参数 | 主武器近战（动作表已被 override） | **等同 V** |
| **副武器类**（下挂榴弹、下挂霰弹…） | 是 | 提供一把独立的、寄生在主武器上的枪 | 主武器近战 | **使用副武器** |

### 9.2 `SubWeaponInfo` POJO

```jsonc
// sbw/attachments/sub_weapon_gp_25.json
{
  "Slot": "SubWeapon",
  "Bone": "sub_weapon_pos",
  "SubWeapon": {
    "Data": null,                        // 可选：默认 null = 用附件自身 id 对应的 sbw/guns/sub_weapon_gp_25.json
    "AmmoSlot": "SubWeapon",             // 副武器自己的弹药槽（默认 SubWeapon，与主武器完全分开）
    "Animation": ["fire_sub_weapon"],    // 可选：副武器开火时**宿主枪**的动画候选链（不写就是这一条），见 §11.9
    "ReloadSound": "...", "ReloadEndSound": "..."
  },
  "Model": "...", "Texture": "..."
}
```

```jsonc
// sbw/guns/sub_weapon_gp_25.json —— 与配件同名，就是这把副武器的枪数据
{
  "Projectile": "superbwarfare:gun_grenade",
  "Damage": 80, "ExplosionDamage": 80, "ExplosionRadius": 5,
  "AmmoType": "superbwarfare:grenade_40mm", "Magazine": 1,
  "RPM": 60, "Weight": 1.5,
  "SoundInfo": { "Fire1P": "...", "Fire3P": "..." }
}
```

- **`Data` 默认取附件自身 id**：因为副武器物品本身就是 `GunItem`，`GunData.getDefault()` 在 `defaultDataId` 为空时会走 `item.getDefaultData(this)` → 按**物品注册 id** 从 `CustomData.GUN_DATA` 解析（`GunData.kt` 的 `getDefault`）。所以同一物品 id 下"配件定义 + 枪数据"成对出现即可。⚠ **这个字段在 §11.9 之前其实没生效**（只有 `DataValidator` 读它，运行时永远按物品 id 解析），现在是 `SubWeaponRuntime.applyBaselineId` 把它落成 tag 上的 `defaultDataId`。
- **`Animation` 在四期的语义**：从"副武器开火时宿主枪的动画"改成"**副武器激活时**宿主枪的开火动画"
  （键名与默认值 `["fire_sub_weapon"]` 都不变），并新增 `HoldAnimation` / `ViewBone`；
  **换弹动画不在这个 POJO 里** —— 它属于副武器**自己的枪械资源**（`sbw/guns/<id>.json` 的 `Animation.Reload`）；
  `ReloadSound` / `ReloadEndSound` **保留**。完整字段表见 **§9.8.5**、动画归属见 **§9.8.7**。
- `Data` 非空时才是"借用别的枪数据"的特殊情况，**主要用途是多对一**：多个配件 id 共用一份副武器枪数据。**不建议**让它指向"手持形态那把武器"的同名 json —— 两份数据的关注点不同（手持那份有 `DrawTime`/`ZoomTime`/`AvailablePerks`/`Icon`/手持模型的 `ProjectileBone`，副武器要的 `RPM`/`ShootShake`/`ProjectileLife` 未必在内），共享等于把两边的平衡焊死，改一边会静默改另一边。
- **触发冷却不在配件数据里配**（早期草案里的 `Cooldown` 字段已删）：一律按那份枪数据的 `RPM` 自动算（`1200 / RPM`），"这把武器多快"只在枪数据里写一次。
- 副武器的形态完全由那份数据决定：`Projectile` 写实弹 → 开火链路；写 `@ray` → 射线；写 `@melee` + `MeleeActions` → **近战副武器**（走近战链路）。因此 v4 的 `AttackType` 字段彻底不需要。

### 9.3 运行时：寄生 GunData

| 要素 | 做法 | 依据 |
|---|---|---|
| 物品 | **副武器物品自己**（`SubWeaponItem : GunItem, AttachmentProvider`） | 既是配件又是枪 |
| 合成栈 | `ItemStack(subWeaponItem, 1)`，`tag = attachment.getOrCreateTag(slot)`（**同一个 CompoundTag 实例**） | `Attachment.getOrCreateTag` 返回枪 NBT 子 tag 的活引用（`Attachment.kt:72-85`）；`ItemStack(item, count, tag)` 只存引用 |
| 数据基线 | 默认由物品 id 解析；`SubWeapon.Data` 非空时才用 `defaultDataId` | `GunData.getDefault()`：`defaultDataId` 为空 → `item.getDefaultData(data)` |
| 状态（弹药/热量/换弹/耐久/revision） | 全部写在这份共享 tag 上（`GunData` 的 state 在栈 tag 的 `"GunData"` 子 compound 里） | → **随主武器 NBT 持久化**，无新存档字段、无全局 map |
| 弹药 | 副武器自己的 `AmmoSlot`（`gunDataTag.AmmoSlot.<name>`），来源由 Data 的 `AmmoType` 决定（背包物品 / 自己的弹匣） | `subdata/AmmoSlot.kt:17-43`、`AmmoConsumer` 现成 |
| 实例身份 | `SubWeaponRuntime` 持有 `(主武器 UUID, slot) → ItemStack` 强引用缓存 | `DATA_CACHE` 是 **weakKeys（按栈实例身份）**（`GunData.kt:1785-1787`），栈实例不能每次重建 |
| 客户端 resync | 主武器 `reloadTagFrom` 是 `clearTag + merge`，vanilla `CompoundTag.merge` 对嵌套 compound **递归合并** → 附件子 tag 实例仍有效 | `GunData.kt:1583-1606` |
| tick | 合成栈不在背包，**`GunItem.inventoryTick` 不会跑** → 主武器 gun tick 里顺带 tick 它 | `GunItem.kt:184` |
| 开火 | 服务端装配合成栈 → `GunData.shoot(...)`（`GunData.kt:994-1019`，与主武器**同一个入口**）→ 现成的 `GunItem.shootBullet`：弹药从副武器自己的 `AmmoSlot` 扣，后坐/音效/爆炸按它的 Data | 零新发射逻辑 |
| 缓存清理 | 主武器丢失/换枪/卸载配件时移除条目 | — |

**风险与注意（实现时逐条验证）**：
1. 合成栈实例生命周期由我们持有（强引用），否则掉出 `DATA_CACHE`（weakKeys）。
2. 副武器 tick 必须显式接上，否则换弹/热量/栓动计时器不走。
3. `SubWeaponItem` 需要 2D 物品模型（背包图标）；它**不需要**第一人称骨骼渲染器（装上后的外观由 `AttachmentDefinition.Model/Texture` 决定，走 `GeoGunRenderer.renderAttachments`）；**手持时必须按普通物品处理**，门禁清单见 §8.3.1。
4. 客户端与服务端要用**同一套装配规则**（服务端只有主手 stack）。
5. 副武器耐久写在共享 tag 上会正确持久化，但不会触发主武器那种物品损坏事件（一期接受）。

### 9.4 G / V 语义（最终版）

> ⚠ **本节描述的是三期的语义，已被四期取代**：G 不再是「触发一次副武器射击」，而是
> **主武器 ↔ 副武器的切换**。下表保留为历史对照，**当前语义见 §9.8.2**。
> 仍然成立的部分：**V 永远近战**、**没有副武器时 G 落到近战入口**、**多个副武器不做优先级**。
> 已废除的部分：**G 触发一次射击**、**空仓按 G 尝试装填一次**（§9.6）、**遍历逐个触发**。

| 情况 | V 键 | G 键 |
|---|---|---|
| 什么都没装 | 主武器自身近战（枪托砸） | **等同 V**（近战） |
| 只装刺刀（无 `SubWeapon`） | 刺刀动作 | **等同 V**（刺刀动作） |
| 只装副武器（如 GP-25） | 主武器自身近战 | **使用副武器** |
| 刺刀 + 副武器共存 | 刺刀动作 | **使用副武器** |

> **V 永远近战；G 有副武器就使用副武器，没有就等同 V。**

**多个副武器**（已定稿）：不做优先级——**遍历一次，逐个触发**：

1. 收集所有已安装且带 `SubWeapon` 定义的槽位（正常情况最多 1 个）。
2. 逐个走自己的流程：
   - 有弹药 → 触发一次攻击（`SubWeaponFireMessage` 里带上全部实际触发的槽位列表 / 或近战走 `MeleeAttackMessage`）；
   - 空仓 → **尝试装填一次**（§9.6）；
   - 冷却中 → 跳过该副武器（不影响其它）。
3. **动作占用统一持有一次**：本次 G 的 `SUB_WEAPON` 占用时长取所有实际触发者里最长的一个（避免"遍历触发"被动作锁逐个拦掉）。
4. debug 日志打印本次解析与触发结果。

- G 与 V 指向**同一个近战入口**时（没有副武器的情况），共享入口函数与状态（连招下标/动作锁/冷却），同 tick 内两键同时按下只触发一次。
- G 在全部副武器都不可用（冷却/无弹药）时：播一声 `TRIGGER_CLICK` 反馈，不产生其它副作用。

### 9.5 `GunActionLock` —— 动作互斥（本次必须补的一层）

现状是"每类动作各自零散门禁"（`reloading()`/`charging()`/`bolt.actionTimer`），**开火与近战之间是空的**。

| 字段 | 位置 | 说明 |
|---|---|---|
| `activeAction: GunAction` | 客户端按枪状态（与连招计数器同一处） | `NONE` / `FIRING` / `RELOADING` / `BOLTING` / `MELEE` / `SUB_WEAPON` |
| `actionTicks: Int` | 同上 | 剩余占用 tick，每 tick 递减，归零回 `NONE` |

规则：
1. 进入任一动作前检查 `activeAction == NONE`（换弹/拉栓沿用各自状态机，但也要一并检查）。
2. 动作时长：开火 = 一个射击周期；近战 = `action.Duration`；副武器 = `Cooldown` 或副武器数据的 RPM 周期；换弹/拉栓 = 现有计时器。
3. 被拒绝的入口不产生任何副作用。
4. 服务端做一层廉价检查（收到近战/开火/副武器消息时看自己这边的 reload/bolt 状态）——健壮性，不是反作弊。
5. 顺带修掉缺陷 2 与"换弹时挥砍"这类边界；共存场景下尤其关键。

### 9.6 副武器的换弹与弹药（三期版，⚠ 已被 §9.8.4 取代）

> ⚠ **四期已废除自动装填**：副武器激活后玩家**自己按 R** 装填，与其它枪完全一致。
> 本节保留为历史记录；`SubWeaponRuntime` 里的自动装填判定、退避、动作栏进度提示、
> 装填音效跳变全部删除（§9.8.4）。

- 副武器的弹药天然独立（自己的 `AmmoSlot` + 自己的 Data 里的 `AmmoType`）。
- **空仓按 G = 尝试装填一次**（走它自己的 `ReloadTypes`/换弹时间；一期可以只播音效 + 计时，不做专属动画）。
- 若 Data 写 `Magazine: 0`（背包型），则每发直接从背包扣，不进入装填分支。

### 9.7 渲染与动画

| 项 | 做法 |
|---|---|
| 渲染 | 槽位注册表分派；刺刀约定用 `bayonet_pos`，其它配件一律用**配件自己的 `Bone`**（`GeoGunRenderer` 里 barrel 槽那套 `definition.bone` 逻辑，`:744-748`）；多配件可同时渲染 |
| HUD | **不在本方案范围**（后续自行重写） |
| 一期动画 | 副武器开火走**宿主枪的动画候选链**（`SubWeapon.Animation`，默认 `["fire_sub_weapon"]` → 没有就退回宿主枪的 `Fire`），见 §11.9；刺刀用枪的 melee clip（或 `Override.Animation` 指向枪动画文件里的 clip） |
| 二期动画 | **配件自带动画文件**：扩展 `AttachmentModelReloadListener`（现在 `animPath` 为空，`:10`）加载 `animations/bedrock/attachment`；给 `BedrockAttachmentModel` 补 `applyPose`/`resetPose`（底层 `TreeModelInstance` 已支持，`GeoGunModel.kt:88` 就是这么用的）；渲染时枪身跑主 runner、配件跑自己的 runner（附件本来就是独立模型挂骨骼渲染，`GeoGunRenderer.kt:576-643`） |

### 9.8 【四期】**主/副武器切换**：让"当前操控的枪"变成副武器

> 这一节是四期的**规范**；§9.1–§9.7 与 §11.8 / §11.9 里与它冲突的结论（G 触发一次射击、
> 自动装填、`SubWeaponFireMessage`、服务端确认式开火音/动画、副武器不抢 V 之外的任何输入）
> **一律作废**。四期落地清单见 **§11.10**。

#### 9.8.1 核心机制：不换物品，换「当前操控的枪」

**需求**：按下 G 之后，在主武器与副武器之间切换；由于副武器也是一个具有 `GunData` 的枪械，
应该让**当前操控的 gun 变成副武器**，这样能对副武器进行原本的开火、换弹、瞄准等操作，
而不需要单独判断 `subweapon`。

**⛔ 不能这么做（可行性不足，本方案明确放弃）**：把副武器的合成栈塞进玩家的主手
（`player.setItemInHand(MAIN_HAND, subStack)` / 服务端 `inventory.setSelected`），
或者真的往快捷栏里塞一件副武器物品。三个硬障碍：

1. **主手是双端权威的物品槽**。服务端那份由 `ServerboundSetCarriedItemPacket` / `Inventory`
   驱动，客户端的本地改写会在下一次同步被原样冲掉——而"客户端以为切了、服务端以为没切"
   在这一套"判定在客户端、结算在服务端"的信任模型里是最坏的一类 bug（三期已经为同类问题
   付过一次代价：§11.8.3 的三个症状全部源于两个 `GunData` 抢着写同一份 tag）。
2. **副武器栈是"合成栈"**，物品数永远是 1，tag 是宿主枪 NBT 里附件子 tag的**活引用**
   （§9.3）。把它当成真的主手物品，等于让一件凭空造出来的栈去占据玩家的物品槽：
   一旦玩家滚轮 / 丢物品 / 死亡掉落，就会掉出一件不该存在的物品。
3. **快捷栏是"玩家的 9 个槽"**，副武器是"**某把枪身上的一个配件**"，两者基数不同：
   玩家换一把枪，主手该回到这把新枪而不是"上一把枪的副武器"。

**✅ 本方案**：显式引入一个「**当前操控的枪**」（active gun）状态，并把它做成
**双端一致、随枪持久化、由服务端权威**的一份数据：

| 项 | 做法 |
|---|---|
| 状态载体 | `GunState` 新增两个字段：`ActiveSlot: String = ""`（空 = 主武器，否则是 `AttachmentType` 枚举名如 `"SUBWEAPON"`）与 `ActiveOwner: StructuredUUID? = null`（该副武器所属**宿主枪**的 UUID） |
| 为什么写进枪械状态 | 它随宿主枪 NBT 一起持久化、一起同步到客户端（`GunData` 的现成同步链路），**不新增任何存档字段、不新增同步通道**；玩家换枪 / 丢枪，状态天然跟着那把枪走 |
| 为什么还要记 `ActiveOwner` | 副武器状态本身就住在宿主枪的附件子 tag 里（§9.3），`ActiveOwner` 只是把这条隐含约束**显式化**：主武器 UUID 与它不符时，这次部署自动作废（收起），避免"附件被拆了 / 枪被复制了 / 状态被搬到了另一把同型号的枪上"这类边缘情况把玩家永久锁在副武器上 |
| 默认值 | `ActiveSlot = ""`，即**开箱即用永远是主武器**；旧存档零迁移 |
| 权威侧 | **服务端**：客户端按 G 只发一个"请切换"的报文（§9.8.10），服务端校验后落 NBT、回一条确认报文；客户端**收到确认才演切换动作**（与三期 §11.9 给开火音/开火动画定的"服务端拍板"同一个口径，那一条结论继续有效，只是拍板的对象从"这一发打没打出去"变成"这次切没切成"） |
| 服务端查询 | `ActiveGun.stackOf(player)`：主手不是枪 → `ItemStack.EMPTY`；`ActiveSlot` 非空且 `ActiveOwner` 匹配 → 对应的副武器合成栈；否则 → 主手物品 |
| 客户端查询 | 同一份逻辑，但读的是同步过来的 `GunData`（**客户端不再存第二份状态**：一期 §5.2 那条"客户端计数不能放 `GunState`"的教训在这里反过来——部署状态**必须**双端一致，所以它就该放 `GunState` 并由服务端写） |

**唯一真正的成本：读取入口要收敛。** 全仓 `mainHandItem` 的调用点：**Kotlin 侧 119 处（分布在 77 个文件）、
Java 侧 70 处**，其中绝大多数是"我正在操作的那把枪"。四期的机械工作量就是把这些点分成两类
（下表括号里是实测的"主战场"规模：`event/` + `client/` 两个目录就占了 **61 处**）：

| 类别 | 处理 | 例子 |
|---|---|---|
| **A. 「我正在操作的那把枪」** | 换成 `ActiveGun.stackOf(player)` / `ActiveGun.dataOf(player)` | `ClientEventHandler.handleGunShoot`（`:1675`）、`handleWeaponZoom`（`:2564`）、`handleWeaponDraw`（`:2604`）、`handleGunMelee` 入口（`:629` 附近）、`handleGunRecoil`（`:2867`）、`shootClient`/`handleShootAnimationV2` 一族（`:2721`）、`ClickEventHandler.handleWeaponFirePress`（`:475`）/`handleWeaponZoomPress`（`:640`）、`ClientMouseHandler`（`:86`/`:271`）、所有 `network/message/send/*` 的 handler（`FireKeyMessage:25`、`ReloadMessage:22`、`ShootMessage:24`、`WeaponZoomingMessage:14`、`SwitchScopeMessage:20`、`AdjustZoomFovMessage:20`、`UnloadMessage:16`、`SensitivityMessage:19`、`MouseMoveMessage:22`、`FireModeMessage:20` 等）、HUD overlay（`CrossHairOverlay:80`、`AmmoBarOverlay:85`、`AmmoCountOverlay:45`、`HeatBarOverlay:33`、`HandsomeFrameOverlay:29`、`ItemRendererFixOverlay:16`）、`GunEventHandler.gunTickInternal` |
| **B. 「物理上拿着的东西」** | **保持不动**（这是设计里刻意留的少数"看主手"的地方） | `ItemInHandRenderer` / `ItemInHandLayer` 一族 Mixin、`GunItem.inventoryTick`（它 tick 的就是主手那件物品）、`getAttributeModifiers`（属性挂在**手持的那件物品**上）、`PlayerEventHandler` 的交互/放置、`WeaponEditScreen` / 改装命令、`SubWeaponRuntime.tick` 里"主手是不是宿主枪"的那部分判定（§9.8.3）、`ClientEventHandler.handleGunShoot` 里"主手压根不是枪"的早退 |

> **规模参考**：Kotlin 侧 119 处里，`event/` + `client/` 占 61 处（这是 A 组的主战场）；
> `src/main/java/` 侧另有 70 处，绝大多数落在具体枪械物品类与旧 GeckoLib 渲染路径上
> （`item/gun/**`、`client/model/item/**`），**那些基本属于 B 组或已冻结的旧路径**，
> 逐个确认即可，预计真正要改的不到一半。**先按"两目录 61 处"做工作量估算。**

> **一个可以偷懒但危险的替代方案**（评估后不推荐）：让 `GunData.from(player.mainHandItem)`
> 内部偷偷返回副武器的数据。**不行**——`GunData.from` 是"栈 → 它自己的数据"的纯函数，
> 被 `DATA_CACHE`（按栈身份）、`item.getDefaultData()`、附件/弹药子 tag 全部依赖；
> 让它返回另一把枪的数据会让"这把栈的数据"和"这把栈的状态"彻底脱钩（三期 §11.8.3 的坑会全部复现）。

> **⚠ 必须一起处理的一个坑：`GunItem.isHeldWeapon()` 会把副武器挡在门外。**
> 现在那条谓词是 `(stack.item as? GunItem)?.useAsWeaponInHand() == true`（`GunItem.kt:1292`），
> 而 `SubWeaponItem.useAsWeaponInHand()` **是 `false`**（三期 §8.3.1 定的：手持副武器物品时按普通物品处理）。
> 四期把 `ActiveGun.stackOf(player)` 返回成副武器栈之后，凡是**门禁**写法
> （`if (!GunItem.isHeldWeapon(stack)) return`，例如 `handleGunShoot:1678`、`handleWeaponZoomPress:660`、
> `handleWeaponBipodView:2606`、`MeleeClientHandler.tick:90`）都会把副武器判成"不是枪"，**整个机制直接失效**。
>
> **处理**：把这批"门禁"从 `isHeldWeapon` 换成**新的、语义正确的谓词**
> （`GunItem.isOperable(stack)`：是 `GunItem` 即可，不看"在不在手上"），
> 而 `isHeldWeapon` **保持原样**——它仍然要负责它原本那件事：**手持副武器物品本身时按普通物品处理**
> （§8.3.1 的 A–E 组门禁一行不动，因为那种情况确实不该当枪）。
> 两者的分工写进 `GunItem` 的 KDoc：
>
> | 谓词 | 回答的问题 | 用在哪 |
> |---|---|---|
> | `isHeldWeapon(stack)` | "这件**物品**被玩家拿在手里时算不算枪" | 渲染 / 视角 / HUD / 属性 / `inventoryTick`（§8.3.1 的清单） |
> | `isOperable(stack)`（新增） | "这件**栈**现在能不能被当成一把枪来操作" | 所有**已通过 `ActiveGun` 解析出栈之后**的门禁（开火/换弹/瞄准/近战/动画状态机） |
>
> 注意 `GunItem.inventoryTick`（`:184-188`）**必须留在 `isHeldWeapon` 那一侧**：
> 它跑的是"拿在手上的枪"的状态机，而副武器栈的状态由 `SubWeaponRuntime.tick` 显式推进（§9.3）——
> 换成 `isOperable` 会让副武器被 tick 两遍。**这两个谓词混用的地方就是四期最容易出的 bug。**

#### 9.8.2 G / V 语义（四期最终版）

| 情况 | V 键 | G 键 |
|---|---|---|
| 什么都没装 | 主武器自身近战（枪托砸） | **等同 V**（近战）——没有副武器可切 |
| 只装刺刀（无 `SubWeapon`） | 刺刀动作 | **等同 V**（刺刀动作） |
| 只装副武器，当前是主武器 | 主武器自身近战 | **切到副武器** |
| 只装副武器，当前是副武器 | **主武器**自身近战（副武器没有近战） | **切回主武器** |
| 刺刀 + 副武器共存，当前是主武器 | 刺刀动作 | **切到副武器** |
| 刺刀 + 副武器共存，当前是副武器 | **刺刀动作**（主武器的动作表，装了刺刀就是刺刀） | **切回主武器** |
| 装了多个副武器 | 同上 | 切到**枚举顺序里的第一个**可用槽位（不做优先级/轮换，`AttachmentType.entries` 顺序即结果；正常情况最多一个） |

> **V 永远近战，且近战恒用「主武器」的动作表**——副武器没有近战输入，也不参与近战判定。
> 副武器激活时按 V，主机枪做近战动作、主武器结算伤害，副武器保持挂在枪上不动。
> 这条是硬性的：近战动画与判定都是主场枪的骨骼几何体（§4），副武器模型只是挂在
> `sub_weapon_pos` 上的一个挂件，它没有也拿不到自己的判定体。

**空仓 / 装填中按 G 一律有效**：G 只负责"换一把枪操控"，能不能开火是那把枪自己的事
（打不出去就是不响，与主武器一致）。三期那条"全部副武器都不可用时播 `trigger_click`
并吞掉按键"的逻辑**删除**——现在按 G 一定是在切换。

**切换冷却**：不需要。切换动作本身占用动作锁（§9.8.8），锁没走完再按 G 会被拒。
想连点两下快速来回切也做不到，这是有意的（避免"抽搐式"切换配合动画抖动）。

#### 9.8.3 副武器激活后的能力：**零专属代码**

这是四期最大的收益——需求里说的"不需要单独判断 subweapon"就落在这里。
`GunItem.shoot(data, shooter, …)`、`GunItem.tryStartReload(shooter, data)`、
`GunData.zoom()`、`GunData.canShoot()`、`GunData.shouldStartReloading()` 全部是**纯 `GunData` 驱动**的
（三期 §11.8.1-⑬ 已经把这条结论验证过一遍），所以：

| 能力 | 主武器（现状） | 副武器激活后 | 有没有新代码 |
|---|---|---|---|
| 开火 | 客户端 `handleGunShoot` → `FireKeyMessage` → 服务端 `onFireKeyPress` → `GunData.shoot` | **同一条链路**，只是 `GunData` 换成副武器的 | 无 |
| 扣扳机方式 | 读 `data.selectedFireModeInfo()`（主武器数据里的 `DefaultFireMode`/`AvailableFireModes`） | 读**副武器自己那份数据**的模式 → GP-25 是 `Semi`，写成 `Auto` 就是连发 | 无（三期 `SubWeaponClientHandler` 里那套 `Semi`/`Auto`/`Burst` 手写状态机**整块删除**，改由 `handleGunShoot` 现有的模式分支接管） |
| 换弹 | 按 R → `ReloadMessage` → `tryStartReload` | 同一条链路，装的是副武器的 `AmmoType`、走它自己的 `EmptyReloadTime` | 无（三期的自动装填 + 退避 + 动作栏进度 + 开始/结束音效跳变全部删除） |
| 瞄准 | 右键 → `ZoomMessage` → `ClientEventHandler.zoom` → `zoomTime/zoomPos` | 同样的 `zoomTime`，只是 `ZOOM_TIME`/`Weight`/`CanZoom` 读副武器的数据 | 无（三期"副武器不抢右键"的隐含约定作废） |
| 开火音 | 客户端 `playGunClientSounds` → `GunItem.resolveFire1PSounds(data)` | 同一个函数、同一处调用，参数是副武器的 | 无 |
| 弹壳 | `ShellEject` 抛壳 | 副武器**不抛壳**（§11.9-A 的结论保留：弹壳模型与 `shell` 骨骼都是宿主枪的） | 一行判断 |
| 枪口焰/烟 | 主机枪的 `flare` / 枪口配件 | **副武器模型自己的 `flare`**，整段部署期都归它（§9.8.6） | 条件从"开火窗口"改成"部署中" |
| 后坐 / 抖动 / 散布 | 读主武器数据 | 读副武器数据 | 无 |
| Perk / 弹种 / 耐久 / 热量 | 主武器自己的 | 副武器自己那份 `GunData` 的 | 无 |

**宿主枪在副武器激活期间不再被 tick**（`GunItem.inventoryTick` → `gunTick` 只作用于主手物品）：

- 宿主枪的换弹 / 拉栓在**切走的那一刻打断**（与主武器切枪同一套清理，见 §9.8.4）；
- 副武器的 tick 由 `SubWeaponRuntime.tick` 继续负责（它挂在宿主枪的 `gunTick` 之后，本来就不受
  `inMainHand` 约束），但 `inMainHand` 入参改成 **`主手拿着宿主枪 && 部署的就是这个槽位`**；
- 未部署的副武器在背包里躺着时**状态照常推进但不自动装填**（§9.8.4），与"别人的枪在包里"一致。

#### 9.8.4 换弹、弹药与"切走即中断"

| 项 | 四期结论 |
|---|---|
| 触发 | **玩家按 R**（`ReloadMessage` → `tryStartReload(shooter, ActiveGun.dataOf(player))`）。没有任何自动装填 |
| 弹药 | 副武器自己的 `AmmoSlot` + 自己的 `AmmoType`（GP-25 = `superbwarfare:grenade_40mm`），备弹从背包扣；**与主武器完全隔离**（状态住在宿主枪 NBT 的附件子 tag 里，§9.3） |
| 卸载 | **删除**：`SubWeaponRuntime` 的 `shouldStartReloading` 自动装填、`autoReloadBackoff`、`Instance.wasReloading` 跳变、`onReloadStarted`/`onReloadFinished`、`showReloadingProgress`、`AUTO_RELOAD_BACKOFF`/`RELOAD_HINT_INTERVAL`/`RELOAD_SOUND_VOLUME` 常量 |
| 信息反馈 | 靠**现有 HUD**：副武器激活时它就是"当前操控的枪"，弹药条/弹药数/热量条读的就是它（§9.8.8）。三期那三条动作栏临时提示（`info.superbwarfare.subweapon.reloading/reloaded/reload_empty`）删除 |
| 切换时中断 | 切走（主→副、副→主）时，被切走的那把枪：`reload.setTime(0)` + `NOT_RELOADING` + 单发装填各阶段计时器 + `bolt.actionTimer.reset()`；**不算"装填完成"**，不播完成音。落点复用 `SubWeaponRuntime.interruptReload`（已有）+ `LivingEventHandler` 里切枪那一段的同一套动作 |
| 换弹动画 | **副武器自己资源里的 `Animation.Reload`**，由它自己的附件模型播（§9.8.7）。宿主枪在此期间照常播 `idle`，两套骨骼不冲突 |
| 换弹音效 | **保留配件的 `SubWeaponInfo.ReloadSound` / `ReloadEndSound`**（三期实现原样复用）：副武器这支动画走的是新增的附件播放链路，**不接 `sound_effects` 关键帧**，所以音效仍由配件数据 + 状态跳变负责（§9.8.7）。⚠ 与 §12.6-62 的旧结论相反，以本节为准 |
| `Magazine: 0`（背包型） | 照旧每发直接从背包扣，不进装填分支（`GunData.useBackpackAmmo()` 现成） |
| 装填期间切枪 | 允许（动作锁只挡开火/近战，不挡 G）。切回来是**从头装**，与主武器切枪语义一致（三期 §11.9-E 的结论保留） |

#### 9.8.5 数据：`SubWeaponInfo` 的字段增删

```jsonc
// sbw/attachments/sub_weapon_gp_25.json —— 配件定义（挂点 / 模型 / 副武器参数）
{
  "Slot": "SubWeapon",
  "Bone": "sub_weapon_pos",
  "Modifiers": [ { "Prop": "Weight", "Op": "Add", "Value": 1.5 } ],
  "SubWeapon": {
    "Data": null,                            // 可选：默认 = 附件自身注册 id（sbw/guns/<id>.json）
    "AmmoSlot": "SubWeapon",                 // 副武器自己的弹药槽

    "Animation": ["fire_sub_weapon"],        // 【语义修订】副武器激活时**宿主枪**的开火动画候选链
    "HoldAnimation": ["hold_sub_weapon"],    // 【新，可选】副武器激活时宿主枪的持枪态（循环）
    "ViewBone": "iron_view",                 // 【新，可选】瞄具位形；不写 = 附件模型的 iron_view
    "ReloadSound": "...", "ReloadEndSound": "superbwarfare:gp_25_reload_2"   // 保留（§9.8.7 的音效口径）
  },
  "Model": "...", "Texture": "..."
}
```

```jsonc
// sbw/guns/sub_weapon_gp_25.json —— **既是枪数据、也是枪械资源**（两者都从 sbw/guns 加载）
{
  "Spread": 1, "Damage": 80, "Magazine": 1, "RPM": 60,
  "AmmoType": "superbwarfare:grenade_40mm", "Projectile": "superbwarfare:gun_grenade",
  // …枪数据部分与三期完全一致…

  "Animation": {
    "Reload": "animation.sub_weapon_gp_25.reload",   // 【新】副武器**自己**的换弹动画
    "ReloadEmpty": "animation.sub_weapon_gp_25.reload"
  },
  "Model": {
    "Animation": "superbwarfare:animations/bedrock/attachment/sub_weapon_gp_25.animation.json",
    "Model": "superbwarfare:models/bedrock/attachment/sub_weapon_gp_25.geo.json",
    "Texture": "superbwarfare:textures/bedrock/attachment/sub_weapon_gp_25.png"
  }
}
```

> **为什么副武器能"有自己的动画"**：这是四期最省事的一点——**副武器物品本身就是 `GunItem`**（三期 §8.3），
> 所以 `GunResource.compute(副武器合成栈)` 会按**物品注册 id** 解析出 `sbw/guns/sub_weapon_gp_25.json`，
> 与手持形态的枪走的是**同一套资源机制**（`CustomData.GUN_RESOURCE` 与 `GUN_DATA` 都从 `sbw/guns/<id>.json` 读，
> `CustomData.kt:40`/`:94`）。于是"副武器换弹播哪支 clip"**不需要任何新字段**，
> 就是它资源里的 `Animation.Reload`。**注意 `sbw/guns/sub_weapon_gp_25.json` 现在还不存在**
> （`sbw/guns/` 里只有 `gp_25.json`，那是手持形态的），四期要新建。

| 字段 | 变更 | 说明 |
|---|---|---|
| `Data` / `AmmoSlot` | 不变 | §9.2 的语义完全保留 |
| `Animation` | **语义修订，键名不变** | 三期 =「副武器开火时宿主枪的动画候选链」；四期 =「**副武器激活时**宿主枪的开火动画候选链」。默认值仍是 `["fire_sub_weapon"]`（`SubWeaponInfo.DEFAULT_FIRE_ANIMATION`），**GP-25 的配件 json 一个字都不用改** |
| `HoldAnimation` | **新增，可选** | 副武器激活时宿主枪的持枪态（循环）。`null` = 不覆盖宿主 idle（**默认行为与三期完全一致**）。写它是为了让"整装武器被端起来"这件事有动画可做，而不是只有开火那一下 |
| `ViewBone` | **新增，可选** | 显式指定瞄具位形骨骼名；不写 → 在**附件模型**里找 `iron_view`；再没有 → 宿主枪的 `scope_view` / `iron_view`（§9.8.6） |
| ~~`ReloadAnimation`~~ | **不做**（评估后取消） | 原方案想在配件里声明一个"宿主枪上的左手换弹候选链"。既然副武器有自己的资源与动画文件（见上），**换弹动画的归属就是它自己的 `Animation.Reload`**，再在配件里放一个同义字段只会让人不知道该改哪个。**§12.6-64 的旧结论已作废** |
| `ReloadSound` / `ReloadEndSound` | **保留** | 三期为"配件没有动画"加的补丁，四期**仍然需要**：见 §9.8.7 的音效口径（副武器动画里的 `sound_effects` 关键帧目前不会响） |
| 触发冷却 | **删除相关代码** | 三期用宿主枪冷却表的 `sub:<slot>` 键来限流"按 G 触发"；四期 G 是切换、开火走副武器自己的 `RPM`，所以 `Cooldown.subWeaponKey` 与 `Instance.cooldownKey`/`cooldownTicks()` 一并删除 |

**候选链的解析规则完全复用二期 §11.5.3-① 的 `GunAnimationNames.resolveFirst`**：
短名按 `animation.<宿主枪 id>.<短名>` 拼接、按顺序取第一个存在的 clip；
显式写的候选全部落空 = error 日志，默认候选落空 = debug 日志（§11.9-A 的结论保留）。

#### 9.8.6 瞄准：副武器自己的 `iron_view` 优先

**先纠正一处术语**：仓库里**没有** `zoom_view` 这个名字。第一人称"枪摆到哪儿"的定位点骨骼是：

| 骨骼 | 含义 | 现状 |
|---|---|---|
| `idle_view` | 非瞄准时的持枪位形 | `GeoGunRenderer.IDLE_VIEW_BONE`：**必需**，拿不到就直接不渲染定位（`computeViewTransform` 返回 `null`） |
| `iron_view` | 机瞄位形 | `GeoGunRenderer.IRON_VIEW_BONE` |
| `scope_view` | 瞄具的分划位形（每个瞄具模式可以有自己的 `scope_view_<n>`，`ScopeInfo.viewBone()`） | `GeoGunRenderer.SCOPE_VIEW_BONE` |
| `bipod_view` | 卧姿脚架位形 | `GeoGunRenderer.BIPOD_VIEW_BONE` |
| `camera` | **只用来做屏幕抖动收敛**（`applyCameraShake` 的旋转补偿），**不是**瞄准位形 | `GeoGunModel.CAMERA_BONE` |

所以需求里的"副武器有没有 zoom_view 骨骼"落地为：

```kotlin
// GeoGunRenderer.computeViewTransform，zoom > 0 时
val aimTransform = subWeaponAimTransform(...)      // ① 副武器附件模型自己的
    ?: scopeViewTransform(scopeRender, hand)       // ② 宿主枪的瞄具分划（装了瞄具才有）
    ?: model.getGlobalTransform(IRON_VIEW_BONE)    // ③ 宿主枪的机瞄
    ?: return hipViewTransform
```

① 的解析顺序：`SubWeaponInfo.ViewBone`（显式）→ 附件模型里的 `iron_view` → `null`。
**候选顺序刻意是"副武器的机械瞄具 → 宿主枪的瞄具 → 宿主枪的机瞄"**：装了红点的枪切到副武器时，
玩家眼睛贴在副武器上、但红点分划还在枪身上——这时取宿主枪的 `scope_view` 反而是对的
（副武器是下挂件，它自己的瞄具就在枪身中段）。真正要避免的是**取到宿主枪的 `iron_view` 却
把副武器模型留在原来的位置**，那会看到"枪抬起来了，榴弹筒还在下面"。

> **⚠ 现状数据付不出 ①**：`sub_weapon_gp_25.geo.json` 的骨骼只有
> `root` / `gun` / `tube` / `ammo` / `trigger` / `bone2..7` / `flare`，**没有 `iron_view`**。
> 所以四期刚落地时 GP-25 会走 ②/③（宿主机瞄位形），视觉上是"整枪抬到机瞄位、榴弹筒跟着上去"——
> 可接受，也不难看。**要给 GP-25 做自己的瞄具位形，只需在附件模型里加一支名为 `iron_view` 的骨骼**，
> 数据（`SubWeaponInfo.ViewBone`）与代码都不用改。这把骨骼的存在性校验放在资源侧（§10 已有的"资源校验"一栏）。

**`zoomTime` 的驱动不动**：`ClientEventHandler.handleWeaponZoom` 只管 `zoomTime/zoomPos` 的进退，
把读 `stack` 的地方换成 `ActiveGun.stackOf(player)` 即可（`ZOOM_TIME`/`Weight`/`CanZoom` 自动变成副武器的）。
FOV 由 `GameRendererMixin` + `data.zoom()` 决定，同样自动跟着走。

#### 9.8.7 动画：副武器有**自己的**资源与动画文件

**需求（四期修订版）**：副武器只需要一个**副武器自带的换弹动画**。由于副武器是**独立的 `GunData`**，
它自然可以使用**独立的 `GunResource`**。以目前的 `sub_weapon_gp_25` 为例，换弹时就用**同名资源 json**
里定义的 `Animation.Reload: animation.sub_weapon_gp_25.reload`（动画以后补），
播放期间**与主武器的 `idle` 做姿态融合**（宿主枪照常 idle，两者不冲突）。
**副武器没有 idle 动画，持枪态以主武器的为准。**

**为什么这条路可行**（三条事实核对过）：

1. **副武器本来就有自己的枪械资源。** `GunResource.compute(stack)` 按**物品注册 id**取资源
   （`GunResource.kt:76-85` 的 `RESOURCE_CACHE` + `idOf(stack)`），而 `CustomData.GUN_RESOURCE`
   与 `GUN_DATA` **都从 `sbw/guns/<id>.json` 加载**（`CustomData.kt:40`/`:94`）。
   副武器的合成栈用的是 `SubWeaponItem`（物品 id = `sub_weapon_gp_25`），
   所以 `GunResource.compute(subStack)` 解出来的就是 `sbw/guns/sub_weapon_gp_25.json` ——
   **同一份 json 既是枪数据、又是枪械资源**，与所有普通枪完全一致。
2. **副武器模型有自己的实例。** `GeoGunRenderer.renderRegisteredAttachments` 通过
   `AttachmentModelReloadListener.getModel(modelPath)` 拿到 `BedrockAttachmentModel`，
   它内部持有 `TreeModelInstance`（`BedrockAttachmentModel.kt:28-29`）——
   与 `GeoGunModel` 是**同一个 `TreeModelInstance` 体系**，而 `GeoGunModel.applyPose/resetPose`
   就是 `instance.applyPose/resetPose`（`GeoGunModel.kt:88-95`）。所以给附件模型加一组
   `applyPose`/`resetPose` 是**照抄**，不是新机制（二期 §9.7 的"配件自带动画文件"路线）。
3. **动画与模型的绑定按文件名 id 配对，已经能用。** `BedrockModelReloadListener` 的构造参数
   本来就有 `animPath: String = ""`（`:16-18`），加载时按 `FileToIdConverter.json(animPath)` 读文件名 id，
   再用 `animPathToIds` / `idToModelPaths` **按 id 配对**（`:52-63`）。
   `GunModelReloadListener` 传的是 `"animations/bedrock/gun"`（`GunModelReloadListener.kt:11-14`），
   而 `AttachmentModelReloadListener` **目前只传了 modelPath、没传 animPath**
   （`AttachmentModelReloadListener.kt:10-12`）。四期只要给它补一个参：

   ```kotlin
   object AttachmentModelReloadListener : BedrockModelReloadListener<BedrockAttachmentModel>(
       "models/bedrock/attachment",
       "animations/bedrock/attachment"        // ← 四期新增这一行
   ) { … }
   ```

   于是 `animations/bedrock/attachment/sub_weapon_gp_25.animation.json`
   自动绑到 `models/bedrock/attachment/sub_weapon_gp_25.geo.json`（**文件名即配对键**，
   `SubWeaponInfo` 里不需要任何动画字段）。

**"姿态融合"到底融什么 —— 是"两个模型各播各的"，不是"两套骨骼合并"：**

| 项 | 结论 |
|---|---|
| 宿主枪 | 照常播自己的 `idle` / `run`（`GeoGunAnimationInstance.cachedPose` → `model.applyPose(...)`，`GeoGunRenderer.kt:296-299`）。**副武器没有 idle，持枪态完全以主武器为准**（按需求） |
| 副武器 | 换弹时它的附件模型跑自己的 runner，`instance.applyPose(副武器换弹的 pose)` |
| 为什么天然不冲突 | 两个模型是**父子但各自独立的姿势树**：宿主枪的 pose 只按宿主模型的骨骼名解析，副武器的 pose 只按附件模型的骨骼名解析（`root`/`gun`/`tube`/`ammo`/`trigger`/`flare`…），命名空间不重叠。副武器的 `root` 是它自己模型的根，**不是**宿主枪的 `root`，所以它动不会带动整枪 |
| 所以**不需要** `NoAllocMergeBlender` | 原方案（在宿主枪的动画文件里做一支只含 `lefthand` 的 clip，再用 `MERGE_BLENDER` 覆盖宿主 idle）**作废**：既然副武器有自己的模型与动画，两套骨骼根本不在一个命名空间里争资源，合并反而是多余的复杂度。**§12.6-65 的旧结论作废** |
| 但也不会带动"手" | 这只手是**宿主枪模型的 `lefthand` 骨骼**，副武器的动画碰不到它。所以视觉效果是"枪照常端着，下挂筒自己开膛、装弹、闭膛"——干净且符合"副武器只由左手操控"的直觉（另一只手忙它的）。**想让左手真的去够榴弹**，那是另一件事：要么在**宿主枪**的动画里加一支左手 clip（就是被作废的那条路），要么以后给副武器自己的模型加一只手。本期不做 |

**渲染/驱动要动的三处**：

| # | 落点 | 动作 |
|---|---|---|
| ① | `resource/model/AttachmentModelReloadListener.kt` | 补 `animPath = "animations/bedrock/attachment"` |
| ② | `client/model/attachment/BedrockAttachmentModel.kt` | +`applyPose(pose)` / `resetPose()` / `getIndex(name)` / `getBone(index)`（照 `GeoGunModel.kt:80-95` 抄；`instance` 已在手边） |
| ③ | `client/animation/gun/GeoGunAnimationInstance.kt` | 部署中 + 副武器 `reloading()` 时，从 `GunResource.compute(副武器合成栈).animation` 取 reload clip 名、建一个**副武器的 runner**，每 tick 推进，`getSubWeaponPose()` 供渲染侧取用。runner 的**状态放在宿主枪的动画实例里**（附件模型实例是全局共享的，不能往它身上挂状态），键用 `sub_weapon_pos` 那个槽位 |

`GeoGunRenderer.renderRegisteredAttachments` 里对 `SUBWEAPON` 槽位多一步：渲染前 `applyPose(副武器 pose)`、渲染后 `resetPose()`。

| 要点 | 说明 |
|---|---|
| clip 名从哪来 | **副武器自己的资源**：`GunResource.compute(subStack).animation.reload`（走 `reloadNormal`/`reloadEmpty` 的既有分支，与普通枪同一套逻辑）。**没有 `SubWeaponInfo` 字段**，也就没有"该改 json 还是改配件"的歧义 |
| 时长对齐 | `playbackSpeed = clip.specifiedEndTimeMs / (reloadTotalTicks / 20f)`，与 §5.1 同一套；`reloadTotalTicks` 是**副武器数据**的 `EmptyReloadTime`/`NormalReloadTime`（GP-25 = 80 tick） |
| 已实装的那支动画 | `animation.gp_25.reload`（1.2s）**可以当参考**，但它是**手持形态**的：驱动 `root`/`righthand`/`lefthand`/`camera`/`head`——那些骨骼名在**附件模型里不存在**，直接拿来用会有一半关键帧落空。所以 `sub_weapon_gp_25.animation.json` 要**按附件模型的骨骼做一份**（`root`/`gun`/`tube`/`ammo`/`trigger`） |
| 换弹音效 | **保留配件的 `ReloadSound` / `ReloadEndSound`**（三期已有实现）。理由：数据包动画的 `sound_effects` 关键帧目前**只在"枪"的播放链路上会响**（`GunModelReloadListener` 造出来的 `BedrockAnimation` 由 `GeoGunAnimationInstance` 消费，那里才有播关键帧音效的逻辑），副武器这支动画走的是新增的附件播放链路，**不接音效关键帧**。GP-25 的 `animation.gp_25.reload` 里正好有 4 条 `sound_effects`（`common_grab_1` / `gp_25_reload_1` / `gp_25_reload_2` / `common_grab_2`）—— 做新动画时**不要指望它们会响**，要么继续用配件的两个字段，要么四期后续把音效关键帧接进附件链路 |
| 找不到 clip | 回退：**宿主枪自己的换弹动画**（等价于三期的观感），并按"附件资源里没做这支 clip"打 **debug** 日志（不是 error —— 没做动画是正常状态，与 §11.9-A 对 `fire_sub_weapon` 的分档一致） |
| `AttachmentModelReloadListener` **未传 animPath 时** | 现在传了之后，`animations` 表会多出附件动画；**资源包可以只放动画不放模型**（反之亦然），配对是靠"文件名 id 相同"，缺一边就只是那一半为空，不会报错 |
| 开火 | 宿主枪播 `SubWeaponInfo.Animation` 候选链（默认 `fire_sub_weapon`）——**这条不变**。AK-12 那支 `animation.ak_12.fire_sub_weapon` **长 1.2s、只驱动宿主的 `root`**，是"整枪为下挂筒让位"的动画，并且带一条 `muzzle_smoke` 粒子关键帧（`locator: "flare"`），所以它是**开火专用**、不要拿它当 `HoldAnimation`（1.2s 的一次性动作不能循环当持枪态） |
| 持枪态 | **副武器不做 idle**（按需求）；宿主枪继续播自己的 `idle`。`SubWeaponInfo.HoldAnimation`（可选）只是给"整装被端起来"留的口子，不写就是宿主 idle 原样 |

#### 9.8.8 HUD、手持表现与动作锁

| 项 | 四期结论 |
|---|---|
| 弹药条 / 弹药数 / 热量条 / 准心 | **不改代码**：它们读 `player.mainHandItem` 的那几处换成 `ActiveGun.stackOf(player)`（§9.8.1 的 A 组），于是自动显示副武器的弹药、热量、准心。**需求方要重写的 HUD 因此天然支持副武器**，不必再为副武器单开一套 |
| 准心 | 副武器数据的 `Crosshair` 为空时走默认（`@Empty`），与"没有配件的手枪"一致；将来给副武器做专属准心就是往它自己的枪数据里写 `Crosshair` |
| 第一人称模型 | 不变：宿主枪模型照旧渲染（含挂在 `sub_weapon_pos` 上的副武器模型）。宿主枪的姿势来自自己的 idle/run，副武器换弹时**它的附件模型**跑自己的 runner（§9.8.7） |
| 左手 | `ItemInHandLayerMixin` 现在会在手持枪时**隐藏左手物品**（第三人称）。副武器模型不是左手物品（它是宿主枪模型的挂件），所以这条**不需要为新机制改动**；`ItemInHandRendererMixin`（把主手装备动画进度强制为 0）同理 |
| 动作锁 | `GunActionLock` 的 `SUB_WEAPON` 保留，语义从"副武器开火占用"改成"**切换中**"：占用时长 = `max(旧枪 DrawTime, 新枪 DrawTime) + 一个握手余量`。切换期间**开火/换弹/近战全部被拒**，但**再按一次 G 也被拒**（避免抽搐式切换） |
| 切换表现 | 复用现成的 `ClientEventHandler.drawTime` + `resetGunStatus()`（切枪时把 `zoom`/`zoomTime`/`burstFireAmount`/`chargeActive` 等全部归零），时间常数取两把枪 `DrawTime` 的较大者（GP-25 数据里 `DrawTime: 1`，几乎瞬时）。将来要做"下挂筒翻起来"的专属动画，再往 `GunAnimation` 加一支 `Deploy` 即可，本方案不预留 |
| 换弹中断 | §9.8.4 的"切走即中断"由切换流程调用，落点复用现成代码 |

#### 9.8.9 双版本（1.20.1 Forge / 1.21.1 NeoForge）物品数据存储适配

**背景**：本仓库当前工作分支是 **1.20.1（Forge 47.2.0）**，另有一条 **1.21.1（NeoForge 21.1）**
分支；后者的 `ItemStack` **移除了物品 NBT**，改用 **DataComponent**（参见 `localmod/README.md` 第五节
的差异表：`物品属性：Item.Properties ↔ DataComponent`）。副武器体系目前是"**用 tag 重建**"的
（`SubWeaponRuntime` 直接 `ItemStack(item, 1, liveTag)`、`stack.tag`、`stack.tag = liveTag`），
两分支必然分叉。

**目标**（按需求）：**逻辑用通用方法，不同版本的实现细节分开做。**

**⛔ 先说不成立的方案**：把 `CompoundTag` 整个换成一个跨版本的中立数据模型。
`GunData` / `GunState` / `Attachment` / `AmmoSlot` 全部直接建立在 `CompoundTag` 上
（`GunState` 走 `encodeToCompoundTag`/`decodeFromCompoundTag` 的 kotlinx NBT 格式），
换掉它等于重写整个枪械数据层。**`CompoundTag` 本身在 1.21.1 里仍然存在**（NBT 没死，
死的是 `ItemStack` 上的 NBT 槽位），所以正确做法是**保留 `CompoundTag` 作为内存态，只把
"它挂在物品上的哪儿、怎么读写"抽出来**。

**做法：一个适配点 `GunStackStorage`**

```kotlin
/**
 * 「物品上的枪械数据」的存取入口。**全仓唯一允许碰版本相关 API 的地方。**
 *
 * 1.20.1 (Forge)：数据就是 ItemStack 的根 CompoundTag（gun sub-tag 在 `GunData` 键下）。
 * 1.21.1 (NeoForge)：数据是一个自注册的 DataComponent（内容仍是一份 CompoundTag），
 *                    读写走 `stack.get(...)` / `stack.set(...)`。
 *
 * ⚠ 1.21.1 侧**待验证**的一点：`GunData` 会把根 tag 与三个子 compound 捕获成 `val`，
 *    所以组件实现必须保证"同一次装配拿到的 tag 实例"与"后续写回时用的实例"是同一个
 *    （1.20.1 侧靠 `ItemStack(item, 1, tag)` 的活引用 + 那句 `if (stack.tag !== liveTag)` 兜底，
 *    1.21.1 侧要在组件写入路径上做等价的事）。**这条不验证就先别动手**，
 *    否则三期 §11.8.3 那三个症状会原样复现。
 */
interface GunStackStorage {
    /** 取（必要时创建）这份栈的根 compound —— 语义等同 1.20.1 的 `stack.getOrCreateTag()` */
    fun rootTag(stack: ItemStack): CompoundTag

    /** 只读：没有就是 null，**不产生副作用**（不要用它去"探测"再写入） */
    fun rootTagOrNull(stack: ItemStack): CompoundTag?

    /** 这份栈是否已经带着枪械数据（替代散落各处的 `stack.tag != null` / `hasTag()`） */
    fun hasData(stack: ItemStack): Boolean

    /**
     * 载体身份令牌。**替换三期 `SubWeaponRuntime` 里的 `liveTag` 引用比较**：
     * 1.20.1 返回 `rootTag` 的 `System.identityHashCode`（活引用的同一性）；
     * 1.21.1 返回 DataComponent 的 patch 版本号 / 递增序号。
     * 只用来回答"还是不是我上次看到的那份载体"，**不参与等值判断**（等值用 CompoundTag 的 `==`）。
     */
    fun carrierToken(stack: ItemStack): Long
}
```

配套的 `GunData` 侧改造（同样是**两个分支各一份实现**，接口共用）：

| 现有 API（1.20.1） | 四期抽象 | 说明 |
|---|---|---|
| `GunData.setDefaultDataId(stack, id)` | 不变（内部改走 `GunStackStorage`） | 载具武器与副武器共用同一套机制（§11.9-C） |
| `stack.getOrCreateTag().getCompound("GunData")` | `GunStackStorage.gunStateTag(stack)` | 三个子 tag（gun / perk / attachment）的关系不变 |
| `Attachment.getOrCreateTag(slot)` 返回**活引用** | `GunStackStorage.subTag(root, key)`；**"活引用"的保证由实现负责** | 三期全部坑的根源就在"必须是活引用"（§11.8.3），1.21.1 侧必须在**同一个 compound 实例上原地改**再 `set` 回去，**不能每次读出来一份副本** |

**四期必须一起改掉的 1.20.1-only 写法**（现在散在 `SubWeaponRuntime` 里）：

| 位置 | 现状 | 改成 |
|---|---|---|
| `SubWeaponRuntime.assemble` | `ItemStack(item, 1, liveTag)` + `if (stack.tag !== liveTag) stack.tag = liveTag` | `GunStackStorage.writeRoot(stack, liveTag)`（1.20.1 实现就是原逻辑，1.21.1 实现是组件写入） |
| `SubWeaponRuntime.installed` | `cached.liveTag !== incoming` 引用比较 | `cached.token != GunStackStorage.carrierToken(stack)` |
| `Instance.liveTag: CompoundTag` | 直接持有根 tag | 改成持有 `(storageImpl, rootTag, token)` 三元组，或干脆持有 `GunData` + token |
| `foldIncoming` | `target.merge(source)`（CompoundTag 语义） | 不变（`CompoundTag` 是内存态，两版本一致） |
| `class SubWeaponRuntime` KDoc 的不变式 ② | 通篇讲"tag 引用必须全程不变" | 改成讲"**载体令牌必须全程不变**"，并把 1.20.1 的实现细节收进 `GunStackStorage` 的 KDoc |

**其它版本相关点**（都不影响四期的业务逻辑，但移植时要一并处理）：

| 项 | 1.20.1 | 1.21.1 | 影响面 |
|---|---|---|---|
| `ForgeRegistries.ITEMS.getValue(id)` | 现用 | `BuiltInRegistries.ITEM.get(id)` | `SubWeaponRuntime.installed` 一处 |
| `player.persistentData`（若要用） | 有 | 有（`Entity#getPersistentData` 仍在） | 本方案不需要它（状态写进枪 NBT） |
| 网络包注册 | KSP `@RegisterPacket` + `SimpleChannel` | `RegisterPayloadHandlersEvent` + `CustomPacketPayload` | `SubWeaponDeployMessage` 等新报文，两个分支各一份注册胶水（**报文内容与 handler 逻辑共用**） |
| 物品模型 / datagen | `ItemModelProvider` | `ModelProvider`（字段有差异） | `ModItemModelProvider` 侧 |
| 物品稀有度 / 属性 | `Item.Properties#rarity` | **DataComponent**（`localmod/README.md` 第五节："物品属性：`Item.Properties` ↔ DataComponent"） | `ModItems.registerSubWeapon` 一处；**注意这正说明"物品属性"这一层也要走适配点**，别在新代码里散写 |
| 附件/枪数据的 JSON | 完全一致 | 完全一致 | 数据包侧零分叉（`localmod/README.md` 的"字段集刻意取交集"同款思路） |

**验收这条的判据**：四期落地后，
**`grep -rn "\.tag" src/main/kotlin/.../subweapon/ src/main/kotlin/.../data/attachment/` 应当为空**，
版本相关 API 只出现在 `GunStackStorage` 的实现文件里。
（1.21.1 分支的实际移植**不在本期范围**，本条只保证"移植时不用重写业务逻辑"；
移植开工前**必须先做掉上面那条 ⚠ 的验证**。）

#### 9.8.10 报文与状态机

```
// 客户端 → 服务端：请求切换
SubWeaponDeployMessage(
    slot: String?,        // null / "" = 切回主武器；否则 = 要部署的副武器槽位（"SUBWEAPON"）
    clientActive: String? // 客户端认为当前是什么（仅用于日志对账，服务端不据此决策）
)

// 服务端 → 客户端：确认（客户端收到才演切换动作）
SubWeaponDeployedMessage(
    slot: String,         // 切换后的 ActiveSlot（""=主武器）
    owner: SerializedUUID?, // 宿主枪 UUID
    ok: Boolean           // false = 服务端拒绝（例如槽位其实没装、部署被边缘条件作废）
)
```

**服务端 handler**（`SubWeaponDeployMessage`）：

1. `player.isSpectator` → 忽略；
2. `stack = player.mainHandItem`；`!GunItem.isHeldWeapon(stack)` → 拒绝（**注意这里看的是真·主手**，
   因为整套状态就挂在主手那把枪的 NBT 上）；
3. `slot` 为空 → 清 `ActiveSlot`/`ActiveOwner`；
4. `slot` 非空 → `SubWeaponRuntime.find(gun, slot, client = false)`，找不到 / 配件不是 `SubWeaponItem`
   / 基线数据解析不出来 → 拒绝并记日志；
5. 通过 → `gun.activeSlot.set(slot)` + `gun.activeOwner.set(hostUuid)` + `gun.save()`；
6. 回 `SubWeaponDeployedMessage`；同时按 §9.8.4 **打断**两把枪的换弹/拉栓；
7. `melee_debug_log` 打开时打印一条 `[SubWeapon] deploy ... -> ...` 对账日志。

**状态机的三个不变量**：

1. **`ActiveSlot` 只能指向"主手那把枪身上确实装着的副武器"**。任何一次读取都要重新校验
   （槽位还在、物品还是 `SubWeaponItem`、`ActiveOwner` 等于主手枪的 UUID），不通过就当主武器用
   并且**顺手把状态清掉**（自愈，而不是每 tick 报错）。
2. **主手物品真的换了 → 自动收起**。落点是现成的"切枪检测"（`LivingEventHandler:331`，
   它本来就在比较新旧主手物品），在那里追加一次"清 `ActiveSlot`"。这样滚轮换枪 / 丢枪 / 死亡
   都不会留下悬空状态。
3. **服务端是唯一写入方**。客户端只发请求、只读确认，**永不自己写 `ActiveSlot`**
   （三期 §11.8.1-⑫"开火还是装填由服务端一个人决定"的教训：两边各写一次就会出现永久静默）。

#### 9.8.11 与近战/`MeleeEffect` 的交互（必须一起改的地方）

| 位置 | 现状 | 四期 |
|---|---|---|
| `MeleeClientHandler.tick` 的 G 分支 | G → `SubWeaponClientHandler.tryTrigger`，返回 `false` 才落到近战 | G → 发 `SubWeaponDeployMessage`，**永远不落到近战**；没有副武器时才与 V 共享近战入口 |
| `MeleeClientHandler` 的 `data` | `GunData.from(stack)`（主手） | **近战恒用主手**：`data` 保持主手（与"操控的枪"是两件事），`syncServerDrivenLocks` 也只同步主手的换弹/拉栓 |
| `item.hasMeleeAttack(data)` / `data.meleeActions()` | 主手 | 不变（近战是主武器的能力） |
| `MeleeAttackMessage.source` | `"MAIN"` / `"SUB:<slot>"` | **只保留 `"MAIN"`**：副武器没有近战，"副武器的近战形态"这条链路（三期 §11.8-⑧）删除；服务端遇未知 source 直接拒绝 |
| 近战动画 | `ClientEventHandler.isGunMeleeActive(stack)`（主手 stack） | 不变（副武器激活时挥的是主武器的近战动画，正是想要的效果） |
| 副武器激活时按 V | — | 主武器做近战动作、主武器结算；副武器挂在枪上不动（§9.8.2） |
| `GunActionLock` | `SUB_WEAPON` = 副武器开火 | `SUB_WEAPON` = 切换中（§9.8.8） |
| `/sbw subweapon info` | 打印槽位/数据 id/弹药/冷却/`canShoot` | 改为打印：槽位 / 数据 id / 是否激活 / 弹药 / 备弹 / 换弹状态；**删掉"冷却"与 `canShoot`**（那是"按 G 触发一次"时代的字段） |

---

## 10. 调试与工具

| 工具 | 内容 |
|---|---|
| 判定体可视化 | `MeleeHitbox` × `MeleeSweep` 采样体线框 + 朝向 + 扫掠箭头 + 打头/打腿高度线（`RenderType.lines()`，参考 `C4Renderer.kt:57`） |
| 调试命令 | `/sbw melee debug`、`info`（打印解析后的有效动作表与来源）、`force <idx>`；`/sbw subweapon info`（打印当前解析出的副武器：槽位 / Data id / **是否激活** / 弹药 / 备弹 / 换弹状态） |
| 日志 | 未命中原因、命中区域、效果触发与概率、**动作锁拒绝原因**、**G 的切换结果**（仅 debug 开关下） |
| DataValidator | 形状参数、`Effects` 预设/`Type`、`MaxTargets`/`Falloff`/`HitTime`/`Cooldown`、**`SubWeapon.Data`（含默认取物品 id 的情况）能否解析到枪数据**、`SubWeapon` 候选链里的空名字、挂点组冲突、靠 `ProjectileAmount<=0` 隐式判近战的迁移提示 |
| 资源校验 | `GunAnimation.Melee` 的 clip 名是否存在；`bayonet_pos`、`sub_weapon_pos`、`iron_view` 等骨骼是否存在（**属资源侧，四期仍未做**） |

### 10.1 一期实际落地的形态

| 工具 | 实现 | 与上表的差异 |
|---|---|---|
| 判定体可视化 | `client/renderer/special/MeleeDebugRenderer.kt`：`RenderLevelStageEvent.AFTER_ENTITIES` 画每个扫掠采样点的 OBB 线框（绿=精确形状 / 黄=圆锥·胶囊近似 / 红=正在挥的那一段） | **未画**朝向箭头、扫掠箭头、打头/打腿高度线；**触发方式**是原版 `F3+B` **或** 配置项 `melee_hitbox_render`（任一即可），不是新键位 |
| 调试命令 | `/sbw melee info` / `actions` / `force <idx> [entity]`（`command/MeleeCommand.kt`） | **没有 `debug` 子命令**（开日志改用配置项）；`force` 要经 `command/MeleeDebugHooks.kt` 转交客户端执行（判定只在客户端做，而 `/sbw` 是服务端注册的） |
| 日志 | `DisplayConfig.MELEE_DEBUG_LOG`（`melee_debug_log`，默认 `false`）：挥击（下标/时长/结算 tick/按键来源）、命中（目标数/距离/夹角/打头打腿）、未命中 | **动作锁拒绝原因**目前没打日志（只有 debug 日志里的按键来源） |
| 判定体外框 | `F3 + B` 或 `DisplayConfig.MELEE_HITBOX_RENDER`（`melee_hitbox_render`，默认 `false`），**任一即可**；主手需为能近战的枪 | 最初误写成"两个条件同时满足"，导致只开一个时看不到（§11.2-⑲） |
| DataValidator | `DataValidator.validateMeleeData`：形状参数自相矛盾、`MeleeSweep` 跨度、`HitTime > Duration`（永远不会出伤）、`Falloff`/`BypassesArmor`/`Chance` 越界、`Effects` 条目缺 `Effect`/`Type`、**隐式近战迁移提示**（带"该枪有弹种能改回 `ProjectileAmount`"的排除条件） | `SubWeapon.Data` / 挂点组相关的校验属于二·三期 |
| 资源校验 | 运行时 error 日志 + 回退第一支 clip | **资源加载后校验未实现**（§5.4 注） |

> 命令与可视化的用法示例：
> ```
> /sbw melee info        # meleeOnly 判定来源 / 形状 / 横扫 / 连招窗口 / 冷却表
> /sbw melee actions     # 逐段：animation、duration、hitTime、形状、伤害、倍率、冷却、effects 数量
> /sbw melee force 0     # 立刻按第 0 段结算一次（不挥、不占锁，方便反复调参）
> ```

---

## 11. 分期落地

| 阶段 | 状态 | 范围 |
|---|---|---|
| **一期：近战本体** | ✅ **已完成** | 判定形状/扫掠、连招、命中区域、伤害类型与标签、`@melee`、动作锁、NBT 冷却表、G 键语义、调试工具 |
| **二期：配件体系 + 刺刀** | ✅ **已完成**（§11.5） | `AttachmentProvider`、槽位注册表 + 挂点组、`BAYONET` + `bayonet_m_9`、注册表驱动的通用配件渲染 |
| **三期：`MeleeEffect` + `SubWeapon`** | ✅ **已完成**（§11.8） | `MeleeEffect` 行为注册表 + `sbw/melee_effects` 预设 + 12 个首发行为；`SubWeaponInfo`、`SubWeaponItem`、`SubWeaponRuntime`、`SUBWEAPON` 槽位、GP-25 下挂榴弹 |
| **三期后续：副武器开火表现** | ✅ **已完成**（§11.9） | `SubWeapon.Animation` 候选链、枪口焰/烟改挂副武器、`SubWeaponInfo.Data` 落地、`Semi`/`Auto`/`Burst`、装填只在持有主武器时推进 |
| **四期：副武器「主/副武器切换」机制** | 🟡 **设计完成，未实现**（§9.8 / §11.10） | G = 主/副武器切换；`GunState.ActiveSlot` + `ActiveGun` 读取入口；副武器走完整开火/换弹/瞄准链路；近战恒用主武器；副武器**自带资源与换弹动画**（附件 animPath + `BedrockAttachmentModel.applyPose`）；`GunStackStorage` 双版本适配 |
| **四期后续** | ⏳ 未做 | 副武器专属 `iron_view` 骨骼与 `hold_sub_weapon` 动画（美术）、副武器专属 HUD/准心、1.21.1 分支的实际移植（按需求由需求方自行推进） |

一期新增/改动的主要落点：

| 类别 | 文件 |
|---|---|
| 数据类（**收在 `data/gun/melee/` 子包**，§11.2-⑯） | `MeleeAction.kt`、`MeleeHitbox.kt`、`MeleeHitboxType.kt`、`MeleeSweep.kt`、`MeleeSortBy.kt`、`MeleeEffectSpec.kt`、`ResolvedMeleeAction.kt`、`ProjectileMarker.kt` |
| 数据接线 | `DefaultGunData.kt`、`GunProp.kt`、`DefaultGunDataOverrides.kt`、`GunData.kt`、`DataValidator.kt` |
| 判定 | `tools/MeleeQuery.kt`（**取代 `doGunMeleeAttack`**） |
| 客户端运行时 | `client/gun/GunActionLock.kt`、`client/gun/MeleeClientHandler.kt`、`client/animation/gun/GeoGunAnimationInstance.kt`、`event/ClientEventHandler.kt`、`event/ClickEventHandler.kt` |
| 服务端结算 | `network/message/send/MeleeAttackMessage.kt`、`event/LivingEventHandler.kt`、`perk/MeleeAttackContext.kt`、`data/gun/subdata/Cooldown.kt`、`event/GunEventHandler.kt` |
| 伤害类型 | `init/ModDamageTypes.kt`、`init/ModTags.kt`、`datagen/ModDamageTypeTagProvider.kt`、`tools/DamageTypeTool.kt`、`damage_type/gun_melee{,_headshot}.json`、`tags/damage_type/melee.json`（datagen 产出） |
| 键位 / 配置 | `init/ModKeyMappings.kt`（`SUBWEAPON_FIRE`）、`config/client/DisplayConfig.kt`（`MELEE_DEBUG_LOG`） |
| 调试 | `command/MeleeCommand.kt`、`command/MeleeDebugHooks.kt`、`client/renderer/special/MeleeDebugRenderer.kt` |
| 语言 | `en_us.json`、`zh_cn.json`（**只补这两个**，§11.2-⑱） |

**验收方式**：`./gradlew clean build`（已通过）→ 手动步骤见 §11.4 末尾的「一期验收步骤」。

### 11.1 一期：近战本体（判定 + 连招 + 命中区域 + 伤害类型 + `@melee` + 动作锁 + 冷却基建 + G 键语义）

> **状态：✅ 全部完成**（`clean build` 通过；`runData` 已生成 `tags/damage_type/melee.json`）。
> 下表逐项对应代码落点；带 ⚠ 的项在 §11.2 有差异说明。

| # | 项 | 状态 | 落点 |
|---|---|---|---|
| 1 | 数据类 + `DefaultGunData` 字段 + `GunProp` 条目 + `withOverrides` 写回（补上漏掉的三个） | ✅ | `data/gun/melee/*.kt`；`DefaultGunData.kt`（+`MeleeComboReset`/`MeleeHitbox`/`MeleeSweep`/`MeleeActions`，`clamped()` 同步）；`GunProp.kt`（+`MELEE_COMBO_RESET`/`MELEE_HITBOX`/`MELEE_SWEEP`/`MELEE_ACTIONS`，`modifyProperty` 加钳制）；`DefaultGunDataOverrides.kt`（补 ⚠ `MELEE_DAMAGE`/`MELEE_RANGE`/`MELEE_DAMAGE_TIME` + 4 个新字段） |
| 2 | `MeleeQuery`（形状 + 扫掠 + 排序/数量/衰减/命中区域），替换 `doGunMeleeAttack` | ✅ | `tools/MeleeQuery.kt`（`Cone`/`Box`(OBB)/`Capsule`、`resolve`、`coarseFilter`、`isHeadshot`/`isLegshot`、`debugBoxes`）；`ClientEventHandler.doGunMeleeAttack` **已删除** |
| 3 | 连招：per-action `Duration`/`HitTime`/`Cooldown`、下标锁存、`MeleeComboReset`、客户端按枪隔离计数器 | ✅（⚠ ①） | `client/gun/MeleeClientHandler.kt` + `client/gun/GunActionLock.kt`（按 UUID 隔离的 `State`） |
| 4 | 动画：`GunAnimation.Melee` → `SingleOrList<String>`、按本段时长拉伸、名字解析与校验 | ✅（⚠ ②⑪） | `resource/gun/GunAnimation.kt`（+`firstMeleeName()`）；`GeoGunAnimationInstance.kt`（`resolveMeleeName`/`meleePlaybackSpeed`/`swingSerial` 重播） |
| 5 | 音效：`MeleeSound` 动作级覆盖 | ✅（⚠ ④） | `MeleeAction.Swing`/`Hit`（`SerializedSoundEvent?`）；客户端播 `Swing`，服务端播 `Hit ?: MeleeSound.Hit ?: MELEE_HIT` |
| 6 | 自定义冷却基建（枪 NBT 冷却表 + 服务端递减） | ✅（⚠ ⑧） | `data/gun/subdata/Cooldown.kt`（子 tag `MeleeCooldown`）；`GunData.cooldown`；`GunEventHandler.gunTickInternal` 里 `data.cooldown.tick()`；键 `melee:<idx>` / `effect:<id>` / `sub:<slot>` |
| 7 | `GunActionLock`（§9.5） | ✅（⚠ ⑨） | `client/gun/GunActionLock.kt`（`GunAction` 枚举 + `blocks()`/`acquire()`/`force()`）；开火侧接在 `ClientEventHandler.handleGunShoot`/`shootClient`，换弹/拉栓由 `MeleeClientHandler.syncServerDrivenLocks` 同步进锁 |
| 8 | 伤害类型 `gun_melee`/`gun_melee_headshot` + `#superbwarfare:melee` + `isMeleeDamage` + 6 处旧判断 | ✅ | `init/ModDamageTypes.kt`（+键 +2 个 `causeXxx`）、`init/ModTags.kt`（`DamageTypes.MELEE`）、`ModDamageTypeTagProvider.kt`、`tools/DamageTypeTool.kt`（`isMeleeDamage`，`isHeadshotDamage` 加 `GUN_MELEE_HEADSHOT`）、`damage_type/gun_melee{,_headshot}.json`、`LivingEventHandler.kt:227/258/500/534`、`PowerfulAttraction.kt:27/48/64` |
| 9 | `Projectile: "@melee"` + `@` 归一化（5 处 `ray`→`@ray`）+ `meleeOnly()` 显式化 + 兼容回退 + 近战枪缺口 | ✅（⚠ ⑫⑬） | `data/gun/melee/ProjectileMarker.kt`（`normalizeProjectileMarker`/`isMeleeProjectileMarker`）、`GunData.meleeOnly()`、`GunItem.shootBullet` 的 `@melee` 早退、`GunProp.modifyProperty` 的 `MAGAZINE` 钳制、`ClickEventHandler.handleWeaponFirePress` 左键直通 |
| 10 | **G 键 + §9.4 语义表** | ✅ | `init/ModKeyMappings.kt` → **`SUBWEAPON_FIRE`**（默认 `G`，按需求命名）；`MeleeClientHandler.tick` 里 `fromSubWeaponKey`，无副武器时与 V 共享同一入口与状态 |
| 11 | 修 §1.2 的缺陷 3/4/5/6/7/8/11 | ✅（缺陷 1/2/9 另见 §11.2-③⑨） | 全在 `MeleeAttackMessage.kt`：3=只在客户端 `swing`；4=`sweepAttack()` 移出循环；5=击退/无伤音效每次挥击最多一次；6=不再把攻击者动量写给受害者；7=主手非枪直接 return；8=衰减读 `Falloff` 字段；11=删掉死掉的原版冷却判断。**缺陷 1**（`gunMelee` 全局单例）=状态按枪隔离；**缺陷 2**（近战/开火无互斥）=`GunActionLock`；**缺陷 9**（三个字段漏写回）=已补 |
| 12 | override 嵌套解析改宽松（§8.7） | ✅ | `data/Prop.kt`：`OVERRIDE_JSON = Json(DataLoader.JSON)`（`ignoreUnknownKeys = true`），`deserialize` 走它 + 失败打 `warn`（属性名 + 原始 JSON） |
| 13 | 兼容验证：旧枪 json 一行不改、行为一致 | ✅（⚠ 见 §11.3） | 22 把 `MeleeDamage > 0` 的枪 json **零改动**；`meleeOnly()` 兼容回退；`HitTime` 逐 tick 对齐旧实现；`@ray` 的 5 个文件是等价改写 |
| 14 | 调试：判定体可视化 + `/sbw melee info` | ✅（⚠ ⑩⑭） | `client/renderer/special/MeleeDebugRenderer.kt`、`command/MeleeCommand.kt`、`command/MeleeDebugHooks.kt`、`DisplayConfig.MELEE_DEBUG_LOG` |
| — | 语言文件 | ✅ | `en_us.json` / `zh_cn.json`：`death.attack.gun_melee*`、`key.superbwarfare.subweapon_fire`、`commands.superbwarfare.melee.*`、`config...melee_debug_log`（**只补这两个语言**，其余语言按需求保持原样） |

### 11.2 正式实现与本文不一致的地方

标注为「设计 → 实现」；**以代码为准**。带 ⚠ 的项同时是踩过的坑，改代码前务必先读。

**① ⚠ `HitTime` 的判定条件（已修，但语义容易写错）**
设计：`t=HitTime` 结算。实现：内部 `meleeTicks` 从 `Duration` 起、**每帧开头**递减，出伤条件是 `meleeTicks <= Duration - HitTime`。
最初的实现写成了 `meleeTicks <= HitTime`，导致 `Duration=16, HitTime=6` 时**第 11 tick 才出伤**（动画早演完，手感明显发粘）。
现由 `ResolvedMeleeAction.hitTickFromStart = duration - hitTime` 统一换算；另 `HitTime` 会 `coerceIn(0, duration)`。

**② ⚠ 连续挥击的动画重播（设计没写）**
`PLAY_ONCE_HOLD` + 状态机"只在状态切换那一帧解析 clip"，使得按住 V 连挥时**只有第一段有动画**。
实现新增 `MeleeClientHandler.swingSerial` + `GeoGunAnimationInstance.consumedMeleeSerial`（照抄 `fireSerial` 套路）。
序号必须在 runner 判定**之前**消费，否则第一段会被重播两次。

**③ 伤害公式：不再乘 `ATTACK_DAMAGE` 属性**
设计 §6.2-7「伤害数值直接读 `MeleeDamage`；`getAttributeModifiers` 的 `ATTACK_DAMAGE` 加成保留」。
实现取前半句：`damage = action.damage * 命中区域倍率 * 衰减`，**`ATTACK_DAMAGE` 只保留属性本身**（对其它系统/显示仍有意义），不参与近战结算。
理由：再乘一次会让 `MeleeDamage` 被"属性加成"二次放大，与 `MeleeAction.Damage` 的语义打架。
`ATTACK_KNOCKBACK` 属性仍然照旧参与击退。

**④ `MeleeAction.Swing` / `Hit` 的类型是 `SerializedSoundEvent?`，不是 `String?`**
理由：与 `MeleeSound` 的字段类型一致，JSON 写法不变，但少了"字符串 → SoundEvent"的一层手工解析。

**⑤ 打腿倍率的缺省值**
设计 §3.5 表格写 `Legshot` 缺省 `1.5 / 0.5`（即打头 1.5 / 打腿 0.5）。
实现：**打头不写时继承枪的 `Headshot`**（AK-47 是 2，不是 1.5），打腿缺省 `0.5`。
这是"全局不新增打头/打腿字段、复用枪的 `Headshot`"（§3.2 注）的必然结果，表格里的 1.5 只是"投射物默认值"的泛称。

**⑥ 报文形状**
- `targets` 是 `List<TargetPayload>`（`uuid` + `hitX/hitY/hitZ` + `distance`），不是 `List<UUID>`：打头/打腿改由**服务端**按 `hitPos` 判定，才能选对 `gun_melee_headshot`。
- `source` 是 `String`（`"MAIN"` / `"SUB:<slot>"`），不是 `MeleeSource` 枚举；常量 `MeleeAttackMessage.SOURCE_MAIN`。

**⑦ 击退/无伤音效的粒度**
设计只要求"修缺陷 5"。实现统一为：**每次挥击，命中音效与无伤害提示音各最多响一次**
（命中音效落在第一个真正受伤的目标身上，`attacker.crit(target)` 同处）。

**⑧ 冷却键命名**
设计写 `冷却键 → 剩余 tick` + 副武器用 `sub:<slot>`。实现加了目的前缀以免共享一张表时撞键：
`melee:<actionIndex>`、`effect:<effectId>`、`sub:<slot>`（`Cooldown.meleeKey/effectKey/subWeaponKey`）。

**⑨ 动作锁的具体接法**
- 开火：`handleGunShoot` 入口处 `blocks(FIRING)` 早退；`shootClient` 里 `force(FIRING, 一个射击周期)`。
  用 `force` 而不是 `acquire`，因为连发/LOW-RPM 补帧会在自己的占用还没走完时再次开火。
- 换弹/拉栓：沿用各自状态机，由 `syncServerDrivenLocks` **每 tick 同步进锁**（状态转假立即释放），不反向控制状态机。
- `SUB_WEAPON` 语义已就位但三期才会被真正占用。

**⑩ 调试入口与可视化**
- `/sbw melee debug` **没有**这个子命令；开日志用配置项 `melee_debug_log`。
- `force` 不是服务端直接打伤害：`/sbw` 是 `RegisterCommandsEvent` 注册的服务端命令，而判定只在客户端做，
  所以经 `MeleeDebugHooks`（可替换函数字段）转交客户端执行；专用服务端上会明确报 `fail.no_client`。
- 可视化只画采样盒线框，**朝向箭头 / 扫掠箭头 / 打头打腿高度线未实现**；触发方式是 `F3+B` + 配置项，不加新键位。

**⑪ `GunAnimation.Melee` 的"资源加载后校验"未实现**：目前只在运行时第一次播放失败时打 error 并回退 `melee[0]`。

**⑫ `@melee` 缺口清单只做了一半**：左键直通近战已做；`CanZoom: false` 属于数据侧约定（未强制）；
`MeleeAction.Durability` 已接；文档未补"近战枪资源只需 `Idle` + `Melee`"这句说明。

**⑬ `meleeOnly()` 的兼容回退仍在用**：22 把旧枪里没有一把写 `ProjectileAmount <= 0`（`beast_gun_test.json` 是唯一一个，
它是测试枪），所以回退路径目前**没有任何正式枪走**；`DataValidator` 会对真正命中回退的枪发迁移警告
（额外排除"有弹种能改回 `ProjectileAmount`"的情况，例如 `secondary_cataclysm.json` 的近战弹种，那是合法的）。

**⑭ 迁移提示的排除条件（设计没写）**：`ProjectileAmount <= 0` 在"某个弹种把弹丸数覆盖成 0"的枪上是合法写法，
所以只有**没有任何弹种能改回 `ProjectileAmount`** 时才提示迁移到 `@melee`。

**⑮ 旧 GeckoLib 路径明确不迁移**（按需求）：`GunGeoItem.kt` / `SecondaryCataclysmItem.java` 只做了
`GunAnimation.Melee` 改型导致的编译修正；`ClientEventHandler.gunMelee` 退化为**恒为 0 的 `@Deprecated` 占位字段**，
旧枪械的近战动画不会再触发，其近战伤害照旧（走同一条 `MeleeAttackMessage`）。

**⑯ 代码组织：melee 数据类收进子包**（按需求）
设计 §3 把 `MeleeHitbox`/`MeleeSweep`/`MeleeAction` 说成"顶层属性"，指的是**JSON 顶层字段**，这一点没变；
但 Kotlin 类放在 `com.atsuishio.superbwarfare.data.gun.melee` 子包下，不再平铺在 `data/gun/`。
`MeleeSound`、`Cooldown` 未移动（前者是既有类，后者本就在 `subdata/`）。

**⑰ Perk 上下文的落地方式（设计 §6.3 只说"补上上下文"）**
实现为 `perk/MeleeAttackContext.kt`：同 tick 传递的 `(actionIndex, source, action)`，
并给 `Perk` 加了**带上下文的两个新重载**（`onMeleeSwing(data, instance, entity, context)` /
`onMeleeAttack(data, instance, target, source, context)`）。**旧签名照旧会被调用**（在旧方法之前调用新方法），
所以既有 `OneTwoPunch`/`CastNoShadows`/`JsPerk` 与 JS 脚本零改动。

**⑱ 语言文件只补 `en_us` + `zh_cn`**（按需求）：其余语言缺失的键在游戏内会显示原始键名。

**⑲ 判定体外框的触发条件（已修）**
最初实现把"`F3+B` 打开"与"`melee_debug_log` 打开"写成了**同时满足**（`&&`），
而那个配置项默认 `false` —— 结果只开一个开关时什么都看不到。
现在拆成两个独立开关：日志 = `melee_debug_log`；线框 = **`F3+B` 或 `melee_hitbox_render`（任一即可）**。

> **顺带发现的既有配置 bug（未修，不属一期范围）**：`config/Config.kt` 的
> `buildConfig(builder, vararg configs)` **把 `configs` 参数丢掉了**，返回的只是 `builder.build()`。
> 于是各个 config 对象里的 `push("display")` / `push("control")` **不会在对象之间弹栈**，
> 后面的对象被嵌进前面对象的 section：客户端配置里 `melee_debug_log`、`melee_hitbox_render`、
> `enable_gun_lod` … 实际都被写进了 **`[kill_message]`** 段而不是 `[display]`。
> 修它需要改成 `builder.configure(DisplayConfig::class.java) { it.<field> }`，
> 会**改变现有配置文件的键路径**（老配置文件里的值会失配、回默认值），所以留作单独一次改动。

**⑳ ⚠ `Box`/调试盒的 yaw 旋转符号写反了（已修，影响判定本身）**
MC 的 yaw 是**从 +Z 朝 +X** 增加的（`look = (-sin(yaw), 0, cos(yaw))`，yaw 90° 看向 **-X**），
而 JOML 的 `rotateY(θ)` 是右手系绕 +Y，把局部 +Z 转到 `(sin θ, 0, cos θ)`（θ=90° 指向 **+X**）——
两者**手性相反**。最初写成 `Quaterniond().rotateY(+yaw)`，导致：

- **可视化**：盒子与人物朝向差 180°（yaw 0/180 时看不出来，45°/90° 最明显）；
- **判定**：`BoxHitbox` 的 OBB 也歪 180° —— 这是一个**真实的命中 bug**，不是纯显示问题
  （正面 2.6 格长的盒体实际戳到了身后）。

修正为 `MeleeQuery.yawPitchQuaternion(yaw, pitch) = Quaterniond().rotateY(-yaw).rotateX(+pitch)`
（㉑ 又补上了 pitch），**判定与调试渲染共用同一个函数**，
避免"看到的盒子"≠"判定的盒子"。仓库里的同类转换也都是这个符号：`VectorTool.combineRotationsYaw`、
`VehicleMotionUtils` / `VehicleVecUtils` 的 `Axis.YP.rotationDegrees(-vehicle.yRot)`、`CameraMixin`、
`C4Entity` 的 `.rotateY(-yaw)`。

> 已用 JOML 1.10.5 实测过符号（yaw ∈ {0, ±45, 90, 135, 180}）：
> `rotateY(+yaw)` 的局部 +Z 除 0/180 外全部不符，`rotateY(-yaw)` 的局部 +Z（前方）与局部 +X（右方）**全部吻合**。

**㉑ `Box` / `Capsule` 的判定体现在跟随 `pitch` 一起旋转（相对设计稿的功能增强）**
设计 §3.3 只写了"绕 Y 旋转 `yaw`"——那是个**只能水平转**的盒子（yaw 之外的姿态恒为 0）。
实现改成**全姿态**：`MeleeQuery.yawPitchQuaternion(yaw, pitch)` =
`Quaterniond().rotateY(-yaw).rotateX(+pitch)`，三根轴分别是
**+Z → 视线**、**+X → 水平右方**、**+Y → `look × right`**。

- 枪托/刺刀本来就是「沿视线捅出去」的，抬头砍、低头砸时盒子该跟着转，否则判定和视觉都对不上；
- `MeleeHitbox.YOffset` 一并从"写死世界 Y"改成"沿局部 +Y 偏移"；
- `MeleeSweep` 仍然只转水平（在 yaw 上加偏移），这点没变；
- 旧枪全是 `Cone`，不受影响；只有显式写 `"Type": "Box"` / `"Capsule"` 的枪吃这个改动。

> 实测覆盖 yaw ∈ {0, ±45, 90, 135, 180, 200, -135} × pitch ∈ {-90, -75, -60, -45, -30, 0, 10, 20, 30, 45, 55, 89, 90}：
> `+Z==lookVector`、`+X==水平右方`、`+Y==look×right`、三轴正交且单位长度，**全部成立**。
> 符号规则记两条：**yaw 要取负**（MC 手性与 JOML 相反）、**pitch 用正值**（MC 俯仰向下为正，JOML `rotateX(θ>0)` 也朝下）。

**㉒ 目标获取链的两处修复 + 诊断日志**
排查"判定体罩住了却打不到"时改掉的两处（都属于"近战距离太短"才会暴露的问题）：

1. **`hasLineOfSight` 的射线起点会落在攻击者自己的碰撞箱里**（`MeleeQuery.hasLineOfSight`）。
   目标贴到脸上时，目标 AABB 的最近点会落在攻击者自身碰撞箱内部，而 `level.clip` 的起点只要在
   方块里就立刻返回 `BLOCK` → **贴脸砍永远打不中**。现在起点用 `pushOutside()` 沿射线推到自身
   碰撞箱之外再 `+1e-4`，终点用 `to - dir*1e-4` 内收，避免把终点探到目标背后的方块里。
2. **粗筛半径没有覆盖形状自身的尺寸**（`MeleeQuery.coarseFilter`）。
   `Cone`/`Capsule` 只吃 `range`，但 `Box` 用的是 `length`（`range` 不参与判定），
   粗筛却只按 `reach` 画球 —— `"Length"` 大于 `reach` 时（如 `Length: 8, Range: 0`）
   盒子前段的目标会被粗筛直接漏掉。现在按形状取 `max(ZFrom + Length, reach)`。

同时加了**诊断日志**：开 `melee_debug_log` 后每次挥击会打印
`hitbox / reach / occlusion -> hits=N`，以及**每个候选卡在哪一步**：

```
[Melee] melee hitbox=Cone reach=4.20 occlusion=true -> hits=1 [HIT zombie d=1.83 a=6.4]
        [shape skeleton d=5.10 a=88.2 dyaw=88.2/50.0 dpitch=1.1/35.0]
        [occlusion creeper d=2.40 a=12.0]
        [no entity passed the coarse filter]      ← 一个候选都没有时
```

> **已经用数值实验排除的两个"看起来很像"的猜测**（别再往这两个方向改）：
> - ❌ **"`Cone` 的夹角量到了脚底"**：`closestPointInBox` 是把眼睛**夹进** AABB，站着打站着时
>   y 会被夹到**眼睛高度**，最近点在眼平线上，平视时 `Δpitch = 0`。
>   （实测：平视 0°、俯视 30° 打胸口都在 `Pitch/2 = 35°` 容差内；
>   改成"量到 AABB 中心"反而会让俯视 30° 打 1.7 格的目标 `Δpitch` 变成 50.8° → **更容易漏**。）
> - ❌ **"`OBB.isColliding(obb, aabb)` 本身是错的"**：用 JOML 1.10.5 复刻了 `OBB.isColliding` 的
>   全部入参顺序，正面 / yaw 45 / yaw 90 / yaw −90 各距离下僵尸 AABB **全部正确相交**，
>   只有超出盒体长度时才不相交。判定几何没有问题。

**㉓ ⚠ 调试线框在骗人：它画的是"近似盒"，不是真实形状（已改为画真实形状）**
`debugBoxes` 把三种形状统一成一个盒体近似，`Cone` 画成"宽 `reach×sin(半角)`、长 `reach/2` 的盒子"。
而 `reach = Range + getEntityReach()` 通常是 **6~7 格**，于是那个盒子**又粗又短**，
看的人会得出完全错误的结论 —— 实测日志（`latest.log`）里就是这两句：

> "OBB 里面有实体，就是打不到；OBB 没有实体，但是打到了"

**判定其实一直是对的**。日志给出的分界干干净净：

```
reach = 7.20 (= Range 1.2 + getEntityReach() 6.0)
HIT  : d = 3.54 ~ 7.18      ← 全部 ≤ 7.2
MISS : d = 7.28, 7.37, 7.54, 7.84, 7.92   ← 全部 > 7.2   （[shape]，纯距离超限）
```

`"OBB 里打不到"` 的观感来自近似盒**画得太短**（只有 `reach/2 = 3.6` 长），
6~7 格处命中的实体自然落在画出来的盒子之外；
`"OBB 外反而打到"` 则来自近似盒**画得太粗**（末端半径 `reach×sin(50°) ≈ 5.5`，实际末端的
角度边缘是个锥面，盒子的四个角超出了锥外）。

修正：`MeleeQuery.debugBoxes` → **`MeleeQuery.debugShapes`**，返回一个 `sealed interface DebugShape`
（`Cone` / `Box` / `Segment`），渲染侧按形状分别画：

| 形状 | 线框 |
|---|---|
| `Cone` | 顶点在眼睛 + 末端圆环（半径 `reach×sin(半角)`）+ 母线；`Pitch >= 180` 时垂直不受限，只画球面天线罩 |
| `Box` | 带 yaw/pitch 姿态的盒体（与判定共用 `yawPitchQuaternion`） |
| `Capsule` | 沿视线的线段 + 两端端盖圆 |

**同时修了 `ak_47.json` 的数据**：`Pitch: 70` → `180`。
`Pitch` 是**总张角**，`70` 只有 `±35°` 容差，比旧实现（垂直不限）紧得多 ——
日志里 `[shape ... dpitch=37.5/35.0]`、`42.9/35.0`、`57.0/35.0`、`67.0/35.0` 这些
"怪就在眼前却打不到"全是它造成的。旧版垂直方向完全没有限制，要等价请写 `180`。

> **顺带说明 `reach` 为什么能到 7.2**：Forge 的 `getEntityReach()` 是
> `ENTITY_REACH 属性值 + (创造模式 ? 3 : 0)`。创造模式实测属性 3.0 → 6.0，加上 `Range: 1.2` 就是 7.2；
> 生存模式是 `3.0 + 1.2 = 4.2`。**在创造模式里试近战范围会得到比生存大一倍的数字，别被它误导。**

### 11.3 兼容性确认清单（行为发生变化的地方）

22 把旧枪 json 一行没改，但下面这些是**有意修正**，手感/结果会与改版前不同：

| 变化 | 旧 | 新 | 影响 |
|---|---|---|---|
| 距离口径 | 目标**脚底** `e.position()` 到眼睛 | 判定体到目标 **AABB 最近点** | 贴脸/仰角时更容易打到；远的反而更严格 |
| 遮挡 | `findMeleeEntity` 无条件用实体结果覆盖方块 pick（方块 pick 是死代码） | `MeleeHitbox.Occlusion`（默认 `true`）统一判定 | **不再能隔墙打人** |
| 角度参照 | 「眼 → 眼」夹角 vs 视线 | 视线 vs 「眼 → AABB 最近点」 | 命中集合略有位移 |
| 垂直角 | 3D 圆锥（俯仰与水平耦合） | `Cone` 默认 `Pitch = 180`（不限）→ 与旧版基本等价 | 旧枪缺省行为不变 |
| `Box`/`Capsule` 姿态 | 无（旧版没有形状判定） | **随 yaw + pitch 全姿态旋转**（§11.2-㉑），`YOffset` 沿局部上方向 | 只有显式写 `Box`/`Capsule` 的枪受影响 |
| 排序 | 射线目标强占 index 0 | 全部按 `SortBy`（默认 `Angle`） | 主目标更稳定 |
| 衰减 | `max((10-i)/10, 0.1)` | `max(1 - i*Falloff, 0.1)`，`Falloff` 默认 0.1 | 第 2 个目标 15→13.5（旧版 13.5 一致），第 3 个 15→12（旧版 12 一致） |
| 伤害基值 | `ATTACK_DAMAGE`（1.0 + `MELEE_DAMAGE`）× 衰减 | `MeleeDamage` × 命中区域 × 衰减 | **少了那个基础 1.0**；`MeleeDamage=15` 从 16 变 15 |
| 出伤 tick | `gunMelee == DURATION - DAMAGE_TIME` | `meleeTicks <= Duration - HitTime` | **逐 tick 一致**，无变化 |
| 切枪 | `gunMelee` 不重置，可能误触发一次攻击 | 状态按枪隔离 | 修掉缺陷 1 |

### 11.4 一期遗留 / 已知缺口
> **三期已补掉其中第 1 与第 8 条**（见 §11.8），其余仍然有效。

1. ~~**`MeleeEffectSpec` 只有数据与校验**~~ → ✅ **三期已实现**：`ModMeleeEffects` 注册表、
   `sbw/melee_effects` 预设，以及 §3.8 那张表里的行为（实际落地 **12 个** ——
   `sound` 与 `particle` 是两个独立行为）。落地时的差异见 §11.8.1-②③④⑥。
2. **`MeleeAction.Durability`** 在结算处接上了，但只对 `MAX_DURABILITY > 0` 的枪生效（多数枪没有耐久）。
3. **可视化**：无朝向箭头 / 扫掠箭头 / 打头打腿高度线。
4. **`ATTACK_DAMAGE` 加成"保留但不用"** 这一点建议后续明确取舍（见 §11.2-③）：要么删掉属性加成，要么把它映射成 `MeleeAction.Damage`。
5. **`GunAnimation.Melee` 的 clip 名资源加载期校验**未做。
6. **`@melee` 缺口**：`CanZoom` 约定未强制、`@melee` 枪的右键行为未定义。
7. **动作锁拒绝原因未打日志**（§10.1）。
8. ~~**§9.4 的"G 全部不可用时报一声 `TRIGGER_CLICK`"未实现**~~ → ✅ **三期已实现**：
   `SubWeaponClientHandler` 在"装了副武器但全部在冷却"时播 `ModSounds.TRIGGER_CLICK` 并吞掉这次按键
   （动作被占用时静默吞掉 —— 那种情况玩家刚按过别的键，再响一声只会吵）。

#### 一期验收步骤（手动）

```
1. 给一把能近战的枪写 MeleeActions（例如 ak_47.json 加 { "HitTime": 6 }），进游戏
2. 单次按 V      → 动画播一次、`HitTime` tick 后出伤
3. 按住 V       → 每 Duration tick 挥一次，**每段动画都重播**（§11.2-②）
4. 隔墙对怪按 V  → 打不到（Occlusion，§11.3）
5. 挥击途中按左键/按 R → 被动作锁拒掉，不产生副作用（§9.5）
6. 打头 / 打腿   → 伤害倍率分别是枪的 Headshot / 0.5，伤害类型走 gun_melee{,_headshot}
7. 开 melee_debug_log → 客户端日志出现 `[Melee] melee swing/hit/miss`；按 F3+B（或在配置里开 `melee_hitbox_render`）→ 看到判定体线框；`/sbw melee info|actions|force 0` 输出正常
8. 22 把旧枪（没写 MeleeActions/MeleeHitbox）→ 与改版前手感一致（唯一差别见 §11.3）
```

### 11.5 二期：配件体系 + 刺刀 ✅ 已实现

> **状态：✅ 已完成**（`compileKotlin` / `runData` 跑通；验收步骤见 §11.5.4）。
> `bayonet_m_9` 的 bedrock 模型与贴图由需求方提供（文件名仍是 `bayonet_knife.geo.json` / `bayonet_knife.png`）；**动作动画尚未制作**，刺刀先用枪自己的 melee clip（§11.5.3-1）。
> **改装界面（`WeaponEditScreen`）与 HUD 按需求一行未动**：新槽位在界面重写前只能用 `/sbw attachment` 指令安装（§11.5.3-⑤）。

| # | 项 | 状态 | 落点 |
|---|---|---|---|
| 1 | **配件物品接口化** | ✅ | 新增 `item/attachment/AttachmentProvider.kt`（接口只声明 `attachmentId`，`definition()` 是扩展函数）；`AttachmentItem.kt` → **`BasicAttachmentItem.kt`，旧文件直接删除、不留别名**；`ModItems.registerAttachment(id, rarity, factory = ::BasicAttachmentItem)`；4 处消费点全部改成接口判断（`ModItems`、`ClientAttachmentImageTooltip:43`、`AttachmentCommand:267`、物品类自身） |
| 2 | **槽位注册表化 + 挂点组基建** | ✅ | 新增 `data/attachment/AttachmentSlots.kt`：`AttachmentSlot`（`mount`/`conflictsWith`/`tagBucket`/`icon`/`mountBone`/`focusBone`/`renderMode`/`withdrawAmmoOnChange`）、`AttachmentMountBone`（`Fixed`/`FromDefinition`/`GunModel`）、`AttachmentRenderMode`（`CUSTOM`/`GENERIC`）、`EDIT_ORDER`；`AttachmentDefinition` +`Mount`/`ConflictsWith`/`AllowSharedMount`；`AttachmentSlots.conflicts()` → `Attachment.conflict()`；`GunData.availableAttachments()` 按挂点 + 显式互斥过滤；`Attachment.cycle`/指令/补全自动跟着走 |
| 3 | **`AttachmentType.BAYONET` + 刺刀落地** | ✅ | 枚举 +`Bayonet`；`ModItems.BAYONET_M_9`；`data/superbwarfare/sbw/attachments/bayonet_m_9.json`；bedrock 模型/贴图（需求方提供）+ `textures/item/bayonet_m_9.png`；`Model`/`Texture`/`Modifiers`/`Override` 齐备；tag + datagen（`attachment/bayonet{,/common}`）；`en_us`/`zh_cn` 语言；渲染见 #6 |
| 4 | **刺刀属性 + 动作表** | ✅ | `bayonet_m_9.json`：`Modifiers`（`MeleeDamage ×1.3` / `MeleeRange +1.2` / `Weight +0.4`）**与** `Override.MeleeActions`（单段突刺）并存，分工见 §11.5.3-② |
| 5 | **动画候选链 + 短名拼接** | ✅ | `MeleeAction.Animation` → `SingleOrList<String>?`；`resource/gun/GunAnimationNames.kt`；`GeoGunAnimationInstance.resolveMeleeName()` 按链解析、失败日志去重；`/sbw melee actions` 打印候选→实际名字。刺刀写 `["hit_bayonet", "hit"]`，动画做出来之前自动落在枪自己的 `hit` 上 |
| 6 | **渲染：`bayonet_pos` + 注册表分派** | ✅ | `GeoGunRenderer.renderRegisteredAttachments()`（`renderMode = GENERIC` 的槽位走通用路径，骨骼按 `AttachmentMountBone` 解析）；`ak_47.geo.json` 的 `bayonet_pos` 由需求方添加；`attachmentFocusBone` 改由注册表的 `focusBone` 提供 |
| 7 | 配件自带动画文件（§9.7 二期路线） | ❌ **未做** | 动画尚未制作，按需求留待后续 |
| — | 改装界面 / HUD | ⛔ **未动** | `WeaponEditScreen.kt` 与改动前一致；未新增 GUI 图标资源 |

#### 11.5.1 新增一个槽位类型要改哪里（注册表化的回报）

按顺序只有 6 步，其中 3 步已经有自动化：

| 步骤 | 位置 | 是否自动 |
|---|---|---|
| ① 加枚举常量 | `AttachmentType.kt` | 手写 |
| ② 登记一条元数据 | `AttachmentSlots.ALL`（`mount`/`tagBucket`/`icon`/`mountBone`/`focusBone`/`renderMode`） | 手写（**新增槽位默认 `renderMode = GENERIC`，不用写渲染代码**） |
| ③ 注册物品 | `ModItems.registerAttachment("xxx")` | 手写 |
| ④ 物品 tag（`attachment/<桶>`、`attachment/<桶>/<稀有度>`、可研究汇总） | `ModTags` + `ModItemTagProvider` | **自动**：`ModTags.Items.ATTACHMENT_BY_SLOT` / `attachmentRarityTag()` 与 datagen 的循环都从注册表读；只要把新物品追加到 `ModItemTagProvider.attachmentItemsBySlot()` 对应槽位的列表 |
| ⑤ 报文下标 / 调试聚焦 | `EditMessage`、`GeoGunRenderer.attachmentFocusBone` | **自动**：都从 `AttachmentSlots.EDIT_ORDER` / `slot.focusBone` 读。**例外**：改装界面不改（§11.5.3-⑤），所以追加在 `EDIT_ORDER` 末尾的槽位暂时没有按钮 |
| ⑥ 数据 / 模型 / 贴图 / 语言 | `sbw/attachments/<id>.json`、`models/bedrock/attachment/*.geo.json`、`lang/en_us|zh_cn` | 手写（`slot` 枚举名、`attachment.superbwarfare.slot.<小写槽位名>`、`item.superbwarfare.<id>`） |

#### 11.5.2 数据侧新增字段

```jsonc
// sbw/attachments/<id>.json
{
  "Slot": "Bayonet",           // 必填，槽位枚举名
  "Mount": "muzzle_lug",       // 可选：覆盖槽位默认挂点组
  "AllowSharedMount": false,   // 可选：允许与同一挂点上的其它配件共存（转接座用）
  "Bone": "bayonet_pos",       // 只有 FromDefinition 的槽位才吃它
  "Modifiers": [ ... ], "Override": { ... },
  "Model": "...", "Texture": "..."   // GENERIC 槽位必填，缺了会在日志里 warning
}
```

`DataValidator` 新增 `validateAttachmentData`（§10 的二期部分）：槽位必须登记进注册表（**致命**）、
`GENERIC` 槽位缺 `Model`/`Texture`、`Bone` 写在 `Fixed` 槽位上（被忽略）、`Override` 里出现未注册的枪械属性名
（宽松解析会静默忽略）——后三类是 warning。
**不做**骨骼存在性校验：那要读客户端模型，属资源侧。

#### 11.5.3 与设计稿不一致 / 踩到的地方

**① 动作表里的动画名只能写短名：候选链 + `animation.<枪 id>.` 拼接（二期新增）。**
动作表住在**枪械数据**里，而一份数据会被多把枪共用（配件/弹种/开火模式都能覆盖它），
所以它**写不了某一把枪的完整 clip 名**——`animation.ak_47.hit` 里的 `ak_47` 只有运行时才知道
（`GunResource` 本来就是按物品注册 id 缓存的）。刺刀这种"一把配件装在多把枪上"的场景，
写死任何一把枪的名字都是错的。

于是 `MeleeAction.Animation` 从 `String?` 改成 **`SingleOrList<String>?`（候选链）**，
解析规则收在 `GunAnimationNames` 里：

| 写法 | 解析结果 |
|---|---|
| `"animation.ak_47.hit"` | 全名，原样使用（**现有数据全是这种，行为不变**） |
| `"hit"` | 拼成 `animation.<宿主枪 id>.hit` |
| `["hit_bayonet", "hit"]` | 按顺序取**第一个存在**的：有 `hit_bayonet` 就用它，没有就用 `hit` |
| 不写 | `GunAnimation.Melee[idx % size]`（旧行为） |

- **短名拼接只用于 `MeleeActions.Animation`**：`GunAnimation.*`（Idle/Fire/Reload/Melee…）仍然写全名
  （资源文件里本来就在同一把枪的上下文里，没有拼接的必要）。
- 判定依据是 `animations.containsKey(...)`（该枪的动画文件里有没有这支 clip），
  已核对**全部 38 把**带 `Animation` 块的枪都是 `animation.<物品注册 id>.` 前缀，缩写的短路规则不会误伤。
- 刺刀因此写成 `["hit_bayonet", "hit"]`：**现在**（还没做刺刀动画）自动落在 `hit` 上，
  **将来**只要往各枪的动画文件里加 `animation.<枪>.hit_bayonet`，同一份配件数据不用改就生效。
- `"hit_lr"` 这种字符串简写（`StringOrObjectFactory`）也走同一条链，即 `"MeleeActions": ["hit"]`
  等于 `animation.<枪>.hit`。
- 失败日志：候选全落空打一条 error 并回退 `GunAnimation.Melee[0]`；**同一条失败只打一次**
  （runner 为空时这个解析每个 tick 都会跑一遍，不去重会刷屏）。
- `/sbw melee actions` 现在打印"候选 → 拼出来的名字"，调动画时不用猜。

**② 刺刀 = 属性 + 动作表并存。**
`Modifiers` 与 `Override` 作用在不同层级（前者改标量、后者整块替换 `MeleeActions`），可以同时写：

```jsonc
"Modifiers": [                                    // 工具提示里看得见的那部分
  { "Prop": "MeleeDamage", "Op": "Mul", "Value": 1.3 },
  { "Prop": "MeleeRange",  "Op": "Add", "Value": 1.2 },
  { "Prop": "Weight",      "Op": "Add", "Value": 0.4 }
],
"Override": {
  "MeleeActions": [{                              // 形状与手感
    "Animation": ["hit_bayonet", "hit"],
    "Duration": 16, "HitTime": 6,                 // 与枪自己的挥击一致：动画补上之前手感零变化
    "Hitbox": { "Type": "Capsule", "Range": 3.3, "Radius": 0.45, "Occlusion": true },
    "Sweep": { "From": 0, "To": 0 },
    "MaxTargets": 8, "Knockback": 0.3
  }]
}
```

三条注意事项：

1. **伤害别两处都乘**：`DamageMultiplier` 乘在 PMC 解析后的 `MeleeDamage` 上（已经含配件的 ×1.3）。
   标量统一放 `Modifiers`（工具提示才显示得出来），动作表只管形状/时长/击退。
2. **动作表是整段替换**：`MaxTargets`/`Falloff`/`SortBy`/`Knockback`/`Headshot`/`Legshot` **不继承**
   枪自己的动作（只有 `Hitbox`/`Sweep`/`Duration`/`HitTime`/`Damage` 有缺省继承），
   所以刺刀里显式写回了 `MaxTargets: 8` / `Knockback: 0.3`，否则击退会从 0.3 掉成 0。
3. **胶囊的长度会叠加 `MeleeRange`**（见 ③）：`Range: 3.3` + 配件 `+1.2` = 实际 4.5 格。
   别再写 `Range: 4.5` 又加 `+1.2`（那会变 5.7）——**挑一种表达**。

AK-47 装上刺刀后的实际效果：伤害 15 → 19.5；判定变成一根 **4.5 格长的细胶囊**
（`Range 3.3 + MeleeRange 1.2`，半径 0.45），对比枪托砸是 4.2 格远的 100° 宽锥；
出伤 tick 与动画时长不变（动画补上后再一起调）。

**③ ⚠ `MeleeRange` 改成"叠加"，不是"兜底"（二期唯一的判定口径改动）。**
设计稿 §3.2 写的是"`MeleeRange` = 额外距离（叠加 `player.getEntityReach()`）"，
但一期把它实现成了 `MeleeHitbox.Range` 的**兜底值**（`rangeOr(defaultRange)`）——
于是 AK-47 这种显式写了 `"Range": 1.2` 的枪，`MeleeRange` 属性**完全不起作用**，
配件也就没法用 `Modifiers` 加近战距离。

二期改成 `resolvedHitbox.rangeOr(0.0) + defaultRange`（`MeleeAction.resolve`）：

| 情况 | 改前 | 改后 |
|---|---|---|
| 写了 `Range`、没写 `MeleeRange`（除下面那 2 处以外**全部**枪） | `Range` | `Range + 0` = **不变** |
| 既没写 `Range` 也没写 `MeleeRange`（绝大多数枪） | `0` | `0 + 0` = **不变** |
| 没写 `Range`、写了 `MeleeRange`（`rpg.json`、`secondary_cataclysm.json` 各 1 处） | `MeleeRange` | `0 + MeleeRange` = **不变** |
| 写了 `Range` 且有配件加 `MeleeRange`（二期新增场景） | 忽略 | **`Range + MeleeRange`** |

也就是说：**旧数据逐值等价**，换来的是
"配件加近战距离 = `Modifiers` 里加一条 `MeleeRange`"，不必再让配件去整块覆盖枪的 `MeleeHitbox`。
判定总距离仍是 `Range + MeleeRange + player.getEntityReach()`；`Capsule` 的线段长度是
`Range + MeleeRange`（它不吃 `getEntityReach()`），这点没变。

**④ 挂点组默认互不冲突，而且不改改装界面。**
5 个既有槽位各自登记了独立的 `mount`（`scope_rail` / `magazine_well` / `muzzle_device` / `stock_interface` /
`grip_rail`），所以**现有行为零变化**；刺刀登记在 `muzzle_lug`，与 `muzzle_device`（枪口配件）不同组，
**可以共存**（§8.2 的定稿）。握把与将来的下挂（`underbarrel_rail`）物理上是同一根下导轨，但本期
**没有**合并挂点组——那会让"装了垂直握把就装不了下挂榴弹"，属于玩法改动，等三期落地下挂时再定。

**⑤ 改装界面完全没动，`EDIT_ORDER` 只是"报文下标 ↔ 槽位"的唯一一份定义。**
界面里 6 个按钮的固定顺序（枪口/瞄具/握把/枪托/弹匣/弹种）与 `EDIT_ORDER` 前 6 项一致，不允许改序；
刺刀追加在下标 6，**当前界面没有它的按钮**，用
`/sbw attachment <entity> set Bayonet superbwarfare:bayonet_m_9` 安装。
重写界面时按 `EDIT_ORDER.chunked(2)` 布局即可自动带上新槽位。
`EditMessage` 里原来的 `when (type) { 0 -> ... 5 -> ... }` 换成了 `EDIT_ORDER[type]`，
挂点互斥报错也加了独立文案（`commands.superbwarfare.attachment.fail.mount`）。

**⑥ 标签桶全部自动化，生成结果只有"新增"与"重排"。**
`ModTags.Items` 里原来 30 个手写常量（`ATTACHMENT_SCOPE*` 等）换成
`ATTACHMENT_BY_SLOT` / `ATTACHMENT_RARITY_SUFFIXES` / `attachmentRarityTag()`，
`ModItemTagProvider.addAttachmentTags()` 从 ~200 行硬编码变成对注册表的循环。
`runData` 后生成的 `tags/items/attachment/**` 与改动前**逐项等价**，
差别只有：新增 `attachment/bayonet{,/common}.json`，以及 `attachment/researchable/*.json` 与
`attachment.json` 里子 tag 的**排列顺序**（tag 是无序集合，无实际影响）。

**⑦ 注册表读取在"整局游戏"里是 fail-fast，在数据包侧是 fail-soft。**
`AttachmentSlots.of()` 对未登记的枚举常量直接抛异常（新增枚举忘了登记 -> 立刻发现），
数据校验/渲染这类只读路径走 `ofOrNull()`，不让数据包把游戏炸掉。

#### 11.5.4 二期验收步骤（手动）

```
1. 拿一把 AK-47（模型里有 bayonet_pos 骨骼）
2. /sbw attachment @s set Bayonet superbwarfare:bayonet_m_9   → 枪口出现刺刀，与消音器共存
3. 按 V          → 伤害 19.5、判定变成 4.5 格的细胶囊（更远、更窄）
                   开 melee_debug_log 看 `reach` 与命中日志；/sbw melee actions 看候选链
4. 动画：枪的动画文件里**没有** hit_bayonet → 自动播它自己的 `hit`（即第 3 步看到的就是这个）；
   往 ak_47.animation.json 里加一支 `animation.ak_47.hit_bayonet` 后**不改任何数据**再挥一次 → 应该改播新动画
5. 装消音器后再装刺刀 → 两个都在（不同挂点组）；先装刺刀再装消音器 → 同样都成功
6. /sbw attachment @s info（或 clear Bayonet）→ 槽位枚举里出现 Bayonet
7. DataValidator（开发环境默认开）→ 启动日志里附件数据无 error，无 "Bone is ignored" 之类 warning
8. 改装界面（EDIT_MODE 键）→ 与原版一致：没有刺刀按钮，其余槽位行为不变
9. 回归：`rpg` / `secondary_cataclysm` 的近战距离仍是 1（`MeleeRange` 语义改动后逐值等价）；
   其余枪不写 `Animation` / 写全名 `animation.<枪>.hit` 的行为不变
```

### 11.6 三期：`SubWeapon` 体系
1. `SubWeaponInfo` POJO + `AttachmentDefinition.SubWeapon` 字段 + `DataValidator` 校验（含"默认取物品 id"的解析检查）。
2. `SubWeaponItem : GunItem, AttachmentProvider` + `ModItems.registerSubWeapon` + 物品模型。
3. **手持行为排除清单**（§8.3.1）：先在 `GunItem` 上加 `useAsWeaponInHand()` + 静态 `isHeldWeapon(stack)`，再逐组替换门禁点（A 六个 Mixin → B 客户端输入 → C HUD → D 物品自身行为 → E 列表/工具类）。
4. `SubWeaponRuntime`：合成栈 + 强引用缓存 + 数据解析 + **主武器 tick 里顺带 tick** + 缓存清理（§9.3 的五条风险逐条验证）。
5. `SubWeaponFireMessage` + 服务端 `GunData.shoot(...)` 链路 + 客户端 `handleSubWeapon` + **遍历逐个触发**（§9.4）。
6. `AttachmentType.UNDERBARREL` + GP-25 落地（`sbw/attachments/gp25.json` + `sbw/guns/gp25.json` 成对）。
7. 副武器空仓按 G 装填（§9.6）+ 冷却。
8. 近战副武器形态（Data 写 `@melee`）留给数据包验证，代码不再额外支持。

> 一期是纯近战本体重构（不含配件），二期、三期互不依赖。
> 三期开工时：`registerSubWeapon(id, rarity) = registerAttachment(id, rarity, ::SubWeaponItem)`
> 已经在二期备好工厂参数，`AttachmentType.UNDERBARREL` 只差枚举 + 注册表登记一条
> （按 §11.5.1 的 6 步走）。

### 11.7 近战手感调整（二期后续，✅ 已实现）

用户实测后提的五条，全部落地：

| # | 项 | 做法 |
|---|---|---|
| 1 | **判定体统一成长方体** | `MeleeHitbox.Type` 默认值 `CONE → BOX`（尺寸 1.8×1.8、`YOffset -0.2`）；删掉 `Length` 字段，三种形状的前向长度统一为 `reach`（§11.7-①） |
| 2 | **`MeleeHeadshot` / `MeleeLegshot`** | 新增两个**近战专用**全局属性（默认 2.0 / 0.5），不再借用投射物的 `Headshot`（§11.7-③） |
| 3 | **动作表只给倍率** | `MeleeAction.Damage` 删除；`DamageMultiplier` / `RangeMultiplier` 分别乘在 `MeleeDamage` 与 `Range + MeleeRange` 上（§11.7-②） |
| 4 | **爆头只认准星目标** | 客户端沿视线射线取准星实体，报文里带 `aimed`；服务端只在 `aimed` 时才算爆头（§11.7-④） |
| 5 | **刺刀与枪口配件互斥** | `AttachmentType.BAYONET` 的挂点组从 `muzzle_lug` 改成与 `BARREL` 相同的 `muzzle_device`（§11.7-⑤） |

#### 11.7.1 判定形状与距离的最终模型

```
reach = (MeleeHitbox.Range + MeleeRange) × 动作的 RangeMultiplier + player.getEntityReach()
```

三种形状的前向长度**都是**这个 `reach`：

- `Box`：半长 = `(Width/2, Height/2, reach/2)`，中心 = 眼睛 + 局部上偏移 `YOffset` + 视线 × (`ZFrom` + `reach/2`)
- `Cone`：`|Δyaw| ≤ Angle/2`、`|Δpitch| ≤ Pitch/2`、距离 ≤ `reach`
- `Capsule`：线段 `ZFrom → ZFrom + reach`，到目标 AABB 最近距离 ≤ `Radius`

改动带来的直接结果：

- **盒子没有 `Length` 字段了**：枪的 `MeleeRange`、配件的距离加成、动作的 `RangeMultiplier`
  都直接作用在长度上，不用每个形状各写一套尺寸（旧 `"Length"` 会变成未知键，DataValidator 会报错）。
- **`Capsule` 不再吃掉 `getEntityReach()`**：旧实现里胶囊的长度是"绝对值"（`Range: 3.3` 就是 3.3 格，
  与实体触及距离无关），和 `Cone`/`Box` 两套口径；现在统一。
- **旧枪零改动**：22 把近战枪里只有 AK-47 写了 `MeleeHitbox`，其余全靠默认值 ——
  默认形状一改，它们**自动**从圆锥变成 1.8×1.8×`reach` 的长方体。
- `MeleeAngle` 只对显式 `"Type": "Cone"` 生效；20 把枪里留着的 `"MeleeAngle": 100` 现在是**无作用的遗留键**
  （不影响校验，随时可删）。

**`Cone` 的调试线框原来确实是坏的**（用户实测反馈），一并修掉：

1. 判定本身有**俯仰符号错误**：`targetPitch = asin(delta.y / len)` 是"向上为正"的仰角，
   而 MC 的 `pitch` 是"向下为正"，两者直接相减 → 抬头/低头时平白多出一个夹角偏差
   （`Pitch: 180` 不受限时被掩盖，只有显式写小 `Pitch` 才暴露）。已改成取负。
2. 线框画的是"垂直于视线的平面圆环"（半径 `reach × sin(半角)`），和真实判定体
   （贴着半径 `reach` 的**球面**、角空间里是个矩形窗口）完全不是一回事。
   现在 `MeleeQuery.coneDebugShape` 在角空间采样四条边、投到球面上，渲染侧只负责连线。

#### 11.7.2 动作表：伤害与距离改成倍率

```jsonc
// 之前：动作可以写绝对伤害，还能自己写一个 Hitbox.Range 当绝对距离
{ "Damage": 19, "Hitbox": { "Type": "Capsule", "Range": 3.2 } }

// 现在：数值只有一个出处（枪的属性），动作只回答"这一段比别的段重多少、伸多远"
{ "DamageMultiplier": 1.25, "RangeMultiplier": 1.3, "Hitbox": { "Type": "Box", "Width": 0.9 } }
```

- `MeleeAction.Damage` **删除**（不是弃用）：数值唯一出处是枪的 `MeleeDamage`，
  配件改数值走 `Modifiers`，动作改手感走倍率，两边不再打架。
- `RangeMultiplier` 只缩放 `Range + MeleeRange`，**不缩放** `player.getEntityReach()`
  （那部分是玩家属性，不该被动作放大）。
- `MeleeAction.Headshot` / `Legshot` 仍是**绝对值覆盖**（它们本身就是倍率，不参与"倍率的倍率"），
  不写时取枪的 `MeleeHeadshot` / `MeleeLegshot`。

#### 11.7.3 打头/打腿倍率独立

| 属性 | 默认 | 说明 |
|---|---|---|
| `MeleeHeadshot` | 2.0 | 近战打头倍率 |
| `MeleeLegshot` | 0.5 | 近战打腿倍率 |

不再复用投射物的 `Headshot`：一把枪"子弹爆头 3 倍"和"枪托砸头 2 倍"本来就是两回事，
混用会让改投射物数值时**静默**改掉近战手感。

> **迁移提示**：默认值取的是 2.0（近战枪里最常见的投射物 `Headshot`）。
> 原来靠投射物 `Headshot` 吃近战的枪里，`awm`/`hunting_rifle`/`k_98`/`mosin_nagant`（3）、
> `mk_14`/`marlin`/`svd`（2.5）现在是 2.0；`aa_12`/`m_1897`/`mp_5`/`m_870`（1.5）现在是 2.0。
> 想保持原样的枪自己写一行 `"MeleeHeadshot": <原值>` 即可。`rpg` 已显式写 `"MeleeHeadshot": 1`
> （它原本 `Headshot: 0`，即"火箭筒砸人不算爆头"）。

#### 11.7.4 爆头只给准星正对的那一个

横扫一次能打到好几个目标，旧实现对**每个**目标独立判断"入射点是否落在头部高度"——
侧后方的敌人被盒子的边角蹭到头部也算爆头。

现在：

1. 客户端在判定前，从眼睛沿**当前视线**打一条射线（`MeleeQuery.crosshairTarget`）：
   先 `level.clip` 取方块命中点，再在候选实体里取"最先撞到的那个 AABB"（`AABB.clip`，膨胀 0.1），
   超出 `reach` 或被方块挡住就是 `null`；
2. `MeleeQuery.Hit` 带上 `aimed`（是否就是这个实体），调试日志里打 `AIM`；
3. 报文 `TargetPayload` 带上 `aimed`；
4. 服务端：`headshot = payload.aimed && isHeadshot(target, zonePos)` —— 几何阈值仍然由服务端算。

打腿没有这个限制（位置判定，所有命中目标都适用）。

> **⚠ 实测踩到的坑：命中区域量错了点，导致"瞄谁谁爆头"。**
> 一期把入射点定义成 `closestPointInBox(box, eyePos)`（眼睛到目标 AABB 的**最近点**），
> 而这个点的 y 会被**夹进目标的碰撞箱**：平地上站着打站着时，眼睛高度（1.62）本来就落在
> 目标 AABB 的 y 区间里，于是入射点恒等于眼睛高度 ——
> `isHeadshot` 的容差是 `eyeHeight ± (0.25/0.3)`，对人形目标**永远命中**，
> 打脚、打胸、打头都是爆头。（一期 `Pitch: 180` 掩盖了另一半问题，见 §11.7.1。）
>
> 现在命中区域量的是**准星射线到该目标 AABB 的最近点**（`Hit.zonePos`，
> `closestSegmentToBox(eyePos, eyePos + look × reach, box)`）：
> 直接瞄准它时就是射线进入碰撞箱的那一点，横扫蹭到时也忠实反映"准星在那个距离上的高度"。
> 判定体与碰撞箱的**接触点**（`Hit.hitPos`）仍然保留，但只用于遮挡判定与几何诊断 ——
> 遮挡必须按"这一刀从哪个方向来"算，不能按准星算。

#### 11.7.5 刺刀与枪口配件互斥

`AttachmentSlots` 里 `BAYONET` 的挂点组改成与 `BARREL` 相同的 `muzzle_device`：
两者**抢同一个枪口挂点**，装了其中一个就装不了另一个。
`AttachmentSlots.conflicts` → `Attachment.conflict` → `GunData.availableAttachments` 过滤 →
指令补全 / `Attachment.cycle` 全部自动跟着走；用 `/sbw attachment set` 硬装时会给一句专门的失败文案
（`commands.superbwarfare.attachment.fail.conflict`）。

> 同一个入口现在还负责**非传递**的显式互斥（副武器 ↔ 刺刀 / 握把，见 §11.8.4）。

> 数据侧除了挂点组，还要注意：**两个刺刀的 `MeleeActions` 覆盖的是整张动作表**，
> 所以 `MaxTargets`/`Knockback` 这些不继承枪自己的动作，得按需要显式写回（§11.5.3-②）。

#### 11.7.6 枪托近战距离上调 + 刺刀提供额外距离

`MeleeRange` 的**默认值从 0.0 提到 2.0**（`DefaultGunData.meleeRange`）：
这是所有枪"枪托砸"的基础触点，原来 0 意味着实际触及距离 = 只有玩家自己的 3.0 格实体触及距离
（和原版空手一样远），近战基本够不着。

生存模式下的实际触及距离（`(Range + MeleeRange) × RangeMultiplier + getEntityReach()`，
创造模式 `getEntityReach()` 还会 +3，所以不要用创造模式试手感）：

| 枪 | `Hitbox.Range` | `MeleeRange` | 触及距离 |
|---|---|---|---|
| 绝大多数枪（没写 `MeleeRange`） | 0（默认） | 2.0（默认） | **5.0** |
| AK-47（自己写了 `Range: 1.2`） | 1.2 | 2.0 | **6.2** |
| 任意枪 + 刺刀（`MeleeRange +1.2`） | 同上 | +1.2 | **6.2 / 7.4** |
| `rpg` / `secondary_cataclysm`（自己写了 `MeleeRange: 1`） | 0 | 1.0 | 4.0（**未变**） |

- 只有一个调节点：枪的 `MeleeRange`（全局默认在 `DefaultGunData`，枪可以单独写）与 `MeleeHitbox.Range`。
- 刺刀提供的额外距离就是它 `Modifiers` 里的 `{ "Prop": "MeleeRange", "Op": "Add", "Value": 1.2 }`，
  想让刺刀更长/更短改这一个数即可（改完所有能装刺刀的枪一起变）。
- `rpg` / `secondary_cataclysm` 显式写了 `MeleeRange: 1`，**比新默认还短**；
  想让它们再长一点，把那两行删掉（或改成 2）就行。

---

### 11.8 三期：`MeleeEffect` + `SubWeapon` ✅ 已实现

> **⚠ 四期修订提示**：下面 B 表的第 **6/7** 项在四期会被**替换**——
> #6 的 `SubWeaponFireMessage` + 服务端 `GunData.shoot(...)` 链路改成"副武器就是当前操控的枪，
> 直接走 `FireKeyMessage`"；#7 的"G 路由 + 冷却"改成"G = 切换"。A 表（`MeleeEffect`）完全不受影响。
> 详见 §9.8。B 表第 1/2/3/4/5/8/9/10/11 项的结论继续有效（模型/贴图可换、手持门禁、
> 装配与状态存储、槽位与挂点、校验、资源、调试命令）。

> **状态：✅ 已完成。** 两块内容各自独立：
> **(A) `MeleeEffect`**（§3.8 那 11 行行为表）+ RPG 的"近战概率爆炸"；
> **(B) `SubWeapon`**（§9 全套）+ 首个副武器 `gp_25`（下挂式单发榴弹发射器）。
> **副武器动画当时不做、HUD/改装界面不在本期**（按需求）：副武器复用主武器自己的开火链路音效，
> 界面按 §11.5.3-⑤ 的既有结论继续用指令安装。
> **（后续）§11.9 已补上副武器开火动画与枪口焰归属，并把 `SubWeaponInfo.Data` 真正接上。**

#### A. `MeleeEffect` —— 近战额外效果

| # | 项 | 落点 |
|---|---|---|
| 1 | 行为接口 + 上下文 | `melee/MeleeEffectBehavior.kt`（`MeleeEffectBehavior` / `MeleeEffectContext`） |
| 2 | 12 个首发行为 | `melee/MeleeEffectBehaviors.kt`：`explosion` / `extra_damage` / `shock` / `potion` / `ignite` / `knockback` / `lightning` / `heal` / `ammo_refund` / `screen_shake` / `sound` / `particle` |
| 3 | 行为注册表 | `init/ModMeleeEffects.kt`（id 归一化：小写、去 `superbwarfare:` 前缀） |
| 4 | 服务端结算器 | `melee/MeleeEffectDispatcher.kt`（`swing()` / `hit()` / `kill()`，概率、冷却、`FirstHit` 记账） |
| 5 | 数据侧预设表 | `CustomData.MELEE_EFFECTS`（目录 `sbw/melee_effects`，**不同步到客户端**） |
| 6 | 预设展开 | `MeleeEffectSpec.resolve()` → `ResolvedMeleeEffect`；结果挂在 `ResolvedMeleeAction.effects`（`by lazy`） |
| 7 | 数据接线 | `MeleeAction.Effects` 改成 `List<StringOrObject<MeleeEffectSpec>>?`（字符串简写可用）；`MeleeAction.resolve()` 透传 `effectSpecs` |
| 8 | 结算接入 | `MeleeAttackMessage`：`Swing` 在 `targets` 判断**之外**、`Hit`/`FirstHit` 在目标真正受伤之后、`Kill` 在目标死亡时 |
| 9 | 预设文件 | `warhead_stab`（爆炸）/ `heavy_impact`（击退+上挑）/ `concussive_strike`（感电）/ `blade_ignite`（点燃）/ `soul_drain`（吸血） |
| 10 | RPG 落地 | `rpg.json` 新增 `MeleeActions`（`Animation: "hit"`、`Duration 24`、`HitTime 10`、`RangeMultiplier 1.3`、`MaxTargets 1`、`Knockback 0.4`、`Cooldown 40`），效果是 `warhead_stab` 概率 **0.35**、冷却 **200** |
| 11 | 校验 | `DataValidator`：条目解析不出行为 = **致命**；数值越界与行为必填字段（`MeleeEffectBehavior.validate`）= 警告；预设文件自身也走同一条 |

#### B. `SubWeapon` —— 副武器

| # | 项 | 落点 |
|---|---|---|
| 1 | 定义 POJO | `data/attachment/SubWeaponInfo.kt`（`Data` / `AmmoSlot` / `Animation` / 两个换弹音效）+ `AttachmentDefinition.subWeapon`。`Animation` 在 §11.9 里改成了开火动画候选链 |
| 2 | 槽位 | `AttachmentType.SUBWEAPON`（`"SubWeapon"`）+ `AttachmentSlots.Bones.SUBWEAPON = "subweapon_pos"`，挂点组 `subweapon_rail`、`GENERIC` 渲染、追加在 `EDIT_ORDER` 末尾（下标 7，界面暂不出按钮） |
| 3 | 物品 | `item/attachment/SubWeaponItem.kt`（`GunItem` + `AttachmentProvider`，`useAsWeaponInHand() = false`、无耐久条、配件 tooltip）+ `ModItems.registerSubWeapon` + `ModItems.GP_25` |
| 4 | 手持门禁 | 六个 Mixin + `ClickEventHandler` / `ClientEventHandler` / `ClientMouseHandler` + 6 个 overlay/tooltip + `GunItem.inventoryTick`/`getAttributeModifiers`/`getItemScreen`，共 **50 处**从 `is GunItem` 换成 `GunItem.isHeldWeapon(stack)` |
| 5 | 运行时 | `subweapon/SubWeaponRuntime.kt`：合成栈 `ItemStack(item, 1, attachment.tag)`、`(枪 UUID, 槽位)` 缓存（换 tag 实例就重建）、`tick()` 挂在 `GunEventHandler.gunTickInternal` 的 `inMainHand` 分支 |
| 6 | 报文 | `network/message/send/SubWeaponFireMessage.kt`（`slots` 开火 / `reloadSlots` 尝试装填） |
| 7 | 客户端 G | `client/gun/SubWeaponClientHandler.kt` + `MeleeClientHandler.tick` 的 G 分支（没有副武器才落回近战） |
| 8 | 首个副武器 | `sbw/attachments/gp_25.json` + `sbw/guns/gp_25.json`（单发、`Magazine 1`、`RPM 60`、`grenade_40mm`、参考 `m_79` 的弹道与音效）；模型/贴图**复用 `steel_pipe_silencer`**（模型做好后换 `Model`/`Texture` 两行即可） |
| 9 | 校验 | `DataValidator.validateSubWeaponData`：`SubWeapon.Data`（含"默认取物品 id"）解析不到枪数据 = **致命**；物品不是 `SubWeaponItem` = 警告 |
| 10 | 资源 | 物品 tag（`ModItemTagProvider.attachmentItemsBySlot`）、datagen 物品模型（`ModItemModelProvider`）、`item.superbwarfare.gp_25` 与 `attachment.superbwarfare.slot.subweapon` 两条语言（`en_us` + `zh_cn`） |
| 11 | 调试 | `/sbw subweapon info [<entity>]`（`command/SubWeaponCommand.kt`）：槽位 / 配件 id / 枪数据 id / 弹药 / 射速 / 冷却 / `canShoot`；`/sbw melee actions` 现在会把每段的 `Effects` **逐条展开**打印（预设解析之后的行为/概率/触发时机/冷却） |

#### 11.8.1 与设计稿不一致 / 需要知道的地方

**① 槽位与挂点改名（需求方要求）**：`AttachmentType.SUBWEAPON`（不是 `UNDERBARREL`），
渲染挂点骨骼 `subweapon_pos`（不是配件自己的 `Bone`，所以配件 json 里**不要写 `Bone`**，
写了会被 `DataValidator` 提示忽略）。挂点组 `subweapon_rail` 与握把的 `grip_rail` **仍然分开**，
但**互斥关系改成显式声明**（`AttachmentSlot.conflictsWith`）：副武器与刺刀、副武器与握把互斥，
刺刀与握把仍可共存。为什么不直接把 `mount` 改成 `"grip_rail"`：挂点组是**传递**的等价关系，
合并之后就没法再表达"A 排斥 B、A 排斥 C，但 B 与 C 共存"了 —— 详见 §11.8.4。

**② `MeleeEffectSpec` 的所有字段都改成可空**：同一个类现在**两用** ——
既是 `MeleeActions[].Effects[]` 的条目，也是 `sbw/melee_effects/<id>.json` 预设文件的数据类。
只有可空才能表达"这一项我没写"，覆盖规则才是干净的**逐字段覆盖**
（条目写了用条目的 → 预设的 → 行为默认值）。
`Trigger` / `Chance` / `Cooldown` 因此也从"有默认值"变成"可空 + 在 `resolve()` 里补默认"。

**③ `Effects` 的元素类型是 `StringOrObject<MeleeEffectSpec>`**（不是裸 `MeleeEffectSpec`）：
文档里 `"Effects": ["superbwarfare:heavy_impact"]` 这种字符串简写才成立
（裸列表 + `JsonPrimitive` 会直接解析失败，而解析失败会**整个数据文件**被跳过并只留一条 error 日志）。
字符串简写会把 `Effect` 与 `Type` **同时**填上，`resolve()` 按"预设优先、行为兜底"解析 ——
所以 `"Effects": ["superbwarfare:explosion"]` 直接写行为 id 也能用。

**④ 概率 roll 与冷却写入的时机**：`Chance` 只在**服务端**用 `level.random` roll；
冷却**只在真的触发之后**才写进枪械 NBT（键 `effect:<key>`）——
语义是"发动过之后这段时间不再发动"，而不是"每 N tick 掷一次骰子"。
`FirstHit` 每次挥击只考虑一次（第一个真正受伤的目标）；`Hit` 每个受伤目标各一次；
`Kill` 在目标被这一击打死时；`Swing` 与是否命中无关（所以它在 `targets.isNotEmpty()` 判断之外）。

**⑤ `explosion` 默认不破坏方块、也不会炸到自己**：`DestroyBlocks` 缺省 **false**。
`CustomExplosion` 用 `level.getEntities(directSource, aabb)` 取目标，而 `Builder` 的 `directSource`
就是攻击者 —— 攻击者**天然不在伤害列表里**，所以贴脸戳爆不会被自己的爆炸炸到，不需要额外豁免。
另外爆炸伤害带冲击波延迟（按距离最多 100 tick），所以 `Kill` 触发不会由爆炸补刀产生。

**⑥ 新增 `Amplitude` 字段**：`screen_shake` 需要"时间 / 半径 / 幅度"三个独立数值，
设计稿的字段表里只有 `Duration` 与 `Radius`，于是补了一个 `Amplitude`。
其它字段的复用约定（`Damage`/`Count`/`Extra`…）写在 `MeleeEffectBehaviors` 的类注释里。

**⑦ 副武器的弹匣天然独立，`AmmoSlot` 目前不影响开火**：
副武器的弹药/热量/换弹/耐久全部写在自己合成栈的 tag 上，而那个 tag 就是**主武器 NBT 里的附件子 tag**，
与主武器天然隔离。`SubWeaponInfo.AmmoSlot` 因此目前只影响"切换弹种时弹药的搬运槽位"
（`GunData` 里 `ammoSlot` 的唯一用途），不参与开火 —— 这一点与设计稿 §9.3 的措辞略有出入，
**以代码为准**。

**⑧ 副武器只推进服务端状态**：`SubWeaponRuntime.tick` 在客户端直接返回
（客户端的副武器状态靠主武器 tag 同步过来，自己再推会打架）。
另外它**不会**为"主武器本身也是副武器"的栈递归（否则一把副武器上再装副武器会无限递归）。

**⑨ 合成栈的 tag 必须来自 `getOrCreateTag`，缓存按 tag 引用校验**：`GunData.rebind` 是 `clearTag + merge`，
被清空的键会以 `tag.copy()` 重新落进去（`CompoundTag.merge` 对"原来不存在"的键是复制），
所以客户端每次 resync 之后附件子 tag 都是**新实例**。缓存必须按引用校验并重建，
否则客户端会一直读一份已经和主武器脱钩的旧 tag。

> **⚠ 实现期间踩到的坑（调试了三轮才定位，症状是"按 G 完全没反应、开火和换弹都不走"）**
>
> 合成栈的根 tag 必须满足两个条件，缺一个都会坏：
>
> **(1) 必须是主武器 NBT 里那份 tag 的活引用 → 用 `Attachment.getOrCreateTag(slot)`，不能用 `AttachmentInstance.tag`。**
> `AttachmentInstance.tag` 来自 `Attachment.getTag()`，它对**字符串形式**的槽位内容返回的是
> `CompoundTag().apply { putString("Id", ...) }` —— **每次调用都是一个新的游离 compound**。
> `getOrCreateTag` 会把该槽位**实体化成 compound 并写回枪 NBT**（`Id` 原样保留，
> `id()` / `getTag()` 语义不变，是无损且幂等的迁移），之后 `getCompound` 返回的就是同一个活引用。
>
> **(2) `ItemStack` 构造完之后，根 tag 必须仍然是那一份引用 —— 不能假设构造器会原样持有。**
> 实测（服务端日志里 `getOrCreateTag` 的引用哈希每 tick 都不变，而缓存依旧每 tick 未命中）
> 说明 `ItemStack(item, 1, tag)` 拿回来的 `stack.tag` **不是**传进去的那个对象。
> 而 `GunData` 在构造时会把根 tag 与 `gunDataTag`/`perkTag`/`attachmentTag` 全部**捕获成 `val`** ——
> 一旦栈里挂的是副本，副武器的弹药/换弹计时器/revision 就全部写进一个**和主武器 NBT 无关的角落**，
> 于是"换弹启动了、`time` 停在 44 再也不动、下次读又是 0、`canShoot` 永远 false"。
> 所以装配时显式补一刀：
> ```kotlin
> val stack = ItemStack(item, 1, liveTag)
> if (stack.tag !== liveTag) stack.tag = liveTag      // 构造器没原样持有就覆盖回去
> ```
>
> **(3) 缓存命中判定要比较我们自己存下的 `liveTag` 引用，而不是 `stack.tag` 反查。**
> 反查会把 (2) 的问题放大成"每 tick 重建一个 `GunData`"，
> 而 `GunData.state` 是"解码一次就缓存"的镜像，多个实例会互相把对方的改动覆盖回去。
> 现在 `Instance` 直接把 `liveTag` 存下来参与比较。
>
> **顺带修掉的第四个坑**：`SubWeaponRuntime.tick` 原来挂在 `if (inMainHand)` 里。
> 推进副武器只要求"主武器正在被 tick"，与它是不是主手无关；绑在 `inMainHand` 上时，
> 只要那个判定为假，副武器的状态机就会被**整段冻住**（症状同样是换弹计时器不递减）。
> 现在它挂在 `data.item.tick(...)` 之后，**整段 tick 不受 `inMainHand` 约束**
> （热量/冷却/perk/计时器照常推进），并带一个 `attachmentTag.isEmpty` 的便宜前置过滤
> （没装配件的枪直接跳过装配流程）。
> **（后续）装填与栓动的进度是例外**：它们只在主手拿着这把枪时推进，且切走时直接中断装填
> ——见 §11.9-E。`inMainHand` 现在原样传给副武器的 `gunTick`，只用来管这两件事和自动装填/提示。

**⑩ G 键的三种归宿**（`SubWeaponClientHandler.tryTrigger` 的返回值就是全部语义）：
主武器上没有副武器 → 返回 `false`，G 落回近战入口（等同 V）；
有副武器且至少有一个槽位可操作 → 进 `SubWeaponFireMessage.slots` 并占用动作锁 `SUB_WEAPON`
（时长取所有触发者里最长的：能开火按射击周期、空仓按它自己的换弹时间）；
有副武器但一个都动不了（全部在冷却 / 正在装填 / 没弹药可装）→ 吞掉这次 G
并**每 `MIN_RELOAD_LOCK_TICKS` 播一声 `trigger_click`**，**不会**落到近战。

**⑫ 「开火还是装填」由服务端一个人决定（实现期间修掉的坑）**：
最初的实现是客户端先判 `canShoot`，把槽位分进"开火列表"或"装填列表"，服务端再各判一次。
只要两边时序不一致（客户端的副武器状态是同步过来的，永远慢一拍），就会出现
**服务端 `canShoot` 为 false → 直接跳过 → 一枪不放，而客户端已经把冷却预写下去 →
后续 G 全部被"冷却中"吞掉**的永久静默。
现在报文只表达意图（`这次 G 请操作这些槽位`），服务端一次判定"能开火就开火、否则试装填"，
**冷却也完全由服务端写**（客户端只读）——单一事实来源，这一类死角在结构上就不存在了。
顺带补了两处"静默变有声"：客户端在"打不出去且背包里没有它要的弹药"时给一声 `trigger_click`；
`melee_debug_log` 打开后，服务端每个被跳过的槽位都会打一条 `[SubWeapon]` 日志说明原因。

**⑪ 副武器当时不做动画、HUD 一行未动**（按需求）：`SubWeaponInfo.Animation` 当时保留但不读取
（**§11.9 已接上**）；副武器开火复用主武器链路（`GunData.shoot` → `GunItem.shootBullet`），
所以弹道/伤害/音效完全由 `sub_weapon_gp_25.json` 决定，没有新的 HUD 代码。

**⑬ 为什么不需要"副武器专属的开火/换弹链路"**：
`GunItem.shoot(data, shooter, …)` 与 `tryStartReload(shooter, data)` 都是**纯 `GunData` 驱动**的
（`GunItem` 里唯一读 `mainHandItem` 的地方是 `inventoryTick`，用来判断"在不在主手"），
而副武器的弹药 / 热量 / 栓动 / 换弹 / Perk **全都住在它自己那份 `GunData`** 里
（那份数据寄存在主武器 NBT 的附件子 tag 上，与主武器天然隔离）。
所以三期真正新增的只有三件事：**装配**（`SubWeaponRuntime`）、**顺手 tick 它**、**G 的路由与冷却**。
`SubWeaponInfo.AmmoSlot` 也因此不影响开火（见 ⑦）。
装一把副武器到某把枪上，只要在枪数据的 `AvailableAttachments` 里加一条
`"SubWeapon": ["superbwarfare:sub_weapon_gp_25"]`（键就是 `AttachmentType.SUBWEAPON.attachmentName`），
再用 `/sbw attachment @s set SubWeapon superbwarfare:sub_weapon_gp_25` 装上即可 —— **不需要改任何代码**。

**⑭ 副武器的 id、自动装填与开火抖动（验收后按需求调整）**

| 项 | 结论 |
|---|---|
| **配件 id** | `gp_25` → **`sub_weapon_gp_25`**：物品注册 id 同时是配件数据 id 与枪数据 id，所以 `sbw/attachments/sub_weapon_gp_25.json`、`sbw/guns/sub_weapon_gp_25.json`、`textures/item/sub_weapon_gp_25.png` 三者必须同名。**旧的 `gp_25` 附件定义会失效，已装过的枪要重新装一次。** bedrock 模型/贴图（`models/bedrock/attachment/gp_25.geo.json` 等）是配件 json 里显式写路径的，**保持原名不动** |
| **挂点骨骼** | 约定骨骼常量改成 **`sub_weapon_pos`**（与枪模型里的骨骼名一致），槽位的 `mountBone` 从 `Fixed` 改成 **`FromDefinition`**：优先用配件自己声明的 `Bone`，没写才退回约定骨骼。`Fixed` 会**静默忽略**配件里的 `Bone`，骨骼名差一个字符就什么都不渲染且**没有任何报错**，这个坑不值得再踩第二次 |
| **自动装填** | **G 只负责开火**：打空了按 G 不会有动作，装填由服务端在 `SubWeaponRuntime.tick` 里自动做（判定就是 `GunData.shouldStartReloading`，与主武器 `autoReload` 同一个谓词；**在 `gunTick` 之前调用**，这样同一 tick 内状态就能切到 RELOADING）。客户端不再发"请装填"的请求。**装填进度只在持有主武器时推进，切走即中断、切回来从头装**（§11.9-E） |
| **装填提示** | 装填期间给玩家发**动作栏文字**（`info.superbwarfare.subweapon.reloading`，带槽位名与百分比），每 4 tick 刷一次。**标记为临时方案** —— 需求方之后会换成 HUD，所以逻辑收在 `SubWeaponRuntime.showReloadingProgress` 一处，换 HUD 时删掉它即可 |
| **开火抖动** | 开火后显式调一次 `subWeapon.shakePlayers(player)`，幅度由副武器**自己的数据**决定（`"ShootShake": [半径, 时长, 幅度]`，三项都 > 0 才生效）。主武器的 `GunItem` 里那一行是注释掉的，载具武器也是各自显式调用，所以这里必须自己调 |
| **换弹音效的归属** | **主武器的换弹音效是动画关键帧发的**，配件没有动画 —— 所以副武器的换弹音效由**配件数据 + 服务端**自己负责：`SubWeaponInfo.ReloadSound` / `ReloadEndSound` 由 `SubWeaponRuntime` 在状态跳变时用 `playLocalSound` 播给射手（⚠ 这个跳变必须在**复用的实例**上判断，否则每次状态重建都会再响一遍，见 §11.8.3）。**开火 1P 音**同样由服务端在真的开火之后用 `playLocalSound` 播，参数来自 `GunItem.resolveFire1PSounds`（与主武器同一份口径）；服务端另外只负责 `Fire3P` / `Far` / `VeryFar` |
| **扣扳机方式** | 当时是**半自动**：只认 G 的上升沿，按住不放不会再打第二发，但整段按住期间 G 都被副武器吞掉、不会掉到近战入口。边沿检测放在 `MeleeClientHandler.tick` 的**最开头**（任何提前 return 都不能跳过它，否则标记会卡在 `true`）。**§11.9-D 之后**：扣扳机方式由副武器自己的开火模式决定（`Semi` 仍是上升沿，`Auto` 按住连发，`Burst` 打完一轮） |
| **触发冷却** | **不在配件数据里配**：一律按副武器 `sbw/guns/<id>.json` 的 `RPM` 自动算（`1200 / RPM`），与主武器开火同一个口径 —— "这把武器多快"只在枪数据里写一次 |
| **装填的"忙"判定** | **不再自己拼判定**：自动装填走主武器的 `GunData.shouldStartReloading` → `GunEventHandler.tryStartReload`（它自带"正在装填 / 正在拉栓 / 计时器没归零 / 没有备弹 / 弹匣是满的"全部拒绝条件），所以"装填中再按 G 把计时器打回满值"在结构上不可能发生。早期版本自己拼 `busy = reloading() \|\| reload.time() > 0`，那只是在给"状态被另一个 `GunData` 覆盖"的症状打补丁 —— 根因见 §11.8.3 |

#### 11.8.2 三期验收步骤（手动）

```
1. 拿一把带 MeleeDamage 的枪，数据里给 MeleeActions[0].Effects 加
   { "Effect": "superbwarfare:heavy_impact", "Chance": 1.0 }
   → 打一下僵尸：被击退 + 上挑
2. rpg：装上任意弹药，贴脸对怪按 V
   → 播放 animation.rpg.hit；约 35% 概率在怪身上炸一发（半径 4、伤害 60、不破坏方块）
   → 连按 V：动作 Cooldown 40 生效（挥击间隔变长）；爆炸效果的 Cooldown 200 生效
     （melee_debug_log 打开后能看到 "effect 'superbwarfare:warhead_stab' skipped: cooling down"）
3. /sbw melee actions → 每段打印候选动画链，以及 `Effects` **展开后**的逐条明细（行为/概率/触发时机/冷却）
4. DataValidator（开发环境默认开）：启动日志里 sbw/melee_effects 与 sbw/guns 无 error
5. 给一把 AK-47（模型里有 subweapon_pos 骨骼）：
   /sbw attachment @s set SubWeapon superbwarfare:gp_25
   → 枪上出现钢管模型（复用 steel_pipe_silencer 的模型/贴图）
   → 按 G：打出一发 40mm 榴弹（与 m_79 同款弹道/音效）；弹匣空
   → 再按 G：尝试装填一次（背包里有 superbwarfare:grenade_40mm 时 45 tick 后装满）
   → 按 V：仍然是主武器自己的近战（副武器不抢 V）
   → 冷却期间按 G：一声 trigger_click，不挥刀
   → /sbw subweapon info：能看到槽位/枪数据 id/弹药/冷却/canShoot
6. 手持 gp_25 物品本身（创造模式物品栏里拿一个）：
   → 准心/弹药条/热量条都不显示，右键不开改装界面，移速不被 Weight 拖慢，没有耐久条
   → 左键不发射任何东西
7. /sbw attachment @s info → 槽位列表里出现 SubWeapon
8. 改装界面（EDIT_MODE 键）→ 与原版一致：没有副武器按钮，其余槽位行为不变
9. 回归：没装副武器的枪，G 仍然等同 V；22 把旧枪的近战手感不变
```

#### 11.8.3 验收后返修：开火音效 / 按住 G / 装填不可靠

> **状态：✅ 已完成。** 三个症状（开火音效不对、按住 G 音效一直响、装填走完却没装上）
> **根子是同一个**：副武器的 [Instance] 被反复重建 —— 每次重建都会换一份 `GunData`，
> 而两个 `GunData` 抢着写同一份 tag。

**症状与日志证据**（`run/logs/latest.log`，`melee_debug_log` 打开）：

```
20:01:38.755 [Server] reload started: SUBWEAPON
20:01:38.804 [Server] assembled SUBWEAPON -> ...: cached=true idMatch=true tagMatch=false
20:01:38.804 [Server] reload started: SUBWEAPON      ← 同一次装填又"开始"了一遍
20:01:39.604 [Server] assembled ... ; reload started  ← 4.3 秒里响了 12 次
20:02:02.953 [Server] reload finished: ammo=1/1
20:02:04.324 [Server] cannot shoot: state=EMPTY_RELOADING ammo=0/1   ← 装填"完成"后状态又回到空仓
```

| # | 症状 | 根因 | 修法 |
|---|---|---|---|
| 1 | 开火音效不对，听不到副武器 `SoundInfo.Fire1P` | 1P 音是**服务端**用 `playLocalSound`（`ClientboundSoundPacket`）补的，与主武器那条"客户端自己播"的链路不同源；而主武器的 3P / Far / VeryFar 在 `SoundRadius` 缺省（0）时是**静音**的，于是这一发只剩自动装填的换弹音 | 1P 音改到**客户端**播：口径收进 `GunItem.resolveFire1PSounds(data)`（主武器 `playGunClientSounds` 与副武器共用同一份参数）。服务端不再补 1P 音 |
| 1b | 没装填好（空仓/装填中）按 G 仍然响开火音 | 1P 音由**客户端预测**：客户端按自己那份同步过来的副武器状态判断"能不能开火"，而那份状态并不可靠（服务端明明在装填，客户端仍然认为能开火）→ 空响一发 | 改成**服务端拍板**：只有 `canShoot` 为真、这一发真的打出去之后，服务端才用 `player.playLocalSound`（`SoundTool` → `ClientboundSoundPacket`，只发给射手一个人）把 `GunItem.resolveFire1PSounds` 算好的音效播给射手。**没开火就绝不会响**，而音量/音高与主武器逐字一致（同一次函数调用算的参数）。换弹开始/完成音（`ReloadSound` / `ReloadEndSound`）也走同一个 `playLocalSound` |
| 1c | 客户端那份副武器状态可能是**装配那一刻**的快照 | `GunData.state` 是"构造时解码一次"的镜像；上一轮把实例改成"永远复用"之后，客户端再没人重新解码（旧实现靠每次重建顺带解码） | 新增 `GunData.pullFromTag()`（只重读状态，不合并 tag、不换栈）；`SubWeaponRuntime` 在折进同步内容后调用。客户端那份本来就是权威视图，所以**内容不同就折**（不再拿 `GunState.revision` 当"内容变了"的信号 —— 它只在枪械状态变化时推进，会漏掉真正的同步内容） |
| 2 | 按住 G 音效一直响 | 不是输入没去抖（`MeleeClientHandler` 的上升沿是对的），而是**换弹音效**在重复：`SubWeaponRuntime` 每次装配失败都新建 `Instance`，`wasReloading` 退回 `false`，下一 tick 就把"正在装填"当成一次新的"装填开始" | 装配改为**永远复用同一个 `Instance`**（见下），跳变标记不再丢失；缓存按客户端/服务端分开（单人游戏里两边共用静态表，会互相把对方的实例挤掉） |
| 3 | 装填走完提示却没装上 / 按 G 时装填计时被打回 | 两个 `GunData` 同时写一份 tag：主武器 `rebind` 走 `clearTag + merge`，把附件子 tag **换成副本**（`CompoundTag.merge` 对"原来不存在"的键是 `copy()`），缓存因此每 tick 未命中 → 新建 `GunData`；而 `GunData` 的实例收养路径可能把**根 tag 就是自己那份**的栈 rebind 回来，`clearTag` 之后 `merge` 的是刚被清空的自己 → **整份状态被抹掉**（连附件 `Id` 一起） | ①`GunData.reloadTagFrom` 加自合并保护：`incoming === tag` 时直接返回，按当前 tag 重新解码；②`SubWeaponRuntime.installed` 遇到副本**不重建**，而是把副本折进手里那份（只认更新的 revision）再把手里那份用 `Attachment.setTag` **挂回槽位** —— 引用、`GunData`、`Instance` 三者全程不变 |

**装配规则（现在是这个类唯一的规则）**：
只要槽位里还是同一个配件、而且我们手里那份 tag 里有枪械状态，就永远复用同一个 `Instance`
（`liveTag` 引用、`GunData`、`wasReloading` / `autoReloadBackoff` 全部连续）。
只有"从没装配过 / 换成了别的副武器 / 手里那份本来就没状态"才会新建。

**④ 复用实例的代价：`GunData.state` 会冻结 → 折进新内容后必须重新解码**

`GunData.state` 是**构造时解码一次**的镜像，之后只靠本实例自己的写入跟进。
旧实现每次 resync 都重建 `Instance`，顺带也就重新解码了；改成"永远复用"之后，
客户端那份 `GunData` 再没人重新解码 —— 于是它一直拿着**装配那一刻**的弹药/装填状态。

修法：`GunData.pullFromTag()`（只重读状态，不合并 tag、不换栈，是 `rebind` 的"轻量版"），
由 `SubWeaponRuntime` 在折进同步内容之后调用。客户端那份本来就是权威视图，所以**内容不同就折**
（`foldIncoming(..., force = client)`）—— 不再拿 `GunState.revision` 当"内容变了"的信号：
那个编号只在**枪械状态**变化时推进，用它当门禁会漏掉真正的同步内容，客户端就永远停在旧快照上。
服务端反过来：手里那份才是权威，只在副本确实更新时才折，免得被旧快照倒回去。

> **这一条也是"未装填好按 G 却响开火音"的根源之一。** 但最终的修法没有停在"让客户端状态变新"上：
> 只要第一人称音还是客户端**预测**，就永远存在"客户端以为能打、服务端正在装填"的窗口
> （同步粒度、时序、以及其它客户端写入都会影响它）。所以开火音改成**服务端拍板**（见上表 1b），
> 客户端状态是否新鲜不再影响音效正确性。

**音效链路（最终形态）**：

```
服务端：canShoot 为真 → shoot() → 成功
        └─ GunItem.resolveFire1PSounds(subWeapon) → player.playLocalSound(...) ──► 射手客户端

主武器：客户端 playGunClientSounds → playGunFire1PSound
        └─ 同一个 resolveFire1PSounds（客户端本地 player.playSound，天然只有自己听得到）

副武器换弹：SubWeaponRuntime 在"开始 / 完成"跳变时 playLocalSound（ReloadSound / ReloadEndSound）
```

要点：**① 只有真的开火才会响**（服务端是唯一事实来源）；
**② 音量/音高口径只有一份**（`GunItem.resolveFire1PSounds`，两条链路共用）。
播放本身**不需要新报文**：`SoundTool.playLocalSound` 就是"服务端让某个客户端播一条音效"的现成做法
（`ClientboundSoundPacket` + `Holder.Direct`，因此数据包里按名字写的、不在音效注册表里的音效
也能正常发过去）。曾经试过"自定义报文 + 客户端 `playSound`"，效果等价但多一层管道，已回退。

> 顺带记一笔排查结论：`playLocalSound` **不是**当初"听不到 1P 音"的原因 ——
> 那时的真正原因是自动装填的换弹音（音量 1.0）在开火后 2 tick 就响起、并被状态反复重建
> 触发成一串（一次开火响 2~12 遍），把 0.5 音量的开火音整个盖住了。换弹音重复的根因见第 ② 条。

**顺带（不属于上述三条，但会误导排障）**：`DataValidator` 用**另起一份**解码结果做校验，
而 `IDBasedData.id` 不是序列化字段，往返一趟会丢 —— 于是 `SubWeapon.Data` 缺省（＝用物品 id，
正常写法）也会被报成 `SubWeapon points at gun data ''`。现在往返之后会补打 id 戳。

**顺带（复用正常枪械流程）**：自动装填的入口从"自己拼 `busy` 判定"换成主武器那条
`GunData.shouldStartReloading` → `GunEventHandler.tryStartReload`，与主武器 `autoReload` 完全同一个谓词和入口
（它自带"正在装填 / 正在拉栓 / 计时器没归零 / 没有备弹 / 弹匣是满的"全部拒绝条件）。

**返修验收**：
```
1. /sbw attachment @s set SubWeapon superbwarfare:sub_weapon_gp_25，背包里带上 40mm 榴弹
2. 按一下 G → 一声 m_79_fire_1p（与手持 m_79 开火同款），弹匣空 → 自动装填（m_79_open）
3. **按住 G 不放**：全程只响一次开火音 + 一次装填音/完成音；不会一直响
4. 装填完成 → 绿色完成提示 + ammo=1/1；按 G 立刻能打出下一发（不会"提示走完了却打不出去"）
5. 装填过程中按 G：什么都不发生（不打断、不重置计时），装填照常走完
6. **没装填好之前（空仓 / 装填中）连按 G：一声开火音都不该有**（只有冷却时的 trigger_click）
7. melee_debug_log 打开时，服务端日志里一次装填只应出现**一次** `reload started` 与一次 `reload finished`
```

#### 11.8.4 配件互斥：显式冲突名单（`ConflictsWith`）

> **状态：✅ 已完成**（需求方要求）。目标冲突表：
>
> | | 刺刀 BAYONET | 握把 GRIP | 副武器 SUBWEAPON |
> |---|---|---|---|
> | **刺刀** | — | 可以共存 | **互斥** |
> | **握把** | 可以共存 | — | **互斥** |
> | **副武器** | **互斥** | **互斥** | — |
>
> （其余槽位：副武器与枪口配件 BARREL 仍可共存；刺刀与枪口配件、一如既往互斥。）

**为什么不能靠挂点组解决**：挂点组（`AttachmentSlot.mount`）是**传递**的等价关系 ——
登记到同一个组名就互相排斥，且"同组"会一路传染。合并挂点组只能表达"一组里只能装一个"，
表达不了上面这张表：副武器要同时排斥两个组，而那两组的成员之间还必须能共存。

**做法**：在挂点组之外加一份**非传递**的显式名单。

| 落点 | 说明 |
|---|---|
| `AttachmentSlot.conflictsWith: Set<AttachmentType>` | 槽位登记项上的默认名单。`SUBWEAPON` 登记为 `{BAYONET, GRIP}`（`AttachmentSlots.ALL` 里那一条） |
| `AttachmentDefinition.ConflictsWith: List<AttachmentType>` | 配件数据可以**追加**自己排斥的槽位（例如某个大号下挂件再排斥瞄具）。未知槽位名会被 `DataValidator` 的严格解析直接报错 |
| `AttachmentSlots.declaredConflicts(type, definition)` | 上面两份名单的并集 |
| `AttachmentSlots.conflicts(type, defA, other, defB)` | **唯一判定入口**：挂点组相同 **或** 任一方点名了对方 → 互斥；任一方 `AllowSharedMount` → 放行（转接座的逃生口，两种互斥都适用） |
| `Attachment.conflict(type, definition)` | `GunData.availableAttachments` / 指令 / `Attachment.cycle` 用的查询（原 `mountConflict`，因为它现在不止管挂点组） |

指令失败文案从 `attachment.fail.mount`（"已经占用了 X 挂点"）改成 `attachment.fail.conflict`
（"%1$s 与它互斥，无法同时安装"）—— 显式互斥没有"挂点"可报，旧文案会误导。

**`/sbw attachment random all` 也跟着改**：原来只保证"每个挂点组抽一个"，现在每组抽完还要拿已抽中的
槽位再过滤一遍（同组 + 显式互斥），否则会抽出"指令都装不上"的组合。先抽到的组赢，后抽到的组让位。

**已有存档不受影响**：过滤只作用在"能不能装"上，不会自动拆掉已经同时装着的旧组合
（比如旧存档里 AK-12 上同时有握把和 `sub_weapon_gp_25`）。想恢复合法状态用
`/sbw attachment @s clear` 或拆掉其中一个。

**验收**：
```
1. AK-12 装 sub_weapon_gp_25 → 再装任意握把 → 失败："GRIP 与它互斥，无法同时安装"
2. AK-12 装 sub_weapon_gp_25 → 再装刺刀     → 同样失败
3. AK-12 装握把 → 再装刺刀 → **成功**（这两个仍然共存）
4. 装刺刀 → 再装枪口配件 → 仍然失败（挂点组 `muzzle_device`，与本期改动无关）
5. /sbw attachment @s set SubWeapon sub_weapon_gp_25（已有握把时）→ 失败文案是新的 conflict 那条
6. /sbw attachment @s random all 反复执行 → 不会出现"握把 + 副武器"或"刺刀 + 副武器"的组合
7. DataValidator：`ConflictsWith` 指向自己的槽位、或与 `AllowSharedMount` 同时写 → 各一条 warning
```



---

### 11.9 副武器开火表现（动画候选链 + 枪口焰归属）+ `SubWeapon.Data` 落地 ✅ 已实现

> **⚠ 四期修订提示**：本节是**三期后续**的落地记录，其中 **B（枪口焰归属）的判据**、**D（开火模式）**
> 与 **E（装填进度只在持有主武器时推进）** 在四期会被**大幅简化或删除**：
> B 的判据从"枪口焰窗口 + `isSubWeaponFire()`"简化成"**部署中**"；
> D 的整套客户端状态机（`Semi`/`Auto`/`Burst` 手写限流）由 `ClientEventHandler.handleGunShoot`
> 现成的模式分支接管；E 随自动装填一起删除（改为玩家按 R、且 R 走常规链路）。
> **A（`SubWeapon.Animation` 候选链）与 C（`SubWeaponInfo.Data` 落地）的结论继续有效**，
> A 只需把语义从"副武器开火时"修订为"副武器激活时"（§9.8.5）。详见 §9.8 与 §11.10。

> **状态：✅ 已完成。** 补上 §11.8 里按需求暂缓的副武器动画，并把一直没生效的
> `SubWeaponInfo.Data` 真正接上。**仍然不做**：HUD、改装界面、副武器自己的换弹动画
> （换弹音效照旧由配件数据声明，见 §11.8.1-⑭）。

#### A. `SubWeapon.Animation`：**宿主枪**的开火动画候选链

做法照抄刺刀的动作候选链（`MeleeAction.Animation`，§11.5 的"动画候选链 + 短名拼接"），
只是换成了**开火**这一条：

| 项 | 结论 |
|---|---|
| 字段 | `SubWeaponInfo.Animation: SingleOrList<String>?` —— 字符串 = 单候选，列表 = 候选链，`[]` = 明确不要候选 |
| 默认 | 不写 = `["fire_sub_weapon"]`（`SubWeaponInfo.DEFAULT_FIRE_ANIMATION`） |
| 解析 | `GunAnimationNames.resolveFirst(candidates, 宿主枪 id) { 动画文件里有没有这支 clip }`：短名拼成 `animation.<枪 id>.<短名>`，**第一个存在的** clip 胜出 |
| 兜底 | 候选全落空 → 宿主枪自己的 `GunAnimation.Fire`，并按解析结果去重记一条日志：**显式写的候选**落空 = error（数据/动画文件写错了），**默认候选**落空 = debug（"这把枪没做这支 clip"是正常情况，不该让日志看起来像坏了） |
| 效果 | AK-12 的动画文件里做了 `animation.ak_12.fire_sub_weapon` → 按 G 打榴弹就播它；没做的枪（绝大多数）→ 照常播自己的 `fire`。**配件数据一个字都不用改**，是"有专属动画就用、没有就退回通用"的同一套表达 |

**只有副武器开火（G）走候选链**，主武器开火照旧 `GunAnimation.Fire`：`fire_sub_weapon` 是
"下挂筒发射"的整枪动画（AK-12 那支长 1.2s），拿它当步枪连发的开火动画是不对的。

**触发点**：`SubWeaponFiredMessage`（服务端 → 射手）→ `SubWeaponClientHandler.playFireAnimation`
→ `GeoGunAnimationInstance.triggerFire(stack, candidates, reportMissing)`。

**开火表现与开火音走同一条口径：服务端确认。** 报文由 `SubWeaponFireMessage.handler()` 在
`canShoot` 为真、真的打出这一发之后发出（就挨着那声 1P 音），客户端收到才播。
最早是客户端在按下 G 的那一刻自己播的，于是"副武器**装填期间**按 G 也会演一遍开火动画"
（动画是纯表现，没有任何判定会兜底 —— 音效早就按这个口径修掉了，动画漏了）。
现在服务端没打出去就不会有这个报文，这一整类"空演"在结构上不可能发生。
代价是动画比按键晚一个单程延迟 —— 与服务端那声 1P 音、以及服务端生成的榴弹同步，反而更一致。

**同时开火多个副武器**是边角情况：每个槽位各发一条报文，客户端按报文各播一次。

**不抛壳**：副武器开火不再往 `pendingShellEjects` 里塞一发 —— 弹壳模型与 `shell` 骨骼都是
**主武器**的 `ShellEject` 配置，打出去的却是副武器的弹药，照旧抛壳就是"榴弹发射时步枪抛壳"。

#### B. 枪口焰 / 枪口烟改挂**副武器模型**的 `flare`

| 项 | 结论 |
|---|---|
| 枪口焰挂点 | `MuzzleFlashRenderer`：副武器开火期间只画在**副武器模型自己的 `flare` 骨骼**上（`GeoGunRenderer.resolveSubWeaponFlareTransform` = 挂点骨骼 × 配件模型的 `flare` 变换，与渲染那条路径同一个判定）；**解析不到就什么都不画**，绝不退回主武器的枪口（榴弹从下挂筒出去，枪管前端不该喷火） |
| 窗口 | `ClientEventHandler.subWeaponFireRotTimer`（阈值同 `fireRotTimer`：`0 < t < 0.3` 可见、3.0 归零）。**刻意不复用 `fireRotTimer`**：后者还会带动整把枪的后坐表现（`handleShootAnimationV2` 读它），而副武器的后坐由它自己的开火动画负责，叠加会抖两下。主武器一开火就把这个窗口清零，火焰立刻回到主武器的枪口 |
| 消音器 | 副武器开火**不看** `isBarrelSilenced`：枪口配件只消它自己那根枪管 |
| 缩放 | 用副武器配件的 `MuzzleFlashScale`（与枪口配件共用同一个字段，不必新开一个只对这一处生效的字段） |
| 枪口烟 | 动画关键帧里的 `muzzle_smoke`（`locator: "flare"`）同样改挂副武器的 `flare`：`GeoGunAnimationInstance.isSubWeaponFire()` 为真时改写 `lastBoneTransforms[FLARE_BONE]`，并且**不注册**枪口配件的 `MUZZLE_BONE`（`resolveMuzzleLocator` 优先取它，留着会把榴弹的烟吸到枪管前端去） |
| 判定"这一发是不是副武器专属动画" | `isSubWeaponFire()` = 触发时带候选 **且** 解析出来的 clip ≠ `GunAnimation.Fire`，并且开火 runner 还在播。候选全落空、播的仍是 `fire` 时它是 `false` —— 那一发视觉上就是主武器在开火，枪口效应该留在主武器的枪口上 |
| 适用范围 | V2 渲染器（`GeoGunRenderer`）的枪。老的 GeckoLib 渲染路径（`AnimationHelper.handleShootFlare`）不参与：它的火焰由 `fireRotTimer` 驱动，副武器开火期间那个计时器是 0，所以不会画错位置（也不会画） |

#### C. `SubWeaponInfo.Data` 真正接上

此前只有 `DataValidator` 读它，运行时永远按物品 id 解析 —— 字段等于没生效。现在：

| 项 | 结论 |
|---|---|
| 落地 | `SubWeaponRuntime.applyBaselineId` → `GunData.setDefaultDataId(stack, id)`（与"载具武器共用一个物品 id"同一套机制），**必须在 `GunData.from(stack)` 之前写**：`GunData` 构造时就把 `defaultDataId` 解码进状态 |
| 默认 | 不写 = 附件自己的注册 id（`sbw/guns/<id>.json` 同名成对），**行为与之前完全一致** |
| 持久化 | 那份子 tag 就是主武器 NBT 里的附件子 tag → id 随主武器存档走、也随同步到客户端，两边解出同一份基线 |
| 数据包改了 `Data` | 复用的 `Instance` 上补写新 id + `pullFromTag()`（同一份 tag，引用 / `GunData` / `Instance` 三者不变），不必拆了重装 |
| 主要用途 | **多对一**：多个配件 id 共用一份副武器枪数据。不建议与"手持形态那把武器"共用一份 json —— 两份数据的关注点不同，共享等于把两边的平衡焊死（详见 §9.2） |
| 校验 | `DataValidator`：id 在 `sbw/guns` 里不存在 = **致命**；候选链里的空名字 = **致命**（与动作表同一条规则）。clip **是否存在**查不了（要读客户端的动画文件），仍然靠运行时日志 |
| 调试 | `/sbw subweapon info` 打印 `gunData=<实际使用的枪数据 id>`；缺基线时的 error 日志也报这个 id |

#### D. 开火模式：`Semi` / `Auto` / `Burst` 都按副武器**自己**的数据走

模式读的是副武器自己那份 `GunData`（`sbw/guns/<id>.json` 里的 `DefaultFireMode` / `AvailableFireModes`），
与主武器的开火模式完全独立 —— 主武器怎么切都不影响副武器。**没有专门的副武器开火模式字段**：
`GunData.selectedFireModeInfo()` 本来就走 PMC，模式里的 `Override`（比如某个模式换 RPM）也照常生效。
⚠ 副武器**没有切换开火模式的入口**（切模式键只作用于手持的那把枪），所以它实际用的永远是数据里的
`DefaultFireMode`：写 `"DefaultFireMode": "Auto"` + `"AvailableFireModes": ["Auto"]` 就是连发副武器。

| 模式 | 按 G | 说明 |
|---|---|---|
| `Semi`（默认） | 一次按键一发 | 上升沿触发，按住不放不会再打（但整个按住期间 G 都归副武器，不掉到近战） |
| `Auto` | **按住就连发** | 每客户端 tick 都会尝试一次，**节奏由冷却表决定**（见下），射速就是数据里的 `RPM` |
| `Burst` | 一次按键打 `BurstAmount` 发 | 与主武器一致：**松开 G 之后剩下的也会打完**（`SubWeaponClientHandler.tick` 推进，按键状态无关） |
| `Hold` / `Charge` | 按半自动处理 | 副武器没有蓄力输入链路（没有 `holdingFireKeyTicks` 那套），蓄力模式在这里没有意义 |

**连发的节奏不自己算**：客户端每 tick 尝试一次，能不能打由**服务端写进主武器冷却表的
`sub:<槽位>`**（每发 `1200 / RPM`）决定 —— 客户端只读，不往那张表里写。
于是连发只会**比标称射速略慢**（客户端看到的是同步过来的冷却值，慢一两个 tick），
永远不会比服务端快（快了也会在 `canShoot` / 冷却那里被拒）。
连发的每个 tick 还会跳过客户端已知"正在装填/拉栓"的槽位，少发几个注定被拒的报文；
**首次扣扳机那一下刻意不这么判** —— 客户端那份状态慢一拍，拿它当门禁会把真正打出去的那一发吞掉。

**装填与弹药（`Magazine > 1`）不需要任何额外代码**：开火扣弹药走的是主武器同一条
`GunItem.afterShoot`（`ammo -= AmmoCostPerShoot`），自动装填走的是主武器同一个谓词
（`SubWeaponRuntime.tick` → `shouldStartReloading` → `tryStartReload` → `reloadAmmo`），
弹种、备弹、换代、退弹、能量弹匣全部复用。数据侧要注意的只有换弹时间：

| 数据 | 结论 |
|---|---|
| `Magazine = 1`（GP-25 这种） | 只有"空仓装填"一种，写 `EmptyReloadTime` 即可 |
| `Magazine > 1` | 打空后按 `EmptyReloadTime` 装填；**没写就是 0 tick（瞬间装满）**，`DataValidator` 会警告 |
| `TacticalReload: true` | 没打空时按 `NormalReloadTime` 装填；**开了却没写这个字段同样是 0 tick**，也会警告 |

**验收**：
```
1. AK-12 装 sub_weapon_gp_25 按 G → 播 animation.ak_12.fire_sub_weapon，枪口焰与烟都在 GP-25 的枪口
2. 换成没做 fire_sub_weapon 的枪（如 M4）装同一配件按 G → 照常播它自己的 fire（默认候选落空只留一条 debug，不报 error）
3. 消音器 + 副武器共存，按 G → 榴弹的枪口焰照常出（消音器只消枪管那一发）
4. 左键打主武器 → 枪口焰仍在主武器的 flare 上（副武器窗口还没走完时也一样）
5. 按 G 打榴弹 → **不抛壳**；主武器连发照旧抛壳
6. /sbw subweapon info → gunData=superbwarfare:sub_weapon_gp_25（配件里写了 Data 就是那个 id）
7. 配件 json 写 "Data": "superbwarfare:not_exist" → /reload 时 DataValidator 报致命
8. **装填期间按 G / 按住 G** → 既没有开火音、也没有开火动画与枪口焰（服务端没打出去就没有报文）
9. 副武器数据写 "DefaultFireMode": "Auto" + "AvailableFireModes": ["Auto"] → 按住 G 持续开火，
   节奏等于 RPM；松手立刻停
10. 副武器数据写 BurstAmount: 3 + DefaultFireMode/Modes 为 Burst → 按一下 G 打 3 发（中途松手也会打完），
    打完要再按一次；Magazine 设 6 → 打空后自动装填，`/sbw subweapon info` 里能看到 ammo 回满
11. Magazine > 1 且没写 EmptyReloadTime → /reload 时 DataValidator 给一条警告
```

#### E. 装填进度只在**持有主武器**时推进（切走即中断，切回来从头装）

副武器的装填由服务端在 `SubWeaponRuntime.tick` 里自动做，而这段逻辑原来**无条件把
`inMainHand = true` 传进 `gunTick`**，于是"只要开了装填就一定会走完"：
把枪收回背包、切到别的枪，装填照样在背包里跑完，切回来弹药已经满了。

| 项 | 结论 |
|---|---|
| 进度推进 | `SubWeaponRuntime.tick` 把主武器的 `inMainHand` **原样**传给副武器的 `gunTick` —— 换弹计时器、栓动计时器、单发装填各阶段只在持有的那些 tick 里走（主武器自己就是这样：那段代码在 `gunTickInternal` 的 `if (inMainHand)` 块里） |
| 切走 | 主手没拿着这把枪时**直接中断装填**：`reload.setTime(0)` + `NOT_RELOADING` + 单发装填的阶段计时器 + `bolt.actionTimer.reset()`。与主武器切枪时 `LivingEventHandler` 做的是同一件事，所以"装填 3/5 秒时切枪，切回来是从 0 重新装" |
| 不误报完成 | 中断的那一次**不算"装填结束"**：`Instance.wasReloading` 直接归零，不播完成音效、不发"装填完成"提示（那两样挂在开始/结束的跳变上） |
| 照常推进的 | 热量、冷却、perk、各种计时器仍然与持有状态无关 —— 它们在同一个 tick 里，只是不在 `inMainHand` 分支内 |
| 不受影响的 | 自动装填的**判定与退避**（只在持有这把主武器时才触发）、换弹音效、动作栏进度提示 |

**验收**：
```
1. 副武器空仓 → 自动装填开始（有开始音效）→ 中途切到别的枪 → 切回来：从 0 重新装，且没有"装填完成"提示
2. 把带副武器的枪放进副手/背包里放一会儿 → 装填进度不涨、弹药不变（不会自己装满）
3. 装填走完（持有全程）→ 照旧播完成音效 + 绿色提示，弹药补满
```



---

### 11.10 四期：**副武器「主/副武器切换」机制** 🟡 设计完成，未实现

> **状态：尚未开工。** 规范见 **§9.8**，本节只回答"改哪些文件、按什么顺序、怎么验收"。
> **一句话**：G 从"触发一次副武器射击"改成"**切换当前操控的枪**"，副武器由此获得完整的
> 开火/换弹/瞄准能力，**近战恒用主武器**。

#### 11.10.1 定位与前置

| 项 | 结论 |
|---|---|
| 与前三期的关系 | **只动副武器的操控方式**，近战本体（一期）、配件体系/刺刀（二期）、`MeleeEffect`（三期 A）**一行不动**；三期 B 的 `SubWeapon` **装配/状态存储/渲染挂点全部保留**（§9.3 的五条不变量继续成立） |
| 开工前必须先做 | **§9.8.9 的 `GunStackStorage`**。理由：四期要动 `SubWeaponRuntime` 的装配代码，而那是全仓唯一直接摸 `stack.tag` 的地方；先把适配点建起来，改装配就不会变成"改两遍" |
| 最大风险 | **双端状态不一致**。部署状态必须由**服务端**写（§9.8.10 不变量 3），客户端只读确认——三期已经为同类问题付过一次代价（§11.8.3） |
| 最容易改错的地方 | **近战**。近战的"动画驱动"是主武器、"数据"也必须恒为主手（§9.8.11），而 `MeleeClientHandler` 里 `data` 与 `stack` 是同一个来源——把 `stack` 换成 `ActiveGun` 就会顺手把近战也切到副武器上，**这是四期唯一一个"看起来更统一、实际是 bug"的改法** |

#### 11.10.2 改动清单

| 模块 | 文件 | 动作 |
|---|---|---|
| **数据层** | `data/gun/GunState.kt` | +`ActiveSlot: String = ""`、+`ActiveOwner: StructuredUUID? = null`（§9.8.1）。**key 名是持久化/线路格式，定了不许改** |
| | `data/gun/GunData.kt` | +`activeSlot` / `activeOwner` 两个 `StateStringValue` 风格访问器（照 `defaultDataId` 的写法，`GunData.kt:1815` 一带）；`save()` 的"内容变了才 bump revision"逻辑自动生效 |
| | `data/gun/subdata/Cooldown.kt` | −`subWeaponKey()`（三期为"按 G 触发"限流用；四期不再需要，§9.8.5） |
| | `data/attachment/SubWeaponInfo.kt` | +`HoldAnimation` / `ViewBone`；**不做** `ReloadAnimation`（换弹动画走副武器自己的资源，§9.8.7）；**保留** `ReloadSound` / `ReloadEndSound`；+`holdAnimationCandidates()` / `viewBoneOrNull()`（§9.8.5） |
| | `data/stack/GunStackStorage.kt`（新） | 版本适配**唯一入口**（§9.8.9）。1.20.1 实现 = 现有 `getOrCreateTag` 逻辑原样搬进来 |
| | `data/DataValidator.kt` | +`SubWeapon` 校验：`ViewBone` 写空串 = 警告；`HoldAnimation` 候选链里的空名字 = 致命。⚠ **不校验副武器的 reload clip 是否存在**：那是客户端的动画文件，属资源侧（§10 已有的"资源校验"一栏） |
| **通用入口** | `tools/ActiveGun.kt`（新） | `mainStack(player)` / `stackOf(player)` / `dataOf(player)` / `isDeployed(data)` / `activeSlot(gun): AttachmentType?` / `subInstance(gun, client)`。**全仓唯一的"当前操控的枪"解析点** |
| | `item/gun/GunItem.kt` | +`isOperable(stack)`（"这个栈能不能被当枪操作"），与已有的 `isHeldWeapon(stack)` 分工；把开火/瞄准/近战/动画状态机那批**门禁**从 `isHeldWeapon` 换成 `isOperable`，其余（渲染/视角/HUD/属性/`inventoryTick`）**保持 `isHeldWeapon`**（§9.8.1 的坑） |
| **报文** | `network/message/send/SubWeaponDeployMessage.kt`（新） | 客户端 → 服务端：请求切换（§9.8.10） |
| | `network/message/receive/SubWeaponDeployedMessage.kt`（新） | 服务端 → 客户端：确认（`slot` / `owner` / `ok`） |
| | `network/message/send/SubWeaponFireMessage.kt` | **整个删除**（开火走 `FireKeyMessage`） |
| **副武器运行时** | `subweapon/SubWeaponRuntime.kt` | 保留：`installed` / `find` / `Instance` / 装配 / 缓存（改用 `GunStackStorage` + 载体令牌）。删除：自动装填、`autoReloadBackoff`、`wasReloading`、`onReloadStarted`/`onReloadFinished`、`showReloadingProgress`、`cooldownKey`/`cooldownTicks`、三个装填相关常量。`tick` 的 `inMainHand` 入参改成 **=`主手拿着宿主枪 && 部署的就是这个槽位`** |
| **客户端运行时** | `client/gun/SubWeaponClientHandler.kt` | 重写：只留"按 G → 发 `SubWeaponDeployMessage`"与"收到确认 → 重置切枪状态"。删除 `burstRemaining`/`burstOwner`/`fire`/`repeatsWhileHeld`/`burstAmountOf`/`holdsFire`/`playFireAnimation` |
| | `client/gun/MeleeClientHandler.kt` | G 分支改成发切换请求（`tryTrigger` 的返回值语义改成"这次 G 归副武器了吗"）；**近战分支的 `data`/`stack` 保持主手**（§11.10.1 的风险点） |
| | `event/ClientEventHandler.kt` | A 组 `mainHandItem` → `ActiveGun`（约 20 处，见 §9.8.1）；近战入口 `handleGunMelee` 显式收主手 stack；`handleGunShoot`/`handleWeaponZoom`/`handleWeaponDraw`/`handleGunRecoil`/`shootClient`/`handleShootAnimationV2` 全部改读副武器数据；`subWeaponFireRotTimer` 的存在理由改成"部署期间" |
| | `event/ClickEventHandler.kt` | `handleWeaponFirePress`/`handleWeaponZoomPress` 用 `ActiveGun.stackOf`；`handleWeaponFirePress` 里"主手不是枪"的早退**保留看主手** |
| | `event/ClientMouseHandler.kt` | `:86`/`:271` 两处同上 |
| | 六个 overlay（`CrossHair` / `AmmoBar` / `AmmoCount` / `HeatBar` / `HandsomeFrame` / `ItemRendererFix`） | 同上；`AmmoBarOverlay` 的 `:512` 一带是备弹显示，一并走 `ActiveGun` |
| **动画 / 渲染** | `resource/model/AttachmentModelReloadListener.kt` | 补第二个构造参数 `animPath = "animations/bedrock/attachment"`（**一行**，§9.8.7）；动画与模型按**文件名 id 配对**，所以 `sub_weapon_gp_25.animation.json` 自动绑到同名 geo |
| | `client/model/attachment/BedrockAttachmentModel.kt` | +`applyPose(pose)` / `resetPose()` / `getIndex(name)` / `getBone(index)`（照 `GeoGunModel.kt:80-95` 抄，`instance` 已在手边） |
| | `client/animation/gun/GeoGunAnimationInstance.kt` | 部署中 + 副武器 `reloading()` 时，用 `GunResource.compute(副武器合成栈).animation` 的 reload clip 建**副武器自己的 runner**（状态挂在宿主枪的动画实例上，键 = 副武器槽位），每 tick 推进并暴露 `subWeaponPose()`；`triggerFire` 的候选链来源改成"部署中的副武器"；`shouldSpin` 改读 `ActiveGun`。**不需要**原来的 `MERGE_BLENDER` 合并路径（§9.8.7） |
| | `client/renderer/gun/GeoGunRenderer.kt` | `renderRegisteredAttachments` 对 `SUBWEAPON` 槽位：渲染前 `applyPose(副武器 pose)`、渲染后 `resetPose()`；`computeViewTransform` 增加副武器 `iron_view` 优先（`subWeaponAimTransform`，§9.8.6）；`renderModel` 里 `subWeaponFire` 的条件从"枪口焰窗口"改成"**部署中**"，`resolveSubWeaponFlareTransform` 复用；`applyCameraShake` 的 `GunData` 改读副武器 |
| **服务端** | `event/GunEventHandler.kt` | `gunTickInternal` 里 `SubWeaponRuntime.tick` 的 `inMainHand` 入参改成"部署中"；宿主枪在被副武器顶替期间**不再走主手那套** `if (inMainHand)` 块 |
| | `event/LivingEventHandler.kt` | 主手物品真的换了时清 `ActiveSlot`/`ActiveOwner`（§9.8.10 不变量 2）；网络包 handler（`FireKeyMessage`/`ReloadMessage`/`ShootMessage`/`WeaponZoomingMessage`/`SwitchScopeMessage`/`AdjustZoomFovMessage`/`UnloadMessage`/`SensitivityMessage`/`MouseMoveMessage`/`FireModeMessage`）改用 `ActiveGun.dataOf(player)` |
| | `network/message/send/MeleeAttackMessage.kt` | `source` 只认 `"MAIN"`；`SUB:<slot>` 分支删除（§9.8.11） |
| **调试** | `command/SubWeaponCommand.kt` | 输出改成"槽位 / 数据 id / **是否激活** / 弹药 / 备弹 / 换弹状态"；删掉冷却与 `canShoot` |
| **美术 / 资源** | `sbw/guns/sub_weapon_gp_25.json`（**新建**） | 副武器的枪数据 + 枪械资源二合一：三期那份枪数据内容搬过来，再补 `Animation.Reload`（`animation.sub_weapon_gp_25.reload`）与 `Model`（指向附件模型与**新增的**附件动画文件） |
| | `animations/bedrock/attachment/sub_weapon_gp_25.animation.json`（**新建，待美术**） | 副武器自己的换弹动画，**按附件模型的骨骼名做**（`root`/`gun`/`tube`/`ammo`/`trigger`）。参考 `animation.gp_25.reload` 的长度（1.2s）与节奏，但骨骼名不能照抄（那是手持形态模型的） |
| | 各枪 `animations/bedrock/gun/<枪>.animation.json` | 需要（可选）：`fire_sub_weapon`（AK-12 已有）、`hold_sub_weapon` |
| | `models/bedrock/attachment/sub_weapon_gp_25.geo.json` | 需要（可选）：加一支名为 **`iron_view`** 的骨骼（§9.8.6 的 ①） |
| | `sbw/attachments/sub_weapon_gp_25.json` | 保持现状即可（`ReloadEndSound` **保留**）；需要时加 `HoldAnimation`/`ViewBone` |
| **语言** | `en_us.json` / `zh_cn.json` | `key.superbwarfare.subweapon_fire` 的文案从"副武器开火"改成"**切换副武器**"（en: `Toggle Sub-Weapon`）；删除三条 `info.superbwarfare.subweapon.*`；+`info.superbwarfare.subweapon.deployed` / `.holstered`（切换成功的动作栏提示，可选） |

#### 11.10.3 建议的实施顺序

四期是"横切"改动，顺序错了会反复返工。建议按下面六步走，**每一步都能单独编译 + 单独验收**：

| 步 | 内容 | 可独立验收的现象 |
|---|---|---|
| **①** | `GunStackStorage` + `SubWeaponRuntime` 改用它（**行为零变化**） | 三期的手动验收步骤（§11.8.2 / §11.8.3 返修验收）**全部照旧通过** |
| **②** | `GunState` 两个字段 + `ActiveGun` + 两条报文 + `/sbw subweapon info` | 按 G 后 `/sbw subweapon info` 显示 `active=true`；重进游戏 / 换枪后状态正确；`melee_debug_log` 能看到 deploy 日志 |
| **③** | 输入与 A 组读取点全部改走 `ActiveGun`（开火/换弹/瞄准/后坐/HUD） | **副武器能开火、能按 R 装填、能右键瞄准**，弹药条显示副武器的弹药；主武器这几样在未部署时**逐项与改前一致** |
| **④** | 近战分支显式收主手 + `MeleeAttackMessage` 只认 `MAIN` | 副武器激活时按 V → 主武器挥刀、伤害按主武器算；副武器不参与判定 |
| **⑤** | 动画与渲染（副武器自带资源的换弹动画 / 附件 animPath / `BedrockAttachmentModel.applyPose` / 开火候选链 / 瞄准位形 / 枪口焰） | AK-12 + GP-25：按 G 端起来、左键播 `fire_sub_weapon`、换弹时**榴弹筒自己在动**（宿主枪保持 idle）、枪口焰在榴弹筒上；副武器**没做** reload clip 时 → 回退宿主枪的换弹动画且只留一条 debug 日志 |
| **⑥** | 清场：删除三期的死代码、语言、`DataValidator`、`SubWeaponCommand` | `grep` 检查（见 §11.10.4）全绿；`./gradlew compileKotlin compileJava runData` 通过 |

#### 11.10.4 验收

**自动化 / 静态检查**：
```
1. ./gradlew compileKotlin compileJava runData        # 编译 + datagen
2. grep -rn "mainHandItem" src/main/kotlin/com/atsuishio/superbwarfare/event \
                           src/main/kotlin/com/atsuishio/superbwarfare/client
   → 剩下的必须全在 §9.8.1 的 B 组（"物理上拿着的东西"）里，逐条能说出理由
3. grep -rn "SubWeaponFireMessage\|justPressed" src/                   → 应为空
4. grep -rn "\.tag" src/main/kotlin/com/atsuishio/superbwarfare/subweapon \
                    src/main/kotlin/com/atsuishio/superbwarfare/data/attachment
   → 应为空（版本相关 API 只在 GunStackStorage 实现里）
5. grep -rn "subWeaponKey\|cooldownKey\|RELOAD_HINT_INTERVAL" src/     → 应为空
6. grep -rn "isHeldWeapon" src/main/kotlin/.../event src/main/kotlin/.../client/gun
   → 只剩"手持副武器物品本身按普通物品处理"那一类（§8.3.1），开火/瞄准/近战门禁都已换成 isOperable
```

**手动验收（GP-25 + AK-12）**：
```
1. /sbw attachment @s set SubWeapon superbwarfare:sub_weapon_gp_25，背包里带 40mm 榴弹
2. 按 G      → 端起来（宿主枪做切换动作），切枪进度走完之前按左键不生效（动作锁）
              → /sbw subweapon info 显示 active=true
3. 左键      → 打出 40mm 榴弹；第一人称音 = gp_25_fire_1p；枪口焰/烟在**榴弹筒**的 flare 上
              → 宿主枪的枪管**不**喷火；**不抛壳**
4. 弹匣空 → 按 R → 换弹；**榴弹筒自己动**（开膛 / 装弹 / 闭膛），宿主枪保持 idle 呼吸摆动
              → 换弹音来自配件的 `ReloadSound`/`ReloadEndSound`；弹药条显示 0/1 → 1/1
5. 右键      → 能瞄准；GP-25 模型没有 iron_view → 走宿主枪的机瞄位形（不报错）
6. 按 V      → **主武器**挥刀（刺刀动作若有），副武器挂在枪上不动；伤害按主武器算
7. 再按 G    → 切回主武器；主武器此前被打断的换弹没有变成"完成"
8. 按住 G 不放 → 只切一次（切换动作期间再按 G 被动作锁拒掉）
9. 切到别的枪（滚轮） → 副武器状态自动收起；切回来是主武器而不是副武器
10. 把枪丢在地上再捡起来 → 状态跟着枪走（NBT 持久化）
11. 没装副武器的枪按 G → 等同 V（近战），与三期一致
12. 改动前的主武器体验回归：22 把旧枪的近战、开火、换弹、瞄准**逐项不变**
13. 资源缺失路径 A：临时把 `sbw/guns/sub_weapon_gp_25.json` 的 `Animation.Reload` 删掉
    → 换弹时回退**宿主枪自己的换弹动画**，日志只留一条 debug（不报 error）
14. 资源缺失路径 B：找一把**没有** `fire_sub_weapon` 的枪装 GP-25 → 照常播它自己的 `fire`
    （默认候选落空只留 debug 日志）
15. 资源加载路径：确认 `/reload` 后 `animations/bedrock/attachment/sub_weapon_gp_25.animation.json`
    能被加载（放一支测试 clip，改 `Animation.Reload` 指向它 → 换弹时播出来）
```

#### 11.10.5 与设计稿不一致 / 需要提前知道的地方

**① 需求里"按下 G 之后在主武器和副武器之间切换"——不是"换物品"，而是"换被操控的枪"。**
需求原话是"应该让当前操控的 gun 变成副武器"，这一点完全照做；但**不能**真的把副武器物品
放进玩家主手（§9.8.1 列了三个硬障碍：主手是双端权威槽、合成栈是凭空造的、
快捷栏与"枪身上的配件"基数不同）。落地形态是 `GunState.ActiveSlot` + 全仓统一的
`ActiveGun` 读取入口。**收益与需求一致**：开火/换弹/瞄准全部复用原链路，没有 `subweapon` 专用分支；
**代价**是 `event/` + `client/` 两个目录里 **61 处**读取点要做一次机械替换
（全仓 Kotlin 侧 119 处；§9.8.1 的 A/B 分组表）。

**② 需求的第 2 条"副武器开火动画暂时还是跟现在一样，在配件的 subweapon 数据里面定义一下（或者不用定义）"
——结论：不用新增字段，现有 `SubWeaponInfo.Animation` 就是它，默认值 `["fire_sub_weapon"]` 也对。**
只做一处**语义修订**（三期的"副武器开火时"→ 四期的"副武器激活时"），
所以 GP-25 的 json **一个字都不用改**，AK-12 已经做好的
`animation.ak_12.fire_sub_weapon`（1.2s、只驱动 `root`）**原样接管**。
"主武器使用 `fire_sub_weapon` 动画进行开火"也由它表达——**那支 clip 本来就住在主武器的动画文件里**，
它驱动的正是主武器模型（`root`），所以三期"宿主枪播候选链"的实现方式在四期**恰好就是想要的**。
唯一要注意的是 `fire_sub_weapon` 是**一次性开火动作**，不能拿它当持枪态循环；
持枪态另开 `HoldAnimation`（可选，不写就是宿主 idle）。

**③ 需求的第 2 条"副武器有 zoom_view 骨骼"——仓库里没有这个名字，落地成 `iron_view` / `scope_view`。**

| 需求里的说法 | 仓库里的真实骨骼 | 四期落地 |
|---|---|---|
| `zoom_view` | 不存在 | — |
| 瞄准位形 | `iron_view`（机瞄）/ `scope_view`（瞄具分划）/ `bipod_view`（脚架） | 副武器 = `SubWeaponInfo.ViewBone` → 附件模型的 `iron_view` → 宿主枪的 `scope_view`/`iron_view` |
| 容易被误认成它 | `camera` | 它**只用于屏幕抖动收敛**（`applyCameraShake`），与瞄准位形无关，**不要**拿它当挂点 |

并且现状**付不出**"副武器自己的瞄具位形"：`sub_weapon_gp_25.geo.json` 里**没有 `iron_view`**。
所以四期刚落地时 GP-25 走的是宿主枪的位形（整枪抬到机瞄、榴弹筒跟着上去，视觉可接受）；
要做成"贴榴弹筒自己的照门"，**只需在附件模型里加一支 `iron_view` 骨骼**，数据与代码都不用改。

**④ 需求的第 3 条"副武器只由左手操控，换弹动画也只有左手骨骼会动"——修订为「换弹动画归副武器自己的资源」（本节按修订版写）。**
原方案（**已作废**）是"把左手换弹做在宿主枪的动画文件里，再用 `MERGE_BLENDER` 覆盖宿主 idle"，
它的出发点是"副武器模型没有自己的动画实例"。**这个出发点不成立**：
`BedrockAttachmentModel` 内部持有 `TreeModelInstance`（`BedrockAttachmentModel.kt:28-29`），
与 `GeoGunModel` 同源，缺的只是 `applyPose`/`resetPose` 这两层薄封装（照抄即可）；
而 `BedrockModelReloadListener` 本来就支持 `animPath`（`:16-18`），
`AttachmentModelReloadListener` 只是**没传**（`AttachmentModelReloadListener.kt:10-12`）。

所以修订后的做法是：**副武器有自己的 `GunResource`**（`GunResource.compute(副武器合成栈)` 按物品 id
解析 `sbw/guns/sub_weapon_gp_25.json`，那份 json 同时是枪数据与枪械资源），
换弹动画写它的 `Animation.Reload`，由它自己的附件模型播。
"与主武器 idle 融合"= **宿主枪照常播 idle、副武器播自己的换弹**，
两个模型是各自独立的姿势树，**骨骼命名空间不重叠**（副武器的 `root` 是它自己模型的根，不是宿主枪的），
所以**天然不冲突**，也**不需要姿态融合原语**。**副武器不做 idle，持枪态以主武器为准**（按需求）。
唯一放弃的是"左手真的去够榴弹筒"：那只手是宿主枪模型的 `lefthand`，副武器的动画碰不到它——
按钮式枪管自己开膛装弹在视觉上是干净的，本期接受。
完整落地清单（附件 animPath / `applyPose` / 副武器 runner 三处改动）见 §9.8.7。

**⑤ 换弹音效：保留配件的 `ReloadSound` / `ReloadEndSound`（与 ④ 连带修订）。**
原方案说"改由宿主枪换弹动画的关键帧负责、删掉这两个字段"，那是在"动画做在宿主枪上"的前提下成立的。
现在动画归副武器自己的资源，而**附件播放链路不接 `sound_effects` 关键帧**——
GP-25 的 `animation.gp_25.reload` 里那 4 条 `sound_effects`
（`common_grab_1`/`gp_25_reload_1`/`gp_25_reload_2`/`common_grab_2`）在新链路里**不会响**。
所以三期那套"配件声明音效 + 状态跳变时 `playLocalSound`"**原样保留**，是最省事也最不容易出错的选择。

**⑥ 自动装填删除，玩家自己按 R。**
需求没有直接说这一条，但它是"让副武器用原本的开火、换弹、瞄准"的**必然推论**：
三期之所以要自动装填，正是因为 G 被"触发一次射击"占满了、没有键位留给 R。
现在副武器就是当前操控的枪，`R` 天然可用，自动装填反而会与手动装填抢状态机。
**回归**：三期 §11.8.1-⑭ / §11.9-E 关于自动装填的整段设计、三条动作栏提示、
`autoReloadBackoff` 全部作废（§9.8.4）。

**⑦ 枪口焰的归属规则简化了。**
三期需要 `subWeaponFireRotTimer`（一个 0.3s 的窗口）+ `isSubWeaponFire()`（"这一发播的是不是
副武器专属 clip"）两个条件来判断"火该喷在哪"。四期只要一个条件：**部署中 → 火归副武器**。
判据更简单也更准（不会出现"部署着、但这一发播的是宿主 `fire`，于是火喷在枪管上"的错位）。
`MuzzleFlashScale` 仍用副武器配件的那个（§11.9-B 的结论保留）。

**⑧ 触发冷却（`sub:<slot>`）删除。**
它是"按 G 触发一次"的限流器。四期开火走副武器自己的 `RPM`（`GunData` 的现成冷却），
G 是切换、由动作锁限流，所以宿主枪冷却表上那一类键**没有存在的理由**。

**⑨ 双版本兼容：只抽"存储访问"，**不**抽 `CompoundTag`。**
1.21.1 死的是 `ItemStack` 上的 NBT 槽位，**NBT 本身还在**，而 `GunData`/`GunState`/`Attachment`/
`AmmoSlot` 全部建立在 `CompoundTag` 上（`GunState` 走 `encodeToCompoundTag`/`decodeFromCompoundTag`）。
把 `CompoundTag` 换成中立模型 = 重写整个枪械数据层，**不可行也不必要**。
所以保留 `CompoundTag` 作为内存态，只把"它挂在物品上的哪儿、怎么读写"收进 `GunStackStorage`
（§9.8.9）。1.21.1 侧的实现是**自注册一个承载 `CompoundTag` 的 DataComponent**，
于是 `SubWeaponRuntime` / `GunData` / 附件子 tag 的全部业务逻辑**一行都不用改**。
唯一需要额外留心的是"**活引用**"这条不变量（三期全部坑的根源）：1.21.1 侧必须
"读出来 → 原地改 → 写回去"，并且给实现配一个**载体令牌**（`carrierToken`）来替代三期的
`liveTag` 引用比较。**实际移植不在本期范围**。

**⑩ 本期不做的**：副武器专属 HUD / 准心（等需求方重写 HUD，四期只保证"读的是当前操控的枪"，
所以重写时天然支持）、副武器的换弹动画本身与 `hold_sub_weapon`（**要美术产出**，
代码侧在缺失时优雅回退）、副武器自己的 `iron_view` 骨骼（同上）、
附件动画的 `sound_effects` 关键帧（§9.8.7 已说明为什么不接）、
"下挂筒翻起来"的专属切换动画（需要时往 `GunAnimation` 加一支 `Deploy`）、1.21.1 分支的实际移植。

---

## 12. 决策记录

### 12.1 已定稿

| # | 议题 | 结论 |
|---|---|---|
| 1 | `@` 前缀 | **统一加 `@`**（`@empty`/`@ray`/`@melee`），旧裸写法保留为兼容别名 |
| 2 | 冷却机制 | **仿 Perk 走枪械 NBT** + 服务端递减；**绝不用原版物品冷却** |
| 3 | 副武器键 | **G** |
| 4 | G 的语义 | **有副武器 → 使用副武器；没有 → 等同 V（近战）**；V 永远近战 → ⚠ **四期改成「主武器 ↔ 副武器切换」**（§9.8.2 / §12.6-56） |
| 5 | 副武器的定义 | **能力式 `SubWeapon` POJO**，任何槽位带它即为副武器 |
| 6 | 副武器与 GunData | **寄生 GunData**：合成栈用副武器物品本身 + 共享附件子 tag + 默认按物品 id 解析数据（§9.3） |
| 7 | 刺刀 | **不是副武器**，只改主武器近战动作表 |
| 8 | 配件物品类层次 | **`AttachmentProvider` 接口 + `BasicAttachmentItem`（原 `AttachmentItem` 改名）+ `SubWeaponItem : GunItem`**；安装/提示/命令统一按接口判断（§8.3） |
| 8b | 副武器物品手持时 | **按普通物品处理**：`GunItem.useAsWeaponInHand()` + 静态 `isHeldWeapon(stack)`，约 50 处手持门禁（含 6 个改视角的 Mixin）（§8.3.1） |
| 9 | 多个副武器 | 不做优先级，**遍历一次逐个触发**，动作占用统一持有一次 → ⚠ **四期改成「切到枚举顺序里的第一个」**（§9.8.2） |
| 10 | 副武器空仓 | 按 G **尝试装填一次** → ⚠ **四期废除：玩家自己按 R**（§9.8.4） |
| 11 | 挂点组 | 保留系统；刺刀与下挂榴弹**互斥**（三期返修按需求方要求改的，见 §11.8.4；原结论是"不同 mount 可共存"） |
| 12 | 打头/打腿倍率 | 全部复用现有值（`Headshot` 1.5 / 打腿 0.5），不新增全局字段 |
| 13 | 扫掠采样 | 每 15° 一步、上限 8 |
| 14 | 距离口径 | 改为「到目标 **AABB 最近点**」（接受这一处行为变化） |
| 15 | 渲染骨骼 | 只新增约定 `bayonet_pos`；其它一律用配件的 `Bone` |
| 16 | HUD / 改装界面 | **不在本方案范围**，后续自行重写 |
| 17 | 长按 V 连挥 / V 键位重复 | 保持现状（有意设计 / 不会同时触发） |

**当前没有待你拍板的开放项。** 实现过程中若遇到与预期不符的既有行为，按"先记进 §12 的决策记录、再改"的方式处理。

> **一期实现期间的补充决策见 §12.2**；与本文不一致的实现细节见 §11.2（共 23 条），遗留缺口见 §11.4；
> **四期的设计期决策见 §12.6**（尚未实现）。

### 12.2 一期实现期间补充的决策

| # | 议题 | 结论 |
|---|---|---|
| 18 | 副武器开火键的名字 | 键位常量命名为 **`SUBWEAPON_FIRE`**（默认 `G`），不用 `SUB_WEAPON`/`G` 之类的临时名 |
| 19 | 近战数据类的包 | 收在 **`data/gun/melee/`** 子包，不平铺在 `data/gun/`（§11.2-⑯） |
| 20 | 近战伤害与 `ATTACK_DAMAGE` | **不乘属性加成**，`damage = MeleeDamage × 命中区域 × 衰减`；属性本身保留（§11.2-③） |
| 21 | 穿甲 | 按 `BypassesArmor` **拆两段**：护甲段 `hurt()` + 穿甲段 `forceHurt()`（§6.2 注） |
| 22 | `player.swing` 打在哪一端 | **客户端**（不是设计里的服务端）——同样修掉缺陷 3，且第三人称更跟手（§5.5 注） |
| 23 | 连挥动画重播 | 新增 `swingSerial` / `consumedMeleeSerial`（§5.1 注、§11.2-②） |
| 24 | Perk 上下文 | `MeleeAttackContext` 同 tick 传递 + `Perk` 新增带上下文的重载（旧签名照旧调用）（§11.2-⑰） |
| 25 | 语言文件范围 | 一期只补 **`en_us` + `zh_cn`**（§11.2-⑱） |
| 26 | 旧 GeckoLib 路径 | **明确不迁移**：只做编译修正，`ClientEventHandler.gunMelee` 退化为恒 0 的 `@Deprecated` 占位（§11.2-⑮） |

### 12.3 二期实现期间补充的决策

| # | 议题 | 结论 |
|---|---|---|
| 27 | 槽位注册表的**挂点组默认值** | 5 个既有槽位各自独立（`scope_rail`/`magazine_well`/`muzzle_device`/`stock_interface`/`grip_rail`）→ **现有行为零变化**；刺刀 `muzzle_lug` 与枪口 `muzzle_device` 不同组、可共存。握把与将来的 `underbarrel_rail` **本期不合并**（合并＝玩法改动，等三期下挂落地再定） |
| 28 | 新增槽位的**默认渲染方式** | `AttachmentRenderMode.GENERIC`：注册表按 `MountBone`（`Fixed` 约定骨骼 / `FromDefinition` 配件自己的 `Bone` / `GunModel` 切枪模型骨骼）自动渲染，新槽位不用写渲染代码。既有 5 个槽位是 `CUSTOM`（瞄具分划、枪托适配器、护木、枪口焰各有专属逻辑） |
| 29 | `AttachmentItem` 旧名 | **直接删除，不留 `typealias`**：仓库里已无引用，别名只会让旧名字继续扩散 |
| 30 | 刺刀的属性表达 | **`Modifiers` 管标量、`Override.MeleeActions` 管形状与手感**，两者并存：伤害/距离只写在 `Modifiers` 里（工具提示才显示得出来、也避免两处相乘），动作表只写 `Animation`/`Duration`/`HitTime`/`Hitbox`/`Sweep`/`MaxTargets`/`Knockback`（§11.5.3-②） |
| 31 | `MeleeRange` 的语义 | 从"`MeleeHitbox.Range` 的兜底值"改成**叠加值**（`rangeOr(0) + MeleeRange`）：旧数据逐值等价，配件从此能用一条 `Modifiers` 加近战距离，不必整块覆盖 `MeleeHitbox`（§11.5.3-③） |
| 32 | 动作表里的动画名 | **`Animation` 是候选链 + 短名拼接**：`animation.` 开头=全名原样用，其它按 `animation.<宿主枪 id>.` 拼接，列表按顺序取第一个存在的。**只用于 `MeleeActions.Animation`**，`GunAnimation.*` 仍写全名（§11.5.3-①） |
| 33 | 刺刀的动画 | 写 `["hit_bayonet", "hit"]`：动画做出来之前在**所有枪**上都自动落在枪自己的 `hit` 上；以后往某把枪的动画文件里加 `animation.<枪>.hit_bayonet` 即可生效，**配件数据不用改** |

### 12.4 二期后续（手感调整）期间的决策

| # | 议题 | 结论 |
|---|---|---|
| 34 | 默认判定形状 | **长方体**（`Type` 默认值改成 `BOX`，1.8×1.8、`YOffset -0.2`）：圆锥的手感不好，而且它的调试线框是坏的（§11.7.1） |
| 35 | 判定体长度 | **三种形状统一用 `reach`**，删掉 `MeleeHitbox.Length`：盒子/胶囊不再各写一套长度，`MeleeRange`、配件加成、`RangeMultiplier` 才能对每个形状都生效 |
| 36 | 动作表的伤害/距离 | **只给倍率**（`DamageMultiplier` / `RangeMultiplier`），绝对值只来自枪的属性；`MeleeAction.Damage` 直接删除 |
| 37 | 近战打头/打腿倍率 | **独立属性** `MeleeHeadshot`（2.0）/ `MeleeLegshot`（0.5），不再复用投射物的 `Headshot` |
| 38 | 爆头归属 | **只有准星正对的那个目标**能爆头：客户端射线取准星实体 → 报文 `aimed` → 服务端 `aimed && isHeadshot(hitPos)`；打腿不受限 |
| 39 | 刺刀与枪口配件 | **互斥**（同一挂点组 `muzzle_device`），不做"卡榫与消音器共存" |
| 40 | `Cone` 的去留 | **保留**（数据包可能还在用），但不再是默认；顺手修掉它的俯仰符号错误与失真的调试线框 |
| 41 | 命中区域量哪个点 | **准星射线到目标 AABB 的最近点**（`Hit.zonePos`）。一期的"眼睛到 AABB 最近点"会被夹到眼睛高度 → 平地上打哪儿都判爆头（§11.7.4 的坑） |
| 42 | 枪托近战基础距离 | `MeleeRange` 默认 **0 → 2.0**（生存 3.0 → 5.0 格）；刺刀的额外距离仍走它的 `Modifiers`（§11.7.6） |
| 31 | 改装界面 / HUD | **一行未动**（按需求）。`EditMessage` 与 `GeoGunRenderer.attachmentFocusBone` 改成读 `AttachmentSlots.EDIT_ORDER`，界面按钮下标与它前 6 项保持一致；刺刀在下标 6，暂时只能用指令安装 |
| 32 | 配件物品 tag 的生成 | 从"每个槽位在 `ModTags`/datagen 里各写一遍"改成**注册表驱动**：`ATTACHMENT_BY_SLOT` / `attachmentRarityTag()` + datagen 循环，新增槽位不再需要手写 tag 常量 |

### 12.5 三期实现期间补充的决策

| # | 议题 | 结论 |
|---|---|---|
| 43 | `MeleeEffect` 的预设数据类 | **复用 `MeleeEffectSpec`**，不另开一个 POJO：同一个类既是条目也是预设文件。代价是它的字段必须**全部可空**（见 44），收益是覆盖规则只有一处实现 |
| 44 | 效果的覆盖粒度 | **逐字段覆盖**：条目 → 预设 → 行为默认值。`Chance`/`Trigger`/`Cooldown` 因此也改成可空并在 `resolve()` 里补默认 |
| 45 | `Effects` 的元素类型 | `List<StringOrObject<MeleeEffectSpec>>?`。裸 `List<MeleeEffectSpec>` 遇到字符串简写会**整个数据文件解析失败**，而文档里的简写写法是常见用法 |
| 46 | 概率与冷却的时机 | `Chance` 只在服务端 roll；冷却**只在真的触发之后**才写（语义是"发动过之后这段时间不再发动"，不是"每 N tick 掷骰子"） |
| 47 | `explosion` 的默认破坏性 | `DestroyBlocks` 缺省 **false**。近战触发的爆炸不该拆家；要拆自己写 `true` |
| 48 | 新增 `Amplitude` 字段 | `screen_shake` 需要"时间/半径/幅度"三个独立数值，设计稿字段表里只有两个，补一个比复用 `Damage`/`Count` 干净 |
| 49 | 副武器槽位名与挂点 | **`AttachmentType.SUBWEAPON`**（`"SubWeapon"`）+ 约定骨骼 **`subweapon_pos`**，挂点组 `subweapon_rail`（不叫 `UNDERBARREL`、不用配件的 `Bone`）——按需求方要求 |
| 50 | 副武器挂点组是否与握把合并 | **不合并挂点组**（`subweapon_rail` ≠ `grip_rail`，挂点组要保持可细分），互斥改成**显式声明**：副武器 `ConflictsWith` 刺刀与握把，而刺刀与握把仍可共存（§11.8.4，按需求方要求） |
| 51 | 副武器首个载体 | **`gp_25`**（下挂式 40mm 单发榴弹发射器，`Magazine 1`、`RPM 60`、属性参考 `m_79`）；模型与贴图**复用 `steel_pipe_silencer`**，模型做好后只改配件 json 的 `Model`/`Texture` 两行 |
| 52 | 副武器动画与 HUD | 三期时**都不做**（按需求）；**§11.9 已补上开火动画**（`SubWeapon.Animation` 候选链，默认 `["fire_sub_weapon"]` → 退回 `Fire`）与枪口焰/烟归属。副武器复用主武器开火链路的音效；HUD 与改装界面仍然一行未动，副武器继续用 `/sbw attachment` 安装 |
| 53 | 副武器状态放哪 | 全部写在自己合成栈的 tag 上，而那个 tag **就是主武器 NBT 里的附件子 tag** → 随主武器持久化，无新存档字段。`SubWeaponInfo.AmmoSlot` 目前不影响开火（见 §11.8.1-⑦） |
| 54 | 副武器的触发冷却 | 写在**主武器**的冷却表上（键 `sub:<槽位>`，时长一律取 `1200 / RPM`，配件数据里不配），这样客户端能直接读到，不必先装配再判断 → ⚠ **四期废除**：开火走副武器自己的 RPM，G 是切换、由动作锁限流（§9.8.5） |
| 55 | 自动化的验收 | `./gradlew compileKotlin compileJava runData`（生成物品模型与物品 tag）。**近战效果与副武器的实际手感仍需手动验收**，步骤见 §11.8.2 |

### 12.6 四期设计（副武器「主/副武器切换」）期间的决策

> 四期**尚未实现**，本节的编号是设计期的定稿；实现期间若遇到与预期不符的既有行为，
> 按同样的方式追加到本节。

| # | 议题 | 结论 |
|---|---|---|
| 56 | G 的语义 | **主武器 ↔ 副武器的切换**（不是"用一次副武器"，也不是"换物品"）：`GunState.ActiveSlot` 由**服务端**写，客户端只发请求 + 读确认（§9.8.1 / §9.8.10） |
| 57 | 为什么不真的换主手物品 | 主手是双端权威槽（客户端改写会被同步冲掉）、副武器栈是"凭空造的合成栈"（丢出去会掉出不该存在的物品）、快捷栏 9 槽与"枪身上的配件"基数不同 → **一律不换物品**，只换"被操控的枪"（§9.8.1） |
| 58 | "被操控的枪"怎么表达 | 全仓统一入口 `ActiveGun.stackOf/dataOf`；`mainHandItem` 的 **Kotlin 侧 119 处 + Java 侧 70 处**分成 A 组（正在操作的枪 → 改，主战场是 `event/`+`client/` 的 61 处）与 B 组（物理上拿着的东西 → 不改，含全部 Mixin / `inventoryTick` / `getAttributeModifiers` / 改装界面）（§9.8.1） |
| 58b | 门禁谓词 | 新增 `GunItem.isOperable(stack)`（"这个栈能不能被当枪操作"），与 `isHeldWeapon(stack)`（"这件物品拿在手上算不算枪"）**分工**。**必须分开**：`SubWeaponItem.useAsWeaponInHand() == false`，所有 `if (!isHeldWeapon(stack)) return` 式的门禁会把副武器整个挡在门外；而 `inventoryTick` 那一侧必须留在 `isHeldWeapon`（否则副武器会被 tick 两遍）（§9.8.1 的坑） |
| 59 | 副武器的开火/换弹/瞄准 | **零专属代码**：`GunData.shoot` / `tryStartReload` / `zoom` 全是纯 `GunData` 驱动，把 `GunData` 换成副武器那份即可；三期的 `SubWeaponFireMessage`、`SubWeaponClientHandler` 的 `Semi`/`Auto`/`Burst` 手写状态机全部删除（§9.8.3） |
| 60 | 近战 | **恒用主武器**：V 走主手的 `GunData`（装了刺刀就是刺刀动作），副武器没有近战输入也不参与判定；`MeleeAttackMessage` 的 `SUB:<slot>` 链路删除（§9.8.2 / §9.8.11） |
| 61 | 副武器的换弹 | **玩家按 R**；删除三期全部自动装填（`shouldStartReloading` 轮询、退避、`wasReloading` 跳变、动作栏进度、开始/结束音效）（§9.8.4） |
| 62 | 换弹音效 | 由**宿主枪换弹动画的关键帧**负责；配件字段 `ReloadSound`/`ReloadEndSound` **删除**（避免两套音源重音）（§9.8.4）→ ⚠ **已推翻，见 §12.6-65c：两字段保留**（动画改成做在副武器自己的资源里之后，附件链路不接音效关键帧）（§11.10.5-⑤） |
| 63 | 副武器开火动画 | **不新增字段**：现有 `SubWeaponInfo.Animation` 就是它（默认 `["fire_sub_weapon"]`），三期"宿主枪播候选链"的实现方式在四期恰好就是想要的；只需把语义从"副武器开火时"修订为"副武器激活时"（§9.8.5 / §11.10.5-②） |
| 64 | 副武器的换弹动画放哪 | **放副武器自己的枪械资源里**（`sbw/guns/sub_weapon_gp_25.json` 的 `Animation.Reload: animation.sub_weapon_gp_25.reload`）——副武器是独立的 `GunData`，因此天然有独立的 `GunResource`，与普通枪同一套机制。落地三处改动：`AttachmentModelReloadListener` 补 `animPath`、`BedrockAttachmentModel` 补 `applyPose`/`resetPose`、宿主动画实例里给副武器建一个 runner（§9.8.7）。**`SubWeaponInfo.ReloadAnimation` 不新增**（原方案作废） |
| 65 | 副武器动画怎么与主武器"融合" | **不需要姿态融合**：宿主枪照常播 `idle`、副武器播自己的换弹，两个模型是**各自独立的姿势树**，骨骼命名空间不重叠（副武器的 `root` 是它自己模型的根），所以天然不冲突。原方案（在宿主枪动画文件里做一支只含 `lefthand` 的 clip + `NoAllocMergeBlender` 覆盖宿主 idle）**作废**（§9.8.7 / §11.10.5-④） |
| 65b | 副武器要不要 idle | **不要**：换弹之外它就是静止挂在枪上，**持枪态以主武器的 idle 为准**（按需求）。`SubWeaponInfo.HoldAnimation` 只是给"整装被端起来"留的可选口子（§9.8.7） |
| 65c | 副武器换弹音效 | **保留** `SubWeaponInfo.ReloadSound` / `ReloadEndSound`（三期实现原样复用）：新增的附件动画播放链路**不接 `sound_effects` 关键帧**，数据包里写在副武器动画里的音效不会响（§9.8.7 / §11.10.5-⑤） |
| 66 | 副武器瞄准位形 | 需求里说的 `zoom_view` 在仓库里不存在，落地为：`SubWeaponInfo.ViewBone` → 附件模型的 **`iron_view`** → 宿主枪的 `scope_view` / `iron_view`。`camera` 骨骼只用于屏幕抖动收敛，**不是**瞄准位形（§9.8.6 / §11.10.5-③） |
| 67 | 枪口焰归属 | 判据从"开火窗口 + 是否播了副武器专属 clip"简化为**一条：部署中 → 火归副武器**（更简单也更准）；`MuzzleFlashScale` 仍用副武器配件的（§11.10.5-⑦） |
| 68 | 触发冷却 | **删除** `Cooldown.subWeaponKey` / 宿主枪冷却表的 `sub:<slot>` 键：它是"按 G 触发"的限流器，四期不需要（§11.10.5-⑧） |
| 69 | 双版本（1.20.1 Forge / 1.21.1 NeoForge） | **只抽"存储访问"，不抽 `CompoundTag`**：新增 `GunStackStorage`（NBT / DataComponent 两个实现），`CompoundTag` 仍是内存态，业务逻辑共用；三期的 `liveTag` 引用比较改成**载体令牌** `carrierToken`。实际移植不在本期范围（§9.8.9 / §11.10.5-⑨） |
| 70 | 动作锁 | `GunAction` 的 `SUB_WEAPON` 保留，语义改成"**切换中**"（时长 = `max(两把枪的 DrawTime) + 余量`）；切换期间开火/换弹/近战/再次切换全部被拒（§9.8.8） |
| 71 | 切换表现 | 复用现成的 `drawTime` + `resetGunStatus()`，时间常数取两把枪 `DrawTime` 的较大者；专属的"下挂筒翻起来"动画留到将来往 `GunAnimation` 加一支 `Deploy`，本方案不预留（§9.8.8） |
| 72 | 实施顺序 | 六步：① `GunStackStorage`（行为零变化）→ ② 状态 + 报文 + 命令 → ③ 输入/A 组读取点 → ④ 近战收主手 → ⑤ 动画与渲染 → ⑥ 清场。**每步都能单独编译 + 单独验收**（§11.10.3） |

**四期没有待拍板的开放项。** 唯一需要外部输入的是**美术产出**（副武器自己的换弹动画
`animations/bedrock/attachment/sub_weapon_gp_25.animation.json`、`hold_sub_weapon`、
附件模型的 `iron_view` 骨骼）——三者都做了"缺失时优雅回退"，所以不阻塞代码落地（§11.10.5-⑩）。

---

## 13. 附：与现有基建的对应表

| 需求 | 复用什么（现成） | 位置 |
|---|---|---|
| 盒体/扫掠判定 | `OBB` + `OBB.isColliding(obb, aabb)` | `tools/OBB.kt:39`、`:486` |
| 打头/打腿阈值 | 投射物的 `eyeHeight`/`bbHeight` 判定（含 0.5 腿伤默认） | `ProjectileEntity.kt:305-315`、`:81`、`IAdvancedHitDetection.kt:181-191` |
| 过滤/队伍/烟雾 | `BASIC_FILTER`、`IN_SAME_TEAM`、`NOT_IN_SMOKE` | `tools/SeekTool.kt:57`、`:140`、`:75` |
| 「模式标记」写法 | `Projectile` 的 `empty`/`ray` 分支 | `GunItem.kt:738-746` |
| 「单值或列表」「字符串或对象」 | `SingleOrList`、`StringOrObject` + `@StringOrObjectFactory` | `data/SingleOrList.kt`、`data/StringOrObject.kt`、模板 `AmmoConsumer.kt:36` |
| 「id + 参数」的数据预设表 | `DataLoader.createData` + `IDBasedData` | `CustomData.kt:23`、`data/gun/ProjectileInfo.kt` |
| **配件物品身份** | 抽 `AttachmentProvider` 接口（现为 `is AttachmentItem`，仅 4 处引用） | `item/attachment/AttachmentItem.kt:11`、`ClientAttachmentImageTooltip.kt:43`、`AttachmentCommand.kt:267`、`ModItems.kt:627` |
| **「手持才算枪」的统一谓词** | 新增 `GunItem.useAsWeaponInHand()` + `GunItem.isHeldWeapon(ItemStack)`（静态，供 Mixin 调用）；替换约 40 处手持门禁 | 6 个 Mixin（`ItemInHandRendererMixin.java:18` 等）+ `ClickEventHandler` / `ClientEventHandler` / HUD overlay（§8.3.1 清单） |
| **配件数据 id → 定义** | `AttachmentDefinition.from(id)`（按物品/配件 id 查 `CustomData.ATTACHMENTS`） | `AttachmentDefinition.kt:166-170` |
| **副武器的"第二把枪"数据** | **配件定义说了算**：`SubWeapon.Data`（不写就是附件自身 id）→ `GunData.setDefaultDataId` 落到 tag 上，再由 `getDefault()` 解析（车辆武器同款机制） | `SubWeaponRuntime.applyBaselineId`、`GunData.getDefault()`、`GunData.kt:159-169` |
| **副武器状态存放** | `Attachment.getOrCreateTag(slot)` 返回枪 NBT 子 tag 的活引用 | `subdata/Attachment.kt:72-85` |
| **副武器弹药** | `AmmoSlot.getAmmo/set/reset(slot)` | `subdata/AmmoSlot.kt:17-43` |
| **副武器开火** | `GunData.shoot(...)` 全部重载 → `GunItem.shootBullet` | `GunData.kt:994-1019`、`GunItem.kt:726-800` |
| **副武器的开火/换弹/瞄准（四期）** | **不需要任何副武器专用链路**：`GunItem.shoot` / `tryStartReload` / `GunData.zoom()` 全是纯 `GunData` 驱动，把 `GunData` 换成副武器那份即可（§9.8.3） | `GunItem.kt`（`shoot`/`tryStartReload`）、`GunData.kt`（`zoom`/`canShoot`/`shouldStartReloading`） |
| **「当前操控的枪」的统一读取入口（四期）** | 新增 `ActiveGun.stackOf/dataOf`，替换 A 组约 61 处 `player.mainHandItem`（§9.8.1） | `tools/ActiveGun.kt`（新）+ `ClientEventHandler` / `ClickEventHandler` / `ClientMouseHandler` / HUD overlay / `network/message/send/*` |
| **双端一致的部署状态（四期）** | 写进 `GunState`（随枪持久化 + 随主武器同步），服务端权威；`Attachment.getOrCreateTag` 的活引用语义保留（§9.8.9） | `data/gun/GunState.kt`、`data/gun/GunData.kt` |
| **物品数据存储的版本适配（四期）** | 新增 `GunStackStorage`：`CompoundTag`（1.20.1 NBT）↔ `DataComponent`（1.21.1）两个实现，业务逻辑共用；`liveTag` 引用比较 → 载体令牌 `carrierToken`（§9.8.9） | `data/stack/GunStackStorage.kt`（新）、`SubWeaponRuntime`、`GunData.setDefaultDataId` |
| **副武器的换弹动画（四期）** | **副武器自己的 `GunResource`**（`CustomData.GUN_RESOURCE` 与 `GUN_DATA` 都从 `sbw/guns/<id>.json` 加载，`CustomData.kt:40`/`:94`）：换弹 clip 名取它资源的 `Animation.Reload`，由**它自己的附件模型**播（§9.8.7） | `GunResource.kt:76-85`、`CustomData.kt:40`/`:94`、`DefaultGunResource.kt:98-99` |
| **附件模型播放动画（四期）** | `BedrockModelReloadListener` 的 `animPath` 参数**本来就支持**（`:16-18`，按文件名 id 与模型配对）：`AttachmentModelReloadListener` 补一个参数即可；`BedrockAttachmentModel` 内的 `TreeModelInstance`（`:28-29`）与 `GeoGunModel` 同源，`applyPose`/`resetPose` 照抄（`GeoGunModel.kt:88-95`） | `AttachmentModelReloadListener.kt:10-12`、`BedrockModelReloadListener.kt:52-63`、`GunModelReloadListener.kt:11-14` |
| **副武器的循环持枪态（四期，可选）** | 照 `holdOpen` 的写法：`SubWeaponInfo.HoldAnimation` + 宿主枪的循环 runner（`updateHoldOpen` 同款） | `GeoGunAnimationInstance.kt:837-867` |
| **副武器瞄准位形（四期）** | `computeViewTransform` 里插一级：副武器附件模型的 `iron_view` → 宿主枪的 `scope_view` → 宿主枪的 `iron_view`（§9.8.6） | `GeoGunRenderer.kt:1146-1184` |
| **副武器的动画候选链（四期复用三期）** | `GunAnimationNames.resolveFirst(candidates, 宿主枪 id)`（二期为刺刀做的候选链 + 短名拼接） | `resource/gun/GunAnimationNames.kt`、`GeoGunAnimationInstance.resolveFireName` |
| **副武器开火的客户端表现** | **服务端确认**：`SubWeaponFiredMessage`（与那声 1P 音挨着发，`player.sendPacket`）→ 客户端播动画 + 枪口焰（§11.9-A/B）。⚠ **四期保留"服务端确认"这一条口径，但拍板对象从"这一发打没打出去"变成"这次切没切成"**（`SubWeaponDeployedMessage`）；开火表现回到主武器那套客户端本地播 | `network/message/receive/SubWeaponFiredMessage.kt`、`SubWeaponClientHandler.playFireAnimation` |
| **副武器实例身份** | `DATA_CACHE`（weakKeys 按栈实例）+ `UUID_CACHE` adopt + `rebind` | `GunData.kt:1785-1854`、`:1553-1575` |
| 冷却计数（写法样板） | `Perks.reduceCooldown(perk, key)`；玩家级用 `persistentData` | `subdata/Perks.kt:217`、`mobeffect/RadiationMobEffect.kt:100-109` |
| 伤害类型注册 / 标签 | `ModDamageTypes.registerDamageType`/`causeXxxDamage`；`ModTags.DamageTypes` | `init/ModDamageTypes.kt:17-55`、`init/ModTags.kt:206-240` |
| 强制伤害 / 药水效果 | `DamageHandler.forceHurt`、`LivingEntity.forceApplyEffect` | `tools/DamageHandler.kt:25`、`tools/EffectHandler.kt:10` |
| 爆炸 / 震动 / 音效 / 粒子 | `CustomExplosion.Builder`、`ShakeClientMessage.sendToNearbyPlayers`、`SoundTool.*`、`ParticleTool.*` | `tools/CustomExplosion.kt:501`、`ShakeClientMessage.kt:57`、`tools/SoundTool.kt:37-66`、`tools/ParticleTool.kt:30-66` |
| 进度阈值时间线（写法参考） | `GunActionStep`（**走 `getDefault()`，不可覆盖**） | `GunActionStep.kt:74`、`GunActionStepExecutor.kt:10` |
| 配件数值修改 / 对象覆盖 | `AttachmentModifier`、`Override` | `AttachmentDefinition.kt:174`、`:93` |
| 逐层属性叠加 | PMC 层序 | `GunData.kt:414-482` |
| 动画状态机 / 分层叠加 / 速度 | `resolveState`/`play`/`combineLayers` | `GeoGunAnimationInstance.kt:87`、`:259`、`:749` |
| 「配件 → 换动画」样板 | 弹匣等级 → 鼓式换弹动画 | `GunData.kt:71`、`:84`、`GeoGunAnimationInstance.kt:162-176` |
| 配件自带动画（二期） | `AttachmentModelReloadListener` 加 animPath + `BedrockAttachmentModel.applyPose` | `AttachmentModelReloadListener.kt:10`、`GeoGunModel.kt:88` |
| 附件挂骨骼渲染（多配件共存） | `GeoGunRenderer.renderAttachments` 注册表化 + `getGlobalTransform(bone)` | `GeoGunRenderer.kt:410-429`、`:576-643` |
| 线框调试渲染 | `RenderType.lines()` | `C4Renderer.kt:57` |
| 数据核验 | `DataValidator` | `data/DataValidator.kt` |

**需要自己新写、仓库没有原语的**：`lightning`、`Lift`（垂直上挑）、自定义冷却表、`GunActionLock`、槽位注册表 + 挂点组、`AttachmentProvider` 接口层、`GunItem.useAsWeaponInHand()` + `isHeldWeapon(stack)` 手持谓词、`SubWeaponRuntime`（寄生 GunData 的装配与 tick）；
**四期追加**：`ActiveGun`（"当前操控的枪"的统一读取入口）、`GunStackStorage`（NBT ↔ DataComponent 的版本适配点）、副武器的 `iron_view` 接进 `computeViewTransform`。
