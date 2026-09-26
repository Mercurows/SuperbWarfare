package com.atsuishio.superbwarfare.subweapon

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.attachment.SubWeaponInfo
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.GunState
import com.atsuishio.superbwarfare.data.gun.subdata.Cooldown
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.event.GunEventHandler
import com.atsuishio.superbwarfare.item.attachment.SubWeaponItem
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime.BY_UUID
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime.tick
import com.atsuishio.superbwarfare.tools.playLocalSound
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraftforge.registries.ForgeRegistries
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 副武器的运行时。
 *
 * 「寄生 GunData」的全部要点都在这里：
 *
 * | 要素 | 做法 |
 * |---|---|
 * | 物品 | 副武器物品**自己**（`SubWeaponItem : GunItem, AttachmentProvider`） |
 * | 合成栈 | `ItemStack(subWeaponItem, 1, liveTag)` —— tag 就是主武器 NBT 里那个附件子 tag 的**活引用** |
 * | 数据基线 | 默认按物品注册 id 解析（`sbw/guns/<id>.json` 与配件同名成对出现）；`SubWeaponInfo.Data` 非空时才覆盖 |
 * | 状态 | 弹药/热量/换弹/耐久/revision 全部写在这份共享 tag 上 → **随主武器 NBT 持久化**，无新存档字段 |
 * | 实例身份 | 缓存表（见 [BY_UUID]）持有合成栈的强引用，否则会掉出 `GunData.DATA_CACHE`（weakKeys） |
 * | tick | 合成栈不在背包里，`GunItem.inventoryTick` 不会跑 → 主武器 gun tick 里顺带 tick（见 [tick]） |
 * | 开火 | 服务端装配合成栈 → `GunData.shoot(...)`，与主武器**同一个入口** |
 * | 装填 | 服务端 `tryStartReload`（主武器自动装填的同一个入口）+ 主武器那套 `gunTick` 状态机 |
 *
 * **客户端与服务端用同一套装配规则**：`GunData` 的同步会把主武器整份 tag 推给客户端，
 * 附件子 tag 也在里面，所以两边都能解出同一份副武器状态。
 *
 * ## 三条不变量（这个类坏过的每一次都踩在这里）
 *
 * **① 一份 tag 只能有一个 [Instance] / 一个 `GunData`。**
 * `GunData.state` 是"解码一次就缓存"的镜像，`save()` 会把**整份**状态写回 tag；
 * 两个实例同时写同一份 tag 就会互相把对方的改动盖回去 ——
 * 表现是"装填走完提示了却没装进去""按一下 G 装填计时被打回满值""像是两条装填并行在跑"。
 * 所以缓存命中判定必须稳，重建必须罕见（见 ②）。
 *
 * **② tag 引用必须全程不变。**
 * `GunData` 构造时把根 tag 与它的子 compound 全部捕获成 `val`，所以 tag 一换，
 * 这个 [Instance] 就永久脱钩了。而主武器的 `GunData.rebind` 走 `clearTag + merge`：
 * 被清空的键会以 `tag.copy()` 重新落进去（`CompoundTag.merge` 对"原来不存在"的键是复制），
 * **每次 resync 之后附件子 tag 都是新实例**。
 * 早期版本因此每 tick 重建一次 `GunData`，两个实例抢着写同一份状态，于是：
 * 装填完成被覆盖、装填音效重复播、按住 G 一直响 —— 全是这一个原因。
 * 现在遇到副本不再重建，而是把新内容折进手里那份再把手里那份**挂回槽位**
 * （[Attachment.setTag]），引用、`GunData`、[Instance] 三者全程不变。
 *
 * **③ 客户端和服务端各自一份缓存。**
 * 单人游戏里两边在同一个 JVM、同一份静态表，主武器 UUID 也相同；
 * 混用一张表会让两边轮流把对方的 [Instance] 挤掉（每次挤掉都重置下面的
 * [Instance.wasReloading]，于是装填音效又响一次）。所以缓存按 `clientSide` 分开。
 */
object SubWeaponRuntime {

    /**
     * 一把已装配的副武器。
     *
     * @param slot 它在主武器上的槽位（也是报文里用的标识：[slotName]）
     * @param attachmentId 配件数据 id（与 [stack] 的 tag 里的 `Id` 对应）
     * @param info 配件上的 `SubWeapon` 定义
     * @param clientSide 这份实例属于哪一侧（缓存键的一半，见不变式 ③）
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
        val clientSide: Boolean,
        val stack: ItemStack,
        val liveTag: CompoundTag,
        val data: GunData,
    ) {
        /**
         * 上一 tick 是否在装填。
         *
         * 用来捕捉"装填开始/结束"这**一瞬间**（跳变）—— 完成提示、"一发都没装进去"的退避、
         * 以及换弹音效都挂在这个跳变上。没有它就只能在装填期间每 tick 猜。
         *
         * ⚠ 这是**跨 tick 的记忆**，也是"实例必须复用"的另一个理由：实例一换，
         * 这个标记退回 `false`，新实例第一次 tick 看到的恰好是"正在装填"，
         * 于是又报一次"装填开始" —— 听感就是按住 G 时换弹音效一直响。
         */
        var wasReloading: Boolean = false

        /**
         * 自动装填的退避计时。
         *
         * 上一次装填**一发都没装进去**（备弹是缓存值、可能已经过期）时置上：
         * 否则下一 tick `shouldStartReloading` 又为真 → 又装一次 → 又装不进 →
         * 玩家会看到"装填走了一遍又一遍，就是打不出去"。
         */
        var autoReloadBackoff: Int = 0

        /** 报文与冷却键里用的槽位标识 */
        val slotName: String get() = slot.name

        /** 主武器冷却表上的键（`sub:<slot>`） */
        val cooldownKey: String get() = Cooldown.subWeaponKey(slotName)

        /**
         * 触发冷却 tick：**一律按副武器数据里的 `RPM` 自动算**（`1200 / RPM`），与主武器开火同一个口径。
         *
         * 配件数据里没有单独的冷却字段：一把武器"多快"只应该在枪数据里写一次。
         * 至少 1 tick（`rpm <= 0` 会让下次触发立刻通过，等于没有冷却）。
         */
        fun cooldownTicks(): Int {
            val rpm = data.get(GunProp.RPM).coerceAtLeast(1)
            return (1200 / rpm).coerceAtLeast(1)
        }
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
     * 解析主武器上装着的全部副武器（按槽位去重，顺序 = `AttachmentType.entries`）。
     *
     * 只有**同时满足**这三条的槽位才算副武器：
     * 1. 该槽位装了配件，且配件的定义能解析出来；
     * 2. 定义里有 [SubWeaponInfo]；
     * 3. 对应物品是 [SubWeaponItem]（否则拿不到 `GunData`）。
     *
     * @param client 调用方在哪一侧。**必须显式传**：单人游戏里客户端与服务端共享静态表，
     *   用错一侧的实例会让状态在两边互相覆盖（见类 KDoc 的不变式 ③）。
     */
    @JvmStatic
    fun installed(gun: GunData, client: Boolean): List<Instance> {
        val cache = cacheOf(gun, client)
        val found = LinkedHashMap<AttachmentType, Instance>()

        for (attachment in gun.attachment.installed()) {
            val info = attachment.definition.subWeapon ?: continue
            val item = ForgeRegistries.ITEMS.getValue(attachment.id) as? SubWeaponItem ?: continue

            // ⚠ 必须用 `getOrCreateTag`，不能用 `AttachmentInstance.tag`。
            //
            // `Attachment.getTag()` 对**字符串形式**的槽位内容返回的是
            // `CompoundTag().apply { putString("Id", ...) }` —— **每次调用都是一个新对象**。
            // 拿它当合成栈的根 tag，会同时踩两个坑：
            //   1. 副武器的状态写进一个游离的临时 compound，**永远回不到主武器 NBT**；
            //   2. 引用对不上，下面的缓存永远不命中。
            // `getOrCreateTag` 会把该槽位实体化成 compound **并写回枪 NBT**，
            // 之后 `getCompound` 返回的就是同一个活引用。
            val incoming = gun.attachment.getOrCreateTag(attachment.slot)
            val cached = cache[attachment.slot]

            // 装配规则只有一条：**只要还是同一个配件、而且我们手里那份 tag 里有枪械状态，
            // 就永远复用同一个 [Instance]**（必要时把槽位里那份挂回槽位）。
            // 其余情况都是"没得复用"：槽位从没装配过 / 换成了别的副武器 / 手里那份本来就没状态。
            val instance = if (cached != null && cached.attachmentId == attachment.id && carriesGunState(cached.liveTag)) {
                if (cached.liveTag !== incoming) {
                    // 主武器 rebind 过：附件子 tag 被换成了**副本**（见类 KDoc 的不变式 ②）。
                    // 把副本折进手里那份，再把手里那份挂回槽位 ——
                    // tag 引用、`GunData`、[Instance] 三者全程不变。
                    //
                    // **客户端那份是权威视图，无条件折**：`GunState.revision` 只在枪械状态变化时推进，
                    // 拿它当"内容变了"的信号会漏掉真正的同步内容，客户端就会一直拿着装配那一刻的
                    // 弹药/装填状态（曾经让"正在装填"也判定成"能开火"）。
                    // 服务端手里那份才是权威，所以只在副本确实更新时才折，免得被旧快照倒回去。
                    val folded = foldIncoming(cached.liveTag, incoming, force = client)
                    gun.attachment.setTag(attachment.slot, cached.liveTag)

                    // 折进来了新内容就**必须重新解码**：`GunData.state` 是"构造时解码一次"的镜像，
                    // 只靠本实例自己的写入跟进（`pullFromTag` 见 `GunData`）。
                    if (folded) cached.data.pullFromTag()

                    debug {
                        "re-attached the live tag of ${attachment.slot} -> ${attachment.id} " +
                                "(folded=$folded)"
                    }
                }
                cached
            } else {
                assemble(attachment.slot, attachment.id, info, item, client, incoming)
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

    /**
     * 真正新建一份 [Instance]：合成栈必须**原样持有** [liveTag]。
     *
     * 不假设 `ItemStack(ItemLike, int, CompoundTag)` 一定原样持有这份 tag：
     * 只要拿回来的不是同一个对象，就显式覆盖回去。
     * `GunData` 在构造时会把根 tag 与它的子 compound 全部**捕获成 val**，
     * 一旦栈里挂的是副本，副武器的所有状态都会写进一个和主武器 NBT 无关的角落
     * —— 症状就是"换弹启动了但计时器永远停在原地、下次读又是 0"。
     */
    private fun assemble(
        slot: AttachmentType,
        attachmentId: ResourceLocation,
        info: SubWeaponInfo,
        item: SubWeaponItem,
        client: Boolean,
        liveTag: CompoundTag,
    ): Instance {
        val stack = ItemStack(item, 1, liveTag)
        if (stack.tag !== liveTag) {
            stack.tag = liveTag
        }

        val instance = Instance(slot, attachmentId, info, client, stack, liveTag, GunData.from(stack))
        warnIfNoBaseline(instance)

        return instance
    }

    /**
     * 把 [source] 折进 [target]（先清空再 merge，与 `GunData.reloadTagFrom` 同一套做法）。
     *
     * @param force `true` = 只要内容不同就折（客户端：同步过来的那份就是权威视图）。
     *   `false` = 只在 [source] 确实更新（`GunState.revision` 更大）时才折 ——
     *   服务端手里那份才是权威，副本可能来自一份更旧的快照（主武器被另一个栈 rebind 时带进来的旧 NBT），
     *   无条件覆盖会把副武器的弹药/装填进度倒回去。
     * @return 是否真的折了内容（折了的话调用方**必须**让对应的 `GunData` 重新解码状态，
     *   见 [GunData.pullFromTag]）
     */
    private fun foldIncoming(target: CompoundTag, source: CompoundTag, force: Boolean): Boolean {
        if (target === source) return false
        if (target == source) return false
        if (!force && !GunState.isNewerRevision(revisionOf(source), revisionOf(target))) return false

        for (key in target.allKeys.toList()) {
            target.remove(key)
        }
        target.merge(source)

        return true
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
     * 主武器 tick 时顺带 tick 全部副武器。
     *
     * 三个前提都必须成立：
     * - **只在服务端**推进（客户端的副武器状态跟着主武器 tag 同步过来，自己再推会打架）；
     * - 主武器自己**不是副武器**（否则一把副武器上再装副武器会无限递归）；
     * - 主武器身上**确实有附件**（便宜的前置过滤，绝大多数枪直接跳过装配流程）。
     *
     * **状态推进与"自动装填 / 提示"分开**：
     * - 进度、栓动、热量这些**每把枪都推**（`inMainHand` 无关）——
     *   一旦绑在 `inMainHand` 上，那个判定为假时副武器的状态机就会被整段冻住
     *   （换弹计时器永远停在同一 tick、`canShoot` 永远 false）；
     * - **自动装填、音效、动作栏提示只在 `inMainHand` 时做** ——
     *   挂在背包里的枪不该自己吃备弹、也不该给玩家弹提示。
     *
     * @param inMainHand 这把主武器是不是正被持有（`GunEventHandler.gunTickInternal` 的入参）
     */
    @JvmStatic
    fun tick(shooter: Entity?, gun: GunData, inMainHand: Boolean = true) {
        if (shooter == null) return
        if (shooter.level().isClientSide) return
        if (gun.item is SubWeaponItem) return
        if (gun.attachmentTag.isEmpty) return

        val instances = installed(gun, client = false)
        if (instances.isEmpty()) return

        // 诊断：每把主武器只打一次，用来确认"副武器的 tick 到底有没有在跑"。
        if (loggedTickSeen.add(gun.uuid?.toString() ?: gun.id)) {
            debug { "tick: driving ${instances.map { it.slotName }} for ${gun.id} (mainHand=$inMainHand)" }
        }

        for (instance in instances) {
            val sub = instance.data
            val reloadingBefore = instance.wasReloading

            // ① 自动装填：判定与主武器 `autoReload` 同一个谓词，入口是主武器的 `tryStartReload`
            //    （它会自己拒掉"正在装填 / 正在拉栓 / 计时器没归零 / 没有备弹 / 弹匣是满的"）。
            //    **必须在 `gunTick` 之前调用**：换弹是两段式的（`tryStartReload` 只 markStart，
            //    真正的状态切换在下一 tick 的 `gunTick` 里），先 mark 再 tick 就能在本 tick 内起步。
            if (inMainHand) {
                if (instance.autoReloadBackoff > 0) {
                    instance.autoReloadBackoff--
                } else if (sub.shouldStartReloading(shooter)) {
                    GunEventHandler.tryStartReload(shooter, sub, save = false)
                }
            }

            // ② 推进状态机（与持有状态无关：已经在走的换弹必须能走完）
            GunEventHandler.gunTick(shooter, sub, inMainHand = true)

            // ③ 处理"开始 / 结束"这两个跳变
            val reloadingNow = sub.reloading()
            if (!reloadingBefore && reloadingNow) {
                onReloadStarted(shooter, instance)
            } else if (reloadingBefore && !reloadingNow) {
                onReloadFinished(shooter, instance)
            }
            instance.wasReloading = reloadingNow

            // ④ 装填进度提示（临时方案：动作栏文字；以后换成 HUD）
            if (inMainHand) {
                showReloadingProgress(shooter, instance)
            }
        }
    }

    /**
     * 换弹开始的那一瞬间。
     *
     * 音效走**配件数据自己的** `ReloadSound`：主武器的换弹音效是动画关键帧发的，
     * 配件没有动画，所以副武器必须自己声明一次（不写就是不发声）。
     *
     * 播放用 `playLocalSound`（`SoundTool` → `ClientboundSoundPacket`，只发给射手）：
     * 它支持 `Holder.Direct`，所以数据包里按名字写的音效（`SoundEventSerializer` 造的、
     * 不在音效注册表里的那种）同样能发给客户端。
     *
     * 这个回调只应该在"真的开始了一次装填"时响一次 —— 它重复触发意味着实例/状态被重建过
     * （见类 KDoc 的不变式 ①②）。
     */
    private fun onReloadStarted(shooter: Entity?, instance: Instance) {
        val player = shooter as? ServerPlayer ?: return
        val sound = instance.info.reloadSound ?: return
        player.playLocalSound(sound, RELOAD_SOUND_VOLUME, 1f)
        debug { "reload started: ${instance.slotName}" }
    }

    /**
     * 换弹结束的那一瞬间：播完成音效 + 发**绿色**完成提示；一发都没装进去就退避一会儿。
     *
     * 副武器不做动画的话，"自动装填"整个过程是**不可见**的 ——
     * 玩家只会看到"过了一小会儿又能打一发"，像是白送了一发。音效 + 提示把这个过程讲清楚。
     */
    private fun onReloadFinished(shooter: Entity?, instance: Instance) {
        val sub = instance.data
        val ammo = sub.ammo.get()

        // 一发都没装进去（备弹是缓存值、可能已经过期）：退避，别每 tick 重启装填。
        if (ammo <= 0) {
            instance.autoReloadBackoff = AUTO_RELOAD_BACKOFF
            debug { "reload finished with an empty magazine, backing off" }
        }

        val player = shooter as? ServerPlayer ?: return

        // 装填"走完了"不等于"装进去了"：没装进弹药时给红字，别报成完成
        if (ammo <= 0) {
            player.displayClientMessage(
                Component.translatable(
                    "info.superbwarfare.subweapon.reload_empty",
                    Component.translatable(sub.stack.descriptionId),
                ).withStyle(ChatFormatting.RED),
                true,
            )
            return
        }

        instance.info.reloadEndSound?.let { player.playLocalSound(it, RELOAD_SOUND_VOLUME, 1f) }

        player.displayClientMessage(
            Component.translatable(
                "info.superbwarfare.subweapon.reloaded",
                Component.translatable(sub.stack.descriptionId),
                ammo,
            ).withStyle(ChatFormatting.GREEN),
            // true = 动作栏，不刷聊天框
            true,
        )

        debug { "reload finished: ${instance.slotName} ammo=$ammo/${sub.get(GunProp.MAGAZINE)}" }
    }

    /**
     * 装填期间给玩家发一条动作栏提示。
     *
     * **临时的**：需求方之后会把它做成 HUD。所以这里只做两件事 ——
     * 判断"正在装填"、按固定间隔节流（`RELOAD_HINT_INTERVAL` tick 一次，
     * 既不会每 tick 刷包，看起来也是连续进度）。
     */
    private fun showReloadingProgress(shooter: Entity?, instance: Instance) {
        val player = shooter as? ServerPlayer ?: return
        val sub = instance.data
        if (!sub.reloading()) return
        if (shooter.tickCount % RELOAD_HINT_INTERVAL != 0) return

        val total = sub.reload.totalTicks.get().coerceAtLeast(1)
        val remaining = sub.reload.time().coerceIn(0, total)
        val percent = ((total - remaining) * 100 / total).coerceIn(0, 100)

        player.displayClientMessage(
            Component.translatable(
                "info.superbwarfare.subweapon.reloading",
                Component.translatable(sub.stack.descriptionId),
                percent,
            ),
            // true = 动作栏，不刷聊天框
            true,
        )
    }

    /** 装填提示的刷新间隔（tick）；4 tick ≈ 每秒 5 次，够平滑又不刷包 */
    private const val RELOAD_HINT_INTERVAL = 4

    /** 一次"没装进任何弹药"的装填之后，自动装填要退避多少 tick */
    private const val AUTO_RELOAD_BACKOFF = 40

    /** 副武器换弹音效的音量（本地音，只有射手听得到） */
    private const val RELOAD_SOUND_VOLUME = 1.0f

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
    private val loggedTickSeen: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private inline fun debug(message: () -> String) {
        if (DisplayConfig.MELEE_DEBUG_LOG.get()) {
            Mod.LOGGER.info("[SubWeapon] {}", message())
        }
    }

    @JvmStatic
    fun debugEnabled(): Boolean = DisplayConfig.MELEE_DEBUG_LOG.get()

    // ------------------------------------------------------------------ 缓存

    private fun cacheOf(gun: GunData, client: Boolean): MutableMap<AttachmentType, Instance> {
        val uuid = gun.uuid
        if (uuid == null) {
            return BY_IDENTITY[if (client) 1 else 0].getOrPut(gun) { HashMap() }
        }

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
