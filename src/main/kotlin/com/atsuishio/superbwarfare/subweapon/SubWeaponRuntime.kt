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
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime.BY_UUID
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime.tick
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraftforge.registries.ForgeRegistries
import java.util.*

/**
 * 副武器的运行时。
 *
 * 「寄生 GunData」的全部要点都在这里：
 *
 * | 要素 | 做法 |
 * |---|---|
 * | 物品 | 副武器物品**自己**（`SubWeaponItem : GunItem, AttachmentProvider`） |
 * | 合成栈 | `ItemStack(subWeaponItem, 1, attachment.tag)` —— tag 就是主武器 NBT 里那个附件子 tag 的**活引用** |
 * | 数据基线 | 默认按物品注册 id 解析（`sbw/guns/<id>.json` 与配件同名成对出现）；`SubWeaponInfo.Data` 非空时才覆盖 |
 * | 状态 | 弹药/热量/换弹/耐久/revision 全部写在这份共享 tag 上 → **随主武器 NBT 持久化**，无新存档字段 |
 * | 实例身份 | [CACHE] 持有合成栈的强引用，否则会掉出 `GunData.DATA_CACHE`（weakKeys） |
 * | tick | 合成栈不在背包里，`GunItem.inventoryTick` 不会跑 → 主武器 gun tick 里顺带 tick（见 [tick]） |
 * | 开火 | 服务端装配合成栈 → `GunData.shoot(...)`，与主武器**同一个入口** |
 *
 * **客户端与服务端用同一套装配规则**：`GunData` 的同步会把主武器整份 tag 推给客户端，
 * 附件子 tag 也在里面，所以两边都能解出同一份副武器状态。
 *
 * ⚠ **tag 实例会在客户端 resync 时被换掉**：`GunData.rebind` 走 `clearTag + merge`，
 * 被清空的键会以 `tag.copy()` 重新落进去（`CompoundTag.merge` 对"原来不存在"的键是复制）。
 * 所以缓存必须按 **tag 引用**校验，换掉了就重建 —— 否则客户端会一直读一份已经和主武器脱钩的旧 tag。
 */
object SubWeaponRuntime {

    /**
     * 一把已装配的副武器。
     *
     * @param slot 它在主武器上的槽位（也是报文里用的标识：[slotName]）
     * @param attachmentId 配件数据 id（与 [stack] 的 tag 里的 `Id` 对应）
     * @param info 配件上的 `SubWeapon` 定义
     * @param stack 合成栈；持有强引用是**有意的**（见类的 KDoc）
     * @param liveTag 合成栈根 tag 的引用。
     *   **不能靠 `stack.tag` 反查**：缓存命中判定必须比较这一份我们亲手存下来的引用，
     *   否则 `ItemStack` 内部一旦不是"原样持有"（复制 / 规整 / 被 `GunData` 换过），
     *   判定就会永远为假 —— 表现为每 tick 重建一次 `GunData`，副武器状态机被反复清零。
     * @param data 合成栈对应的枪械数据
     */
    class Instance(
        val slot: AttachmentType,
        val attachmentId: ResourceLocation,
        val info: SubWeaponInfo,
        val stack: ItemStack,
        val liveTag: CompoundTag,
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

    /** 按 `(主武器 UUID, 槽位)` 缓存；主武器还没拿到 UUID 时退回按 [GunData] 身份缓存 */
    private val BY_UUID = HashMap<UUID, MutableMap<AttachmentType, Instance>>()
    private val BY_IDENTITY = WeakHashMap<GunData, MutableMap<AttachmentType, Instance>>()

    /** [BY_UUID] 的软上限：超过就整体丢弃（重建成本很低，比无界增长好） */
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
            val item = ForgeRegistries.ITEMS.getValue(attachment.id) as? SubWeaponItem ?: continue

            // ⚠ 必须用 `getOrCreateTag`，不能用 `AttachmentInstance.tag`。
            //
            // `Attachment.installed()` 里的 tag 来自 `Attachment.getTag()`，它对**字符串形式**的
            // 槽位内容是 `CompoundTag().apply { putString("Id", ...) }` —— **每次调用都是一个新对象**。
            // 拿它当合成栈的根 tag，会同时踩两个坑：
            //   1. 副武器的状态写进一个游离的临时 compound，**永远回不到主武器 NBT**（换弹计时器每 tick 归零）；
            //   2. 引用对不上，下面的缓存永远不命中，每次都新建一个 `GunData`。
            // 表现就是"按 G 完全没反应 / 卡在同一个 tick 不动"。
            // `getOrCreateTag` 会把该槽位实体化成 compound **并写回枪 NBT**，
            // 之后 `getCompound` 返回的就是同一个活引用。
            val liveTag = gun.attachment.getOrCreateTag(attachment.slot)

            val cached = cache[attachment.slot]
            // 命中条件只看两件事：**还是同一个配件** + **还是同一份 tag 引用**。
            // 后者比较的是我们自己存进 [Instance] 的那份引用，而不是 `stack.tag` 反查出来的值 ——
            // 实测 `ItemStack` 并不保证"传进去什么就原样还回来"，用反查会让判定永远为假。
            val reusable = cached != null
                    && cached.attachmentId == attachment.id
                    && cached.liveTag === liveTag

            found[attachment.slot] = if (reusable) {
                cached
            } else {
                val stack = ItemStack(item, 1, liveTag)
                // 不假设 `ItemStack(ItemLike, int, CompoundTag)` 一定原样持有这份 tag：
                // 只要拿回来的不是同一个对象，就显式覆盖回去。
                // `GunData` 在构造时会把根 tag 与它的子 compound 全部**捕获成 val**，
                // 一旦栈里挂的是副本，副武器的所有状态都会写进一个和主武器 NBT 无关的角落
                // —— 症状就是"换弹启动了但计时器永远停在原地、下次读又是 0"。
                if (stack.tag !== liveTag) {
                    stack.tag = liveTag
                }

                Instance(attachment.slot, attachment.id, info, stack, liveTag, GunData.from(stack))
                    .also { warnIfNoBaseline(it) }
            }

            // 缓存没命中时同时换掉了旧实例，保证同一个合成 tag 上**只有一个** GunData 在写：
            // `GunData.state` 是"解码一次就缓存"的镜像，两个实例会互相把对方的改动覆盖回去。
            if (!reusable && debugEnabled()) {
                debug {
                    "assembled ${attachment.slot} -> ${attachment.id}: cached=${cached != null} " +
                            "idMatch=${cached?.attachmentId == attachment.id} " +
                            "tagMatch=${cached?.liveTag === liveTag} " +
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
        val uuid = gun.uuid
        if (uuid == null) return BY_IDENTITY.getOrPut(gun) { HashMap() }

        if (BY_UUID.size > MAX_CACHED_GUNS) {
            BY_UUID.clear()
        }
        return BY_UUID.getOrPut(uuid) { HashMap() }
    }

    /** 主武器丢失/换枪/卸载配件时清掉它的条目（目前只在调试与资源重载时用得上） */
    @JvmStatic
    fun clear(gun: GunData) {
        gun.uuid?.let { BY_UUID.remove(it) }
        BY_IDENTITY.remove(gun)
    }

    @JvmStatic
    fun clearAll() {
        BY_UUID.clear()
        BY_IDENTITY.clear()
    }
}
