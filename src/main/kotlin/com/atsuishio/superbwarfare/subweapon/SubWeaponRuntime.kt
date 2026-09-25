package com.atsuishio.superbwarfare.subweapon

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.attachment.SubWeaponInfo
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.subdata.Cooldown
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.event.GunEventHandler
import com.atsuishio.superbwarfare.item.attachment.SubWeaponItem
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime.CACHE
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime.MAX_CACHED_GUNS
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime.installed
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime.syncBack
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime.tick
import com.atsuishio.superbwarfare.tools.tag
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import java.util.*

/**
 * 副武器的运行时。
 *
 * 「寄生 GunData」的全部要点都在这里：
 *
 * | 要素 | 做法 |
 * |---|---|
 * | 物品 | 副武器物品**自己**（`SubWeaponItem : GunItem, AttachmentProvider`） |
 * | 合成栈 | `ItemStack(subWeaponItem)`，其 `minecraft:custom_data` 是主武器 `Attachments.<槽位>` 的**快照** |
 * | 数据基线 | 默认按物品注册 id 解析（`sbw/guns/<id>.json` 与配件同名成对出现）；`SubWeaponInfo.Data` 非空时才覆盖 |
 * | 状态 | 弹药/热量/换弹/耐久/revision 全写在合成栈的 `custom_data` 上，再由 [syncBack] 折回主武器子 tag → **随主武器 NBT 持久化**，无新存档字段 |
 * | 实例身份 | [CACHE] 持有合成栈的强引用，否则会掉出 `GunData.DATA_CACHE`（weakKeys） |
 * | tick | 合成栈不在背包里，`GunItem.inventoryTick` 不会跑 → 主武器 gun tick 里顺带 tick（见 [tick]） |
 * | 开火 | 服务端装配合成栈 → `GunData.shoot(...)`，与主武器**同一个入口** |
 *
 * ## 为什么这里不能照搬 1.20：共享 tag 已经不可能
 *
 * 1.20 时 `ItemStack` 直接抱一个 `CompoundTag` 字段，于是合成栈可以**共享**主武器附件子 tag 的实例：
 * 副武器的状态写进合成栈就等于写进了主武器 NBT。1.20.5 起 NBT 变成 `minecraft:custom_data` 组件，
 * `ItemStack(ItemLike, int, CompoundTag)` 构造器被删掉，`CustomData.of(tag)` 与兼容层的
 * `stack.tag` 读写**都是拷贝**（见 `tools/MinecraftUtil.kt`）。照搬 1.20 的两条判据会同时失效：
 * 1. `stack.tag !== liveTag` **永远为真**（读回来的是拷贝）→ 每次装配都重建合成栈与 `GunData`，
 *    副武器状态机被反复清零（换弹计时器永远走不完、按 G 没反应）；
 * 2. 状态写在一个和主武器 NBT 无关的游离 compound 上，**永远回不到主武器**。
 *
 * 所以这里改成**显式同步**：装配时从主武器子 tag 取一份快照，服务端每推进过一次副武器
 * （tick / 开火 / 换弹）就调用 [syncBack] 把宿主的状态折回主武器子 tag，再让主武器 `save()`
 * 写进物品栈；客户端只读，靠主武器 resync（`GunData.rebind` 会把这个槽位的 compound 换成新实例，
 * 见 [Instance.mainTag]）触发重建来拉取服务端状态。
 */
object SubWeaponRuntime {

    /**
     * 一把已装配的副武器。
     *
     * @param slot 它在主武器上的槽位（也是报文里用的标识：[slotName]）
     * @param attachmentId 配件数据 id（与合成栈根 tag 里的 `Id` 对应）
     * @param info 配件上的 `SubWeapon` 定义
     * @param stack 合成栈：副武器状态的宿主。它不在任何背包里，只被 [CACHE] 强引用着 ——
     *   否则会掉出 `GunData.DATA_CACHE`（weakKeys），下一 tick 又是一个新的 `GunData`。
     * @param mainTag 组装时主武器 `Attachments.<槽位>` 里的那个 compound **实例**。
     *   1.21 里它和合成栈的根 tag 是**两份数据**（见类注释），这里只用它当缓存键：
     *   主武器 resync 后 `attachmentTag` 会被清空再 `merge`，原来不存在的键是以 `copy()` 落进去的，
     *   所以这个引用一变就说明该重建合成栈、重新拉取服务端状态。
     * @param data 合成栈对应的枪械数据
     */
    class Instance(
        val slot: AttachmentType,
        val attachmentId: ResourceLocation,
        val info: SubWeaponInfo,
        val stack: ItemStack,
        val mainTag: CompoundTag,
        val data: GunData,
    ) {
        /** 报文与冷却键里用的槽位标识 */
        val slotName: String get() = slot.name

        /** 主武器冷却表上的键（`sub:<slot>`） */
        val cooldownKey: String get() = Cooldown.subWeaponKey(slotName)

        /**
         * 触发冷却 tick：配件写了就用它，否则按副武器数据的 RPM 算一个射击周期。
         *
         * 与主武器开火同一个口径（`1200 / RPM`），且至少 1 tick。
         */
        fun cooldownTicks(): Int {
            if (info.cooldown > 0) return info.cooldown
            val rpm = data.get(GunProp.RPM).coerceAtLeast(1)
            return (1200 / rpm).coerceAtLeast(1)
        }
    }

    /**
     * 按**主武器 [GunData] 实例**缓存（[IdentityHashMap]：键按引用比较）。一个实例 = 一把活着的枪，
     * 客户端每次 resync 都由 `GunData.rebind` 复用它，所以条目能跨 resync 存活。
     *
     * ⚠ 刻意**不用**主武器 uuid 当键：单机里客户端与服务端在同一个 JVM 里共享这张表，
     * 而两边是**两个不同的 [GunData] 实例**（各自持有自己那份 tag）、uuid 却相同 ——
     * 用 uuid 当键会让两边互相顶掉对方的条目，表现为每次装配都重建 `GunData`、
     * 副武器状态机被反复清零（换弹计时器永远走不完）。
     *
     * 强引用是有意的（见 [Instance.stack]），靠 [MAX_CACHED_GUNS] 兜底，不做无界增长。
     */
    private val CACHE = IdentityHashMap<GunData, MutableMap<AttachmentType, Instance>>()

    /** [CACHE] 的软上限：超过就整体丢弃（条目丢了会按主武器 NBT 里的子 tag 重建，成本很低） */
    private const val MAX_CACHED_GUNS = 256

    /** 已经吼过的"缺枪数据"id，避免每 tick 刷屏 */
    private val warnedMissingBaseline = HashSet<String>()

    // ------------------------------------------------------------------ 装配

    /**
     * 解析主武器上装着的全部副武器（按槽位去重，顺序 = `AttachmentType.entries`）。
     *
     * 只有**同时满足**这三条的槽位才算副武器：
     * 1. 该槽位装了配件，且配件的定义能解析出来；
     * 2. 定义里有 [SubWeaponInfo]；
     * 3. 对应物品是 [SubWeaponItem]（否则拿不到 `GunData`）。
     */
    @JvmStatic
    fun installed(gun: GunData): List<Instance> {
        val cache = cacheOf(gun)
        val found = LinkedHashMap<AttachmentType, Instance>()

        for (attachment in gun.attachment.installed()) {
            val info = attachment.definition.subWeapon ?: continue
            val item = BuiltInRegistries.ITEM.get(attachment.id) as? SubWeaponItem ?: continue

            // ⚠ 必须用 `getOrCreateTag`，不能用 `AttachmentInstance.tag`。
            //
            // `Attachment.installed()` 里的 tag 来自 `Attachment.getTag()`，它对**字符串形式**的
            // 槽位内容是 `CompoundTag().apply { putString("Id", ...) }` —— **每次调用都是一个新对象**。
            // 那个对象既当不了缓存键（每 tick 都"不命中"），也不是主武器 NBT 里真正存着的那个
            // compound（往它上面写等于写进垃圾桶）。`getOrCreateTag` 会把该槽位实体化成 compound
            // **并写回主武器 NBT**，之后 `getCompound` 返回的就一直是同一个实例，直到主武器 resync。
            val mainTag = gun.attachment.getOrCreateTag(attachment.slot)

            val cached = cache[attachment.slot]
            // 命中条件只看两件事：**还是同一个配件** + **还是同一份主武器子 tag 实例**。
            // 后者才是"主武器有没有被 resync 过"的判据（1.21 里合成栈根 tag 只是快照，比不了引用）。
            val reusable = cached != null
                    && cached.attachmentId == attachment.id
                    && cached.mainTag === mainTag

            found[attachment.slot] = if (reusable) {
                cached
            } else {
                // 合成栈：**不在背包里**的枪械栈，根 tag 是主武器子 tag 的快照（1.21 拿不到共享实例，见类注释）。
                // 副武器的全部状态就写在它上面，再由 [syncBack] 折回主武器。
                val stack = ItemStack(item)
                stack.tag = mainTag.copy()

                Instance(attachment.slot, attachment.id, info, stack, mainTag, GunData.from(stack))
                    .also { warnIfNoBaseline(it) }
            }

            // 缓存没命中时同时换掉了旧实例，保证同一个主武器子 tag 上**只有一个** GunData 在写：
            // `GunData.state` 是"解码一次就缓存"的镜像，两个实例会互相把对方的改动覆盖回去。
            if (!reusable && debugEnabled()) {
                debug {
                    "assembled ${attachment.slot} -> ${attachment.id}: cached=${cached != null} " +
                            "idMatch=${cached?.attachmentId == attachment.id} " +
                            "tagMatch=${cached?.mainTag === mainTag} " +
                            "bucket=${cache.size} uuid=${gun.uuid}"
                }
            }
        }

        cache.keys.retainAll(found.keys)
        cache.putAll(found)
        return found.values.toList()
    }

    /** 按槽位找一把副武器；`null` = 这个槽位没装副武器 */
    @JvmStatic
    fun find(gun: GunData, slot: AttachmentType): Instance? =
        installed(gun).firstOrNull { it.slot == slot }

    /** 按报文里的槽位标识找（`SUBWEAPON` 这样的枚举名） */
    @JvmStatic
    fun find(gun: GunData, slotName: String): Instance? =
        installed(gun).firstOrNull { it.slotName == slotName }

    // ------------------------------------------------------------------ 回写

    /**
     * 把合成栈上的副武器状态折回主武器 NBT。
     *
     * 1.21 里 `ItemStack` 不再共享 `CompoundTag` 实例（见类注释），"写进合成栈就等于写进主武器"
     * 这件事必须由这里补上 —— **服务端每次推进过副武器之后都要调一次**（[tick] 与
     * `SubWeaponFireMessage` 都已经调了），否则状态只活在 [CACHE] 里那个游离栈上，
     * 换枪 / 存档 / 同步到客户端时全会丢。
     *
     * 内容没变时直接返回，所以正常运行中不会每 tick 都去写一遍主武器物品栈。
     *
     * @param gun 副武器装在其上的主武器
     * @param instance [installed] 装出来的实例
     */
    @JvmStatic
    fun syncBack(gun: GunData, instance: Instance) {
        // 配件在这次推进期间被卸掉/换掉了：**不要**再往这个槽位写。
        // 否则子 tag 里会重新出现 `Id`，`Attachment.id` 又解析得出这个配件 —— 等于把它装了回来。
        if (gun.attachment.id(instance.slot) != instance.attachmentId) return

        // 让 `batch` 里拖延的写先落到合成栈上，再取宿主 tag 的完整内容（1.21 的 `tag` 读回来是拷贝）。
        instance.data.flush()
        val host = instance.stack.tag ?: return

        // 回写目标永远是**当前**主武器里的那个 compound 实例：中途主武器被 resync 过时，
        // 组装时记下的 [Instance.mainTag] 已经不是主武器 NBT 里的那一份了。
        val target = gun.attachment.getOrCreateTag(instance.slot)
        if (target == host) return

        clearTag(target)
        target.merge(host)

        // 附件内容变了：先立起 `mutated`（否则 `save()` 会以"没改过"直接返回），
        // 再由主武器把整份 tag 写进物品栈 —— revision 的推进与客户端同步都在 `persist` 里完成。
        gun.invalidateProperties()
        gun.save()
    }

    /** 清空 [compound] 的全部键（1.21 的 [CompoundTag] 没有 `clear()`） */
    private fun clearTag(compound: CompoundTag) {
        for (key in compound.allKeys.toList()) {
            compound.remove(key)
        }
    }

    // ------------------------------------------------------------------ tick

    /**
     * 主武器 tick 时顺带 tick 全部副武器。
     *
     * 两个前提前提都必须成立：
     * - **只在服务端**推进（客户端的副武器状态跟着主武器 tag 同步过来，自己再推会打架）；
     * - 主武器自己**不是副武器**（否则一把副武器上再装副武器会无限递归）。
     *
     * `inMainHand = true`：对这把副武器来说，"正在操作它"就是主武器的持有状态 ——
     * 换弹/拉栓/栓动这些只在 `inMainHand` 分支里跑的流程必须走到。
     */
    @JvmStatic
    fun tick(shooter: Entity?, gun: GunData) {
        if (shooter == null) return
        if (shooter.level().isClientSide) return
        if (gun.item is SubWeaponItem) return
        // 便宜的前置过滤：一个配件都没装的枪（绝大多数）直接跳过装配流程
        if (gun.attachmentTag.isEmpty) return

        val instances = installed(gun)
        if (instances.isEmpty()) return

        // 诊断：每把主武器只打一次，用来确认"副武器的 tick 到底有没有在跑"。
        // 没有这一行 = `SubWeaponRuntime.tick` 根本没被调用（换弹就永远不会开始）。
        if (loggedTickSeen.add(gun.uuid?.toString() ?: gun.id)) {
            debug { "tick: driving ${instances.map { it.slotName }} for ${gun.id}" }
        }

        for (instance in instances) {
            // 诊断：换弹是"先 markStart、下一 tick 由 gunTick 消费"的两段式。
            // 看到这一行就说明副武器的 tick 确实在跑、也看到了待处理的换弹请求。
            if (instance.data.reload.reloadStarter.shouldStart()) {
                debug { "tick: picked up pending reload starter for ${instance.slotName}" }
            }
            GunEventHandler.gunTick(shooter, instance.data, inMainHand = true)
            // 1.21：合成栈与主武器 NBT 是两份数据，推进完必须显式折回去（见 [syncBack]）
            syncBack(gun, instance)
        }
    }

    /**
     * 副武器**必须**能按物品注册 id 解析到 `sbw/guns/<id>.json`。
     *
     * 解析不到时 [GunData.getDefault] 会退回一份空的 [com.atsuishio.superbwarfare.data.gun.DefaultGunData]：
     * `Magazine = 0` → `useBackpackAmmo()` 为真 → `tryStartReload` 第一行就返回（**永远装不了弹**），
     * 同时 `ProjectileAmount = 0` → `canShoot` 恒为假（**永远开不了火**）。
     * 表现是"按 G 完全没反应"，所以这里必须吼一声，不能让它静默。
     */
    private fun warnIfNoBaseline(instance: Instance) {
        if (!instance.data.getDefault().isDefaultData) return
        if (!warnedMissingBaseline.add(instance.attachmentId.toString())) return

        Mod.LOGGER.error(
            "[SubWeapon] '{}' has no matching gun data; GunData fell back to an empty baseline " +
                    "(Magazine=0, ProjectileAmount=0), so it can neither fire nor reload. " +
                    "Expected a file at data/<namespace>/sbw/guns/{}.json " +
                    "(or set SubWeapon.Data to an existing gun data id).",
            instance.attachmentId, instance.attachmentId.path,
        )
    }

    /** 已经打过"tick 到了"日志的主武器 uuid，只在诊断时用一次 */
    private val loggedTickSeen = HashSet<String>()

    private inline fun debug(message: () -> String) {
        if (DisplayConfig.MELEE_DEBUG_LOG.get()) {
            Mod.LOGGER.info("[SubWeapon] {}", message())
        }
    }

    @JvmStatic
    fun debugEnabled(): Boolean = DisplayConfig.MELEE_DEBUG_LOG.get()

    // ------------------------------------------------------------------ 缓存

    private fun cacheOf(gun: GunData): MutableMap<AttachmentType, Instance> {
        if (CACHE.size > MAX_CACHED_GUNS) {
            CACHE.clear()
        }
        return CACHE.getOrPut(gun) { HashMap() }
    }

    /** 主武器丢失/换枪/卸载配件时清掉它的条目（目前只在调试与资源重载时用得上） */
    @JvmStatic
    fun clear(gun: GunData) {
        CACHE.remove(gun)
    }

    @JvmStatic
    fun clearAll() {
        CACHE.clear()
    }
}
