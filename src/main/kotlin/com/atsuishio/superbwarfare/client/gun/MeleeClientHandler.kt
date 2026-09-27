package com.atsuishio.superbwarfare.client.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.gun.MeleeClientHandler.MELEE_SAFE_LOCK_TICKS
import com.atsuishio.superbwarfare.client.gun.MeleeClientHandler.syncServerDrivenLocks
import com.atsuishio.superbwarfare.client.gun.MeleeClientHandler.tick
import com.atsuishio.superbwarfare.command.MeleeDebugHooks
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.melee.MeleeHitboxType
import com.atsuishio.superbwarfare.data.gun.melee.ResolvedMeleeAction
import com.atsuishio.superbwarfare.data.gun.subdata.Cooldown
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.send.MeleeAttackMessage
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime
import com.atsuishio.superbwarfare.tools.ActiveGun
import com.atsuishio.superbwarfare.tools.MeleeQuery
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/**
 * 客户端近战执行器：输入判定 → 连招下标锁存 → 动作锁 → 按本段 `HitTime` 结算 → 发报文。
 *
 * 玩法时间线：
 * ```
 * t=0        触发：锁存 actionIndex，播 swing 音效，meleeTimer = action.Duration
 * t=HitTime  结算：客户端判定 → 发报文；服务端结算伤害/效果/命中音效
 * t=Duration meleeTimer 归零，允许下一段（按 MeleeComboReset 决定是否重置连招）
 * ```
 *
 * **下标在挥击开始时锁存**：动画状态机只在状态切换那一帧解析 clip 名
 * （`GeoGunAnimationInstance.resolveState`），中途改下标会让动画和判定对不上。
 *
 * **长按 V 连续挥击**是旧版的有意设计，这里保留：动作锁在本段结束后才释放，
 * 所以按住不放的节奏 = 本段 `Duration`，而不是"每 tick 挥一次"。
 */
object MeleeClientHandler {

    /**
     * 挥击序号：每触发一次挥击 +1。
     *
     * **为什么需要它**：动画状态机只在**状态切换**那一帧解析 clip 名并重建 runner
     * （`GeoGunAnimationInstance.tick` 的 `currentState != target` 分支）。按住 V 连续挥击时，
     * 上一段还没结束就进了下一段，`currentState` 与 `target` 都还是 `MELEE`
     * ——状态没变，动画就不会重播，一直停在上一段的最后一帧（`PLAY_ONCE_HOLD`）。
     *
     * 所以给每次挥击发一个单调递增的序号，动画侧按"序号涨了就重播"处理
     * （与开火那套 `fireSerial` / `consumedFireSerial` 完全同一个套路）。
     */
    var swingSerial: Int = 0
        private set

    /**
     * 上一 tick G（副武器切换键）是否按下。
     *
     * 切换只认按键的**上升沿**（按住 G 不该连续发请求）。这个标记必须在 [tick] 的**最开头**更新，
     * 否则任何一次提前 return 都会让它停在 `true`，下一次真正按下就被当成"一直按着"。
     */
    private var subWeaponKeyWasDown: Boolean = false

    /**
     * 本次 tick 的输入处理入口。
     *
     * ## 四期起这里同时管 G（主/副武器切换）
     *
     * **G = 在主武器与副武器之间切换**（§9.8.2），不再是"用一次副武器"。
     * ⚠ **G 与近战完全解耦**：没有副武器时按 G 是**空操作**，不会落回近战 ——
     * **V 是唯一的近战入口**（三期那条"没有副武器时 G 等同 V"已废除）。
     *
     * ## ⚠ 近战恒用**主手**那把枪（四期最容易改错的地方）
     *
     * 近战的动画驱动与判定几何体都是**主武器**的（副武器只是挂在 `sub_weapon_pos` 上的挂件，
     * 它没有也拿不到自己的判定体），所以下文的 `stack` / `data` / `item` **一律保持主手**，
     * **不能**换成 `ActiveGun`。副武器激活时按 V 的正确行为是：
     * 主武器做近战动作、主武器结算伤害，副武器挂在枪上不动。
     *
     * @param stack                 主手物品
     * @param meleeKeyDown          近战键（V）是否按下
     * @param subWeaponFireKeyDown  副武器切换键（G）是否按下
     * @param holdingFireKey        开火键是否按住（`meleeOnly` 枪按住左键也进近战）
     * @param drawTime              切枪进度；未完成时不接受输入
     * @param canOperate            载具禁手 / 非游戏内 / 改装界面等外部门禁是否通过
     * @param skipStateTick         调用方是否已经推进过动作锁计时（避免一 tick 走两步）
     */
    fun tick(
        player: Player,
        stack: ItemStack,
        meleeKeyDown: Boolean,
        subWeaponFireKeyDown: Boolean,
        holdingFireKey: Boolean,
        drawTime: Double,
        canOperate: Boolean,
        skipStateTick: Boolean = false,
    ) {
        // G 的**上升沿**要在所有提前 return 之前算出来：只要有一帧没更新这个标记，
        // 后面松开/再按就会被误判成"一直按着"。
        val subWeaponJustPressed = subWeaponFireKeyDown && !subWeaponKeyWasDown
        subWeaponKeyWasDown = subWeaponFireKeyDown

        // 切换请求的兜底超时也要在任何提前 return 之前推进（丢一次包不该把 G 永久锁死）
        SubWeaponClientHandler.tick(data = null)

        val item = stack.item as? GunItem ?: return
        if (!GunItem.isOperable(stack)) return

        val data = GunData.from(stack)
        val state = GunActionLock.of(data)

        // ⚠ **"现在忙不忙"必须看当前操控的枪，不能看主手。**（四期返修，见下面的门禁）
        //
        // 部署着副武器时换弹的是**副武器**，而主武器自己的换弹在部署那一刻就被中断了
        // （`ActiveGun.deploy` → `SubWeaponRuntime.interruptReload`），之后也不会重新开始
        // （宿主枪被顶替期间不走 `gunTickInternal` 的 `inMainHand` 分支）。
        // 于是只查主手的话，下面每一条门禁都会放行 —— 表现就是**副武器换弹期间能近战**。
        //
        // 未部署时 `operated === data`（同一个栈 → 同一个 `GunData` 实例），
        // 所以这一改写对三期行为**逐字等价**，22 把旧枪不受影响。
        val operated = ActiveGun.dataOf(data, player.level().isClientSide)

        // 服务端权威状态跟着一起进锁：换弹/拉栓期间其它入口必须被拒
        syncServerDrivenLocks(operated, state)

        if (!skipStateTick) state.tick()

        if (state.meleeTicks > 0) {
            tickActiveSwing(player, data, state)
            return
        }

        // G 键：**在主武器与副武器之间切换**（四期）。
        //
        // ⚠ **G 只负责切换，与近战完全解耦**（§9.8.2）：
        // - 当前操控的是主武器 → 切到枚举顺序里的第一个副武器槽位；
        // - 当前操控的是副武器   → 切回主武器；
        // - 一把副武器都没装     → **什么都不做**（不挥砍、不播音、不进动作锁）。
        //
        // 三期那条"没有副武器就落回近战入口（等同 V）"的语义**已废除** ——
        // V 是唯一的近战入口，G 永远不会触发近战。所以这里的 `return` 是无条件的，
        // 不能被 `request` 的返回值决定（它现在也不再有"没切成"这个返回值）。
        //
        // 切换本身**不在这里生效**：请求发给服务端，服务端写 `ActiveSlot` 并回确认，
        // 客户端收到确认才演切换动作（`SubWeaponDeployedMessage`）。
        val fromSubWeaponKey = subWeaponFireKeyDown && !meleeKeyDown
        if (fromSubWeaponKey && subWeaponJustPressed) {
            val deployed = ActiveGun.activeSlot(data)
            val target = if (deployed != null) {
                null // 已经是副武器 → 切回主武器
            } else {
                SubWeaponRuntime.installed(data, client = true).firstOrNull()?.slot
            }

            SubWeaponClientHandler.request(player, data, target)
            return
        }

        // V 是唯一的近战入口（外加"近战型枪按住左键"这一条独立路径）
        val wantsMelee = meleeKeyDown || (data.meleeOnly() && holdingFireKey)
        if (!wantsMelee) return

        if (!item.hasMeleeAttack(data)) return
        if (!canOperate) return
        if (drawTime >= 0.01) return
        if (state.isLocked) return
        // ⚠ [state] 是**主手那把枪**的动作锁（近战进度记在它上面），但"别人的占用"要两把都查：
        // 切换确认之后 `SUB_WEAPON` 是占在**副武器**那把锁上的（`onDeployed` 用
        // `ActiveGun.dataOf(player)` 猜的时长），而 `FIRING` 也占在操控的那把上。
        // 未部署时两者是同一个对象，这一句在那种情况下不增加任何限制。
        if (GunActionLock.of(operated).blocks(GunAction.MELEE)) return
        // ⚠ 查的是 [operated]（当前操控的枪）而不是主手：副武器换弹时主武器自己并没有在换弹
        if (operated.busyForMelee()) return

        triggerSwing(player, data, state)
    }

    /**
     * 这把枪现在**正忙**（换弹 / 拉栓 / 蓄力），近战入口必须被拒。
     *
     * 与 [syncServerDrivenLocks] 是**两层**：那层把服务端权威状态写进动作锁（`state.isLocked`），
     * 这层是同一 tick 内的直接判定。两层都查是刻意留的冗余 —— 锁里那条是"上一 tick 的状态"，
     * 而玩家可能正好在换弹开始的那一 tick 按下 V。
     */
    private fun GunData.busyForMelee(): Boolean =
        reloading() || charging() || bolt.actionTimer.get() > 0 || reload.normal() || reload.empty()

    /**
     * 把服务端权威的换弹/拉栓状态同步进动作锁，让"换弹时挥砍"这类边界被统一拒掉。
     *
     * ⚠ **[data] 传的是当前操控的枪**（`ActiveGun`），不是主手那把：副武器换弹时
     * 主武器自己并没有在换弹，传主手就等于把这条门禁整个绕开（见调用处）。
     * 锁本身仍然挂在主手那把枪上（近战是它的能力），这里只是借它表达"玩家现在忙"。
     *
     * 这两个动作的真实时长由服务端状态机决定，所以这里：
     * - 状态**为真**时用 [MELEE_SAFE_LOCK_TICKS] 兜底占位（真正释放靠下一 tick 的同步）；
     * - 状态**转假**时立刻释放，避免"换弹完了但锁还挂着"。
     */
    private fun syncServerDrivenLocks(data: GunData, state: GunActionLock.State) {
        val reloading = data.reloading()
        val bolting = data.bolt.actionTimer.get() > 0

        if (!reloading && state.activeAction == GunAction.RELOADING) {
            state.release(GunAction.RELOADING)
        }
        if (!bolting && state.activeAction == GunAction.BOLTING) {
            state.release(GunAction.BOLTING)
        }

        if (state.isLocked) return
        if (reloading) {
            state.acquire(GunAction.RELOADING, MELEE_SAFE_LOCK_TICKS)
        } else if (bolting) {
            state.acquire(GunAction.BOLTING, MELEE_SAFE_LOCK_TICKS)
        }
    }

    private fun triggerSwing(
        player: Player,
        data: GunData,
        state: GunActionLock.State,
    ) {
        // ① 连招下标：窗口内再挥击进下一段，超时回到第 0 段
        val actions = data.meleeActions()
        val comboReset = data.get(GunProp.MELEE_COMBO_RESET)
        val inComboWindow = state.sinceLastMelee < comboReset
        val index = if (inComboWindow) (state.meleeActionIndex + 1) % actions.size else 0

        val action = data.resolveMeleeAction(index)

        // ② 本段冷却
        if (action.cooldown > 0 && data.cooldown.isCoolingDown(Cooldown.meleeKey(index))) return

        // ③ 动作占用（时长 = 本段 Duration）
        if (!state.acquire(GunAction.MELEE, action.duration)) return

        state.meleeActionIndex = index
        state.meleeDuration = action.duration
        state.meleeTicks = action.duration
        state.meleeHitResolved = false

        // ⑥ 通知动画侧"新的一次挥击开始了"。按住 V 连续挥击时状态一直是 MELEE，
        //    光靠状态切换判断的话动画只会播第一段（见 [swingSerial] 的说明）。
        swingSerial++

        if (action.cooldown > 0) {
            data.cooldown.set(Cooldown.meleeKey(index), action.cooldown)
        }

        // ④ 挥击音效（客户端本地播放；动作级 Swing 覆盖枪的 MeleeSound.Swing）
        player.playSound(action.swing ?: data.get(GunProp.MELEE_SOUND).swing, 1f, 1f)

        // ⑤ 与旧的帧驱动开火冷却对齐：挥击期间不该顺手打出一发
        ClientEventHandler.fireCooldown = (action.duration + 4).toDouble()

        debugLog {
            "melee swing: gun=${data.id} index=$index duration=${action.duration} " +
                    "hitTime=${action.hitTime} comboWindow=$inComboWindow"
        }
    }

    /**
     * 推进当前这一段的计时，到 `HitTime` 那一 tick 结算。
     *
     * **时序必须与旧实现逐 tick 一致**（`gunMelee == duration - damageTime` 那一帧出伤）：
     * [GunActionLock.State.tick] 已经在**本帧开头**把 `meleeTicks` 递减过一次，
     * 所以这里判的是 `<= duration - hitTime`，而**不是** `<= hitTime`
     * （写成 `<= hitTime` 会让出伤整整晚 1 tick：`hitTime=6` 变成第 11 tick 才打）。
     */
    private fun tickActiveSwing(player: Player, data: GunData, state: GunActionLock.State) {
        if (state.meleeHitResolved) return

        val action = data.resolveMeleeAction(state.meleeActionIndex)
        if (state.meleeTicks > action.hitTickFromStart) return

        state.meleeHitResolved = true
        resolveHit(player, data, action, state.meleeActionIndex)
    }

    private fun resolveHit(
        player: Player,
        data: GunData,
        action: ResolvedMeleeAction,
        actionIndex: Int,
    ) {
        val context = MeleeQuery.contextOf(player, action)

        // 开 debug 日志时走带诊断的版本：能直接看到"候选是谁、卡在哪一步"
        val diag = if (DisplayConfig.MELEE_DEBUG_LOG.get()) {
            MeleeQuery.resolveDiag(player.level(), player, action, context)
        } else {
            null
        }
        val hits = diag?.hits ?: MeleeQuery.resolve(player.level(), player, action, context)

        // 缺陷 3：`player.swing` 双端各调一次会让一次近战触发两次 `onEntitySwing`。
        //   现在只在**客户端**这里调一次（服务端不再 swing），第三人称动作照旧。
        player.swing(InteractionHand.MAIN_HAND)

        sendPacketToServer(
            MeleeAttackMessage(
                source = MeleeAttackMessage.SOURCE_MAIN,
                actionIndex = actionIndex,
                targets = hits.map { it.toPayload() },
            )
        )

        if (diag != null) {
            debugLog { describeMeleeResult(action, context, hits, diag) }
        }
    }

    /**
     * 一次近战判定的日志：命中列表 + **每个候选卡在哪一步**。
     *
     * 排查"盒子明明罩住了却打不到"时，光看命中数是没用的，必须能看到
     * `shape`（夹角/距离不满足）还是 `occlusion`（被方块挡住）。
     */
    private fun describeMeleeResult(
        action: ResolvedMeleeAction,
        context: MeleeQuery.Context,
        hits: List<MeleeQuery.Hit>,
        diag: MeleeQuery.DiagResult,
    ): String = buildString {
        append("melee hitbox=").append(action.hitbox.type)
        append(" reach=").append("%.2f".format(context.reach))
        append(" occlusion=").append(action.hitbox.occlusion)
        append(" -> hits=").append(hits.size)

        for (hit in hits) {
            append(" [HIT ").append(hit.entity.type)
            append(" d=%.2f a=%.1f".format(hit.distance, hit.angle))
            if (hit.aimed) append(" AIM")
            if (hit.headshot) append(" HEAD")
            if (hit.legshot) append(" LEG")
            append(']')
        }

        for (c in diag.candidates) {
            if (c.reason == MeleeQuery.RejectReason.HIT) continue
            append(" [").append(c.reason.label).append(' ').append(c.entity.type)
            append(" d=%.2f a=%.1f".format(c.distance, c.angle))
            if (c.reason == MeleeQuery.RejectReason.SHAPE && action.hitbox.type == MeleeHitboxType.CONE) {
                append(" dyaw=%.1f/%.1f".format(c.yawDelta, action.hitbox.angle / 2))
                append(" dpitch=%.1f/%.1f".format(c.pitchDelta, action.hitbox.pitch / 2))
            }
            append(']')
        }

        if (diag.candidates.isEmpty()) append(" [no entity passed the coarse filter]")
    }

    private fun MeleeQuery.Hit.toPayload() = MeleeAttackMessage.TargetPayload(
        uuid = entity.uuid,
        // 报的是**命中区域判定点**（准星射线到该 AABB 的最近点），服务端按它算打头/打腿
        hitX = zonePos.x,
        hitY = zonePos.y,
        hitZ = zonePos.z,
        distance = distance,
        aimed = aimed,
    )

    /** 换弹/拉栓占用的"安全上限"：真正的释放由状态机自己结束，这里只是别让锁永远占着 */
    private const val MELEE_SAFE_LOCK_TICKS = 400

    private inline fun debugLog(message: () -> String) {
        if (DisplayConfig.MELEE_DEBUG_LOG.get()) {
            Mod.LOGGER.info("[Melee] {}", message())
        }
    }

    /** 载具禁手门禁 */
    @JvmStatic
    fun canOperateWeapon(player: Player): Boolean {
        val vehicle = player.vehicle
        return !(vehicle is VehicleEntity && vehicle.banHand(player))
    }

    /**
     * 装上 `/sbw melee force` 的客户端实现。
     *
     * 命令是服务端注册的，判定只能在客户端做，所以两边靠 [MeleeDebugHooks] 这个挂点对接。
     */
    @JvmStatic
    fun installDebugHooks() {
        MeleeDebugHooks.install { player, data, index ->
            // 本地也把下标锁存过去，让动画跟着切到被 force 的那一段
            val state = GunActionLock.of(data)
            state.meleeActionIndex = index
            state.meleeDuration = data.resolveMeleeAction(index).duration

            forceSwing(player, data, index)
        }
    }

    /**
     * 立刻以第 [index] 段动作结算一次近战。
     *
     * 不经过输入判定与动作锁（这是调试入口），只发报文给服务端结算，
     * **不改**本地的挥击计时/连招下标，方便反复调同一段。
     */
    @JvmStatic
    fun forceSwing(player: Player, data: GunData, index: Int) {
        val action = data.resolveMeleeAction(index)
        val context = MeleeQuery.contextOf(player, action)
        val hits = MeleeQuery.resolve(player.level(), player, action, context)

        player.swing(InteractionHand.MAIN_HAND)
        sendPacketToServer(
            MeleeAttackMessage(
                source = MeleeAttackMessage.SOURCE_MAIN,
                actionIndex = index,
                targets = hits.map { it.toPayload() },
            )
        )

        debugLog {
            "forced melee: index=$index targets=${hits.size} duration=${action.duration} hitTime=${action.hitTime}"
        }
    }
}
