package com.atsuishio.superbwarfare.data.gun.subweapon

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.attachment.SubWeaponInfo
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.GunState
import com.atsuishio.superbwarfare.data.gun.subweapon.SubWeaponRuntime.BY_UUID
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.data.gun.value.ReloadState
import com.atsuishio.superbwarfare.data.stack.GunStackStorage
import com.atsuishio.superbwarfare.event.GunEventHandler
import com.atsuishio.superbwarfare.item.attachment.SubWeaponItem
import com.atsuishio.superbwarfare.tools.playLocalSound
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraftforge.registries.ForgeRegistries
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 副武器的运行时
 */
object SubWeaponRuntime {

    /**
     * 一把已装配的副武器
     */
    class Instance(
        val slot: AttachmentType,
        val attachmentId: ResourceLocation,
        val info: SubWeaponInfo,
        val clientSide: Boolean,
        val stack: ItemStack,
        val carrier: GunStackStorage.Carrier,
        val data: GunData,
    ) {
        /** 上一 tick 是否在装填 */
        var wasReloading: Boolean = false

        /** 报文与冷却键里用的槽位标识 */
        val slotName: String get() = slot.name

        /** 副武器实际使用的枪数据 id */
        val baselineId: String get() = baselineIdOf(attachmentId, info)

        /** 根 tag（活引用）；等价于老的 `liveTag` */
        val root: CompoundTag get() = carrier.root

        /** 载体令牌（不变式 ② 的判据） */
        val token: Long get() = carrier.token
    }

    /** 按 `(客户端?, 主武器 UUID)` 缓存；主武器还没拿到 UUID 时退回按 [GunData] 身份缓存 */
    private data class CacheKey(val clientSide: Boolean, val uuid: UUID)

    /**
     * 缓存键的一半是"哪一侧"，取值本身是线程安全的（单人游戏里客户端线程与服务端线程
     * 会同时读写这张表；桶内则只会被对应那一侧碰）。
     */
    private val BY_UUID = ConcurrentHashMap<CacheKey, MutableMap<AttachmentType, Instance>>()

    /** 没有 UUID 时的兜底表；同样按侧分开（单人游戏里客户端与服务端是两个 `GunData`） */
    private val BY_IDENTITY = arrayOf(
        WeakHashMap<GunData, MutableMap<AttachmentType, Instance>>(),
        WeakHashMap<GunData, MutableMap<AttachmentType, Instance>>(),
    )

    /** [BY_UUID] 的软上限：超过就整体丢弃（重建成本很低，比无界增长好） */
    private const val MAX_CACHED_GUNS = 256

    /** 已经吼过的"缺枪数据"id，避免每 tick 刷屏 */
    private val warnedMissingBaseline: MutableSet<String> = ConcurrentHashMap.newKeySet()

    // ------------------------------------------------------------------ 装配

    /**
     * 解析主武器上装着的全部副武器
     */
    @JvmStatic
    fun installed(gun: GunData, client: Boolean): List<Instance> {
        val cache = cacheOf(gun, client)
        val found = LinkedHashMap<AttachmentType, Instance>()

        for (attachment in gun.attachment.installed()) {
            val info = attachment.definition.subWeapon ?: continue
            val item = ForgeRegistries.ITEMS.getValue(attachment.id) as? SubWeaponItem ?: continue

            val cached = cache[attachment.slot]

            val cachedRoot = cached?.root
            val incoming = if (cachedRoot != null && carriesGunState(cachedRoot)) {
                gun.attachment.getTagIfCompound(attachment.slot)
            } else {
                gun.attachment.getOrCreateTag(attachment.slot)
            }

            val instance = if (cached != null && cached.attachmentId == attachment.id) {
                if (cached.data.defaultDataId.get() != baselineIdOf(attachment.id, info)) {
                    applyBaselineId(cached.stack, attachment.id, info)
                    cached.data.pullFromTag()
                    debug { "baseline of ${attachment.slot} -> ${cached.baselineId}" }
                }

                var folded = false

                if (incoming !== null && incoming !== cached.root) {
                    if (foldIncoming(cached.root, incoming, acceptEqualRevision = client)) {
                        folded = true
                    }
                    gun.attachment.setTag(attachment.slot, cached.root)
                } else if (incoming == null) {
                    gun.attachment.setTag(attachment.slot, cached.root)
                }

                val revision = revisionOf(cached.root)
                if (folded || revision != cached.data.state.revision) {
                    cached.data.pullFromTag()
                }
                cached
            } else {
                val root = incoming ?: gun.attachment.getOrCreateTag(attachment.slot)
                assemble(attachment.slot, attachment.id, info, item, client, root, GunStackStorage.tokenOf(root))
            }

            found[attachment.slot] = instance
        }

        cache.keys.retainAll(found.keys)
        cache.putAll(found)
        return found.values.toList()
    }

    /** 按槽位找一把副武器；`null` = 这个槽位没装副武器 */
    @JvmStatic
    fun find(gun: GunData, slot: AttachmentType, client: Boolean): Instance? =
        installed(gun, client).firstOrNull { it.slot == slot }

    /** 按报文里的槽位标识找（`SUBWEAPON` 这样的枚举名） */
    @JvmStatic
    fun find(gun: GunData, slotName: String, client: Boolean): Instance? =
        installed(gun, client).firstOrNull { it.slotName == slotName }

    private fun assemble(
        slot: AttachmentType,
        attachmentId: ResourceLocation,
        info: SubWeaponInfo,
        item: SubWeaponItem,
        client: Boolean,
        root: CompoundTag,
        token: Long,
    ): Instance {
        val stack = ItemStack(item, 1, root)
        GunData.stackStorage.writeRoot(stack, root)

        // ⚠ 必须在 `GunData.from(stack)` **之前**：`GunData` 构造时就把 `defaultDataId` 解码进状态
        applyBaselineId(stack, attachmentId, info)

        val carrier = GunStackStorage.Carrier(root, token)
        val instance = Instance(slot, attachmentId, info, client, stack, carrier, GunData.from(stack))
        warnIfNoBaseline(instance)

        return instance
    }

    /** 副武器实际使用的枪数据 id：`SubWeapon.Data` 非空就用它，否则回落到附件自己的注册 id */
    @JvmStatic
    fun baselineIdOf(attachmentId: ResourceLocation, info: SubWeaponInfo): String =
        info.data?.takeIf { it.isNotBlank() } ?: attachmentId.toString()

    /**
     * 把枪数据 id 写进合成栈（`SubWeaponInfo.Data` 的落地）
     */
    private fun applyBaselineId(stack: ItemStack, attachmentId: ResourceLocation, info: SubWeaponInfo) {
        GunData.setDefaultDataId(stack, baselineIdOf(attachmentId, info))
    }

    private fun foldIncoming(
        target: CompoundTag,
        source: CompoundTag,
        acceptEqualRevision: Boolean,
    ): Boolean {
        if (target === source) return false
        if (target == source) return false

        if (!target.isEmpty && !carriesGunState(source)) return false

        val sourceRevision = revisionOf(source)
        val targetRevision = revisionOf(target)
        if (sourceRevision < targetRevision) return false
        if (!acceptEqualRevision && !GunState.isNewerRevision(sourceRevision, targetRevision)) return false

        mergePreservingIdentity(target, source)

        return true
    }

    private fun mergePreservingIdentity(target: CompoundTag, source: CompoundTag) {
        // source 里没有的键，target 里不能留
        for (key in target.allKeys.toList()) {
            if (!source.contains(key)) target.remove(key)
        }

        // 两边都是 compound 就递归下去，否则整体替换
        for (key in source.allKeys) {
            val incoming = source.get(key) ?: continue
            val existing = target.get(key)
            if (incoming is CompoundTag && existing is CompoundTag) {
                mergePreservingIdentity(existing, incoming)
            } else {
                target.put(key, incoming.copy())
            }
        }
    }

    /** 根 tag 里的枪械状态子 tag；`null` = 这个槽位还没被装配过 */
    private fun gunStateOf(tag: CompoundTag): CompoundTag? =
        if (tag.contains(GunState.KEY_GUN_DATA, Tag.TAG_COMPOUND.toInt())) {
            tag.getCompound(GunState.KEY_GUN_DATA)
        } else {
            null
        }

    /** 根 tag 里记录的枪械状态 revision（没有状态就是 0） */
    private fun revisionOf(tag: CompoundTag): Long =
        gunStateOf(tag)?.getLong(GunState.KEY_REVISION) ?: 0L

    private fun carriesGunState(tag: CompoundTag): Boolean = gunStateOf(tag) != null

    // ------------------------------------------------------------------ tick

    /**
     * 主武器 tick 时顺带 tick 全部副武器
     */
    @JvmStatic
    @JvmOverloads
    fun tick(shooter: Entity?, gun: GunData, deployedSlot: AttachmentType? = null, inMainHand: Boolean = true) {
        if (shooter == null) return
        if (shooter.level().isClientSide) return
        if (gun.item is SubWeaponItem) return
        if (gun.attachmentTag.isEmpty) return

        val instances = installed(gun, client = false)
        if (instances.isEmpty()) return

        for (instance in instances) {
            val sub = instance.data
            val reloadingBefore = instance.wasReloading

            // 这把副武器是不是"当前操控的枪"：宿主枪在手上 **且** 切出来的就是它。
            // 只有这时装填/栓动的进度才往前走 —— 与主武器 `if (inMainHand)` 完全同一条口径。
            val operated = inMainHand && deployedSlot == instance.slot

            // ⓪ 没被操控：**中断装填**（进度归零），而不是让它在背包里自己走完。
            if (!operated) {
                interruptReload(sub)
            }

            // ① 推进状态机：入参就是"这把副武器有没有被操控" —— 装填/栓动只在被操控时走
            GunEventHandler.gunTick(shooter, sub, inMainHand = operated)

            // ② 处理"开始 / 结束"这两个跳变（被中断的那一次不算"完成"）
            val reloadingNow = sub.reloading()
            when {
                !operated && reloadingBefore ->
                    debug { "reload of ${instance.slotName} interrupted (no longer the active gun)" }

                !reloadingBefore && reloadingNow -> onReloadStarted(shooter, instance)
                reloadingBefore && !reloadingNow -> onReloadFinished(shooter, instance)
            }
            instance.wasReloading = reloadingNow
        }
    }

    /**
     * 中断装填：换弹状态与计时器清干净，栓动计时器一起清
     */
    @JvmStatic
    fun interruptReload(data: GunData) {
        if (!data.reloading() && data.bolt.actionTimer.get() == 0) return

        data.reload.setTime(0)
        data.reload.setState(ReloadState.NOT_RELOADING)

        // 单发装填（`ReloadTypes: ["Iterative"]`）自己的阶段计时器也要一起清，否则会留下半截状态
        if (data.get(GunProp.ITERATIVE_TIME) != 0) {
            data.stopped.set(false)
            data.forceStop.set(false)
            data.reload.setStage(0)
            data.reload.prepareTimer.reset()
            data.reload.prepareLoadTimer.reset()
            data.reload.iterativeLoadTimer.reset()
            data.reload.finishTimer.reset()
        }

        if (data.get(GunProp.BOLT_ACTION_TIME) > 0) {
            data.bolt.actionTimer.reset()
        }

        data.invalidateProperties()
    }

    /**
     * 换弹开始的那一瞬间
     */
    private fun onReloadStarted(shooter: Entity?, instance: Instance) {
        debug { "reload started: ${instance.slotName}" }

        val player = shooter as? ServerPlayer ?: return
        val sound = instance.info.reloadSound ?: return
        player.playLocalSound(sound, RELOAD_SOUND_VOLUME, 1f)
    }

    /**
     * 换弹结束的那一瞬间
     */
    private fun onReloadFinished(shooter: Entity?, instance: Instance) {
        val sub = instance.data
        val ammo = sub.ammo.get()

        debug { "reload finished: ${instance.slotName} ammo=$ammo/${sub.get(GunProp.MAGAZINE)}" }

        val player = shooter as? ServerPlayer ?: return
        instance.info.reloadEndSound?.let { player.playLocalSound(it, RELOAD_SOUND_VOLUME, 1f) }
    }

    /** 副武器换弹音效的音量（本地音，只有射手听得到） */
    private const val RELOAD_SOUND_VOLUME = 1.0f

    /**
     * 副武器**必须**能按 [Instance.baselineId] 解析到 `sbw/guns/<id>.json`
     */
    private fun warnIfNoBaseline(instance: Instance) {
        if (!instance.data.getDefault().isDefaultData) return
        if (!warnedMissingBaseline.add(instance.baselineId)) return

        Mod.LOGGER.error(
            "[SubWeapon] '{}' has no matching gun data at sbw/guns/{}.json; GunData fell back to an empty " +
                    "baseline (Magazine=0, ProjectileAmount=0), so it can neither fire nor reload. " +
                    "Either ship that file or point SubWeapon.Data at an existing gun data id.",
            instance.attachmentId, instance.baselineId.substringAfter(':'),
        )
    }

    private inline fun debug(message: () -> String) {
        if (DisplayConfig.MELEE_DEBUG_LOG.get()) {
            Mod.LOGGER.info("[SubWeapon] {}", message())
        }
    }

    @JvmStatic
    fun debugEnabled(): Boolean = DisplayConfig.MELEE_DEBUG_LOG.get()

    // ------------------------------------------------------------------ 缓存

    private fun cacheOf(gun: GunData, client: Boolean): MutableMap<AttachmentType, Instance> {
        val uuid = gun.uuid ?: return BY_IDENTITY[if (client) 1 else 0].getOrPut(gun) { HashMap() }

        if (BY_UUID.size > MAX_CACHED_GUNS) {
            BY_UUID.clear()
        }
        return BY_UUID.computeIfAbsent(CacheKey(client, uuid)) { HashMap() }
    }

    /** 主武器丢失/换枪/卸载配件时清掉它的条目（目前只在调试与资源重载时用得上） */
    @JvmStatic
    fun clear(gun: GunData) {
        val uuid = gun.uuid
        if (uuid != null) {
            BY_UUID.remove(CacheKey(false, uuid))
            BY_UUID.remove(CacheKey(true, uuid))
        }
        BY_IDENTITY.forEach { it.remove(gun) }
    }

    @JvmStatic
    fun clearAll() {
        BY_UUID.clear()
        BY_IDENTITY.forEach { it.clear() }
    }
}
