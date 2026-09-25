package com.atsuishio.superbwarfare.client.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.gun.MeleeClientHandler.MELEE_SAFE_LOCK_TICKS
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
     * 本次 tick 的近战入口。
     *
     * @param stack                 主手物品
     * @param meleeKeyDown          近战键（V）是否按下
     * @param subWeaponFireKeyDown  副武器键（G）是否按下
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
        val item = stack.item as? GunItem ?: return
        if (!GunItem.isHeldWeapon(stack)) return

        val data = GunData.from(stack)
        val state = GunActionLock.of(data)

        // 服务端权威状态跟着一起进锁：换弹/拉栓期间其它入口必须被拒
        syncServerDrivenLocks(data, state)

        if (!skipStateTick) state.tick()

        if (state.meleeTicks > 0) {
            tickActiveSwing(player, data, state)
            return
        }

        // G 键：装了副武器就用副武器（遍历逐个触发），没装才等同 V。
        // `tryTrigger` 返回 true = 这次 G 已被副武器消费（含"全在冷却"的反馈），不再落到近战。
        val fromSubWeaponKey = subWeaponFireKeyDown && !meleeKeyDown
        if (fromSubWeaponKey && SubWeaponClientHandler.tryTrigger(player, data, state)) return

        val wantsMelee = meleeKeyDown || fromSubWeaponKey || (data.meleeOnly() && holdingFireKey)
        if (!wantsMelee) return

        if (!item.hasMeleeAttack(data)) return
        if (!canOperate) return
        if (drawTime >= 0.01) return
        if (state.isLocked) return
        if (data.reloading() || data.charging() || data.bolt.actionTimer.get() > 0) return
        if (data.reload.normal() || data.reload.empty()) return

        triggerSwing(player, data, state, fromSubWeaponKey)
    }

    /**
     * 把服务端权威的换弹/拉栓状态同步进动作锁，让"换弹时挥砍"这类边界被统一拒掉。
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
        fromSubWeaponKey: Boolean,
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
        state.lastSwingFromSubWeaponKey = fromSubWeaponKey

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
                    "hitTime=${action.hitTime} comboWindow=$inComboWindow key=${if (fromSubWeaponKey) "G" else "V"}"
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
