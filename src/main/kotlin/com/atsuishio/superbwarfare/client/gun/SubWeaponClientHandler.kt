package com.atsuishio.superbwarfare.client.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.animation.gun.GeoGunAnimationInstance
import com.atsuishio.superbwarfare.client.gun.SubWeaponClientHandler.burstRemaining
import com.atsuishio.superbwarfare.client.gun.SubWeaponClientHandler.tick
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.FireMode
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.send.SubWeaponFireMessage
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player

/**
 * 客户端副武器触发（G 键）。
 *
 * 语义表：
 *
 * | 情况 | V | G |
 * |---|---|---|
 * | 什么都没装 | 主武器自身近战 | **等同 V**（本类返回 `false`，交回近战入口） |
 * | 只装刺刀（无 `SubWeapon`） | 刺刀动作 | **等同 V** |
 * | 只装副武器 | 主武器自身近战 | **使用副武器** |
 * | 刺刀 + 副武器共存 | 刺刀动作 | **使用副武器** |
 *
 * 「多个副武器」不做优先级 —— **遍历一次、逐个触发**：
 * 冷却中跳过、剩下的进 [SubWeaponFireMessage.slots]；
 * 动作占用取所有实际触发者里最长的一个，避免"遍历触发"被动作锁逐个拦掉。
 *
 * ## 扣扳机的方式由副武器**自己的**开火模式决定
 *
 * | 模式 | 按 G |
 * |---|---|
 * | `Semi`（默认） | 一次按键一发 |
 * | `Auto` | **按住就连发**，节奏由冷却表（服务端每发写 `1200 / RPM`）决定 |
 * | `Burst` | 一次按键打 `BurstAmount` 发，打完要再按一次；**松开 G 之后剩下的也会打完**（与主武器一致） |
 * | `Hold` / `Charge` | 副武器没有蓄力输入链路，按半自动处理（一次按键一发） |
 *
 * 模式读的是**副武器自己那份 `GunData`**（`AvailableFireModes` / `DefaultFireMode` 都在它自己的
 * 枪数据里），与主武器的开火模式互不影响。
 *
 * ## 客户端不决定"打没打出去"
 *
 * 这里只表达意图：挑出这一次 G 要操作的槽位、上动作锁给个节奏、发报文。
 * **开火音与开火动画都不在这里播** —— 服务端在真的打出这一发之后才会把 1P 音效参数
 * （`playLocalSound`）和开火表现（`SubWeaponFiredMessage`）发过来，客户端收到才演。
 * 这样"装填中 / 空仓 / 冷却中按 G 却响了一声、演了一遍"在结构上不可能发生。
 * 冷却也由服务端写（写在主武器冷却表上），客户端只读 —— 单一事实来源，
 * 免得两边各写一次导致"谁都不动"的死角。
 *
 * 全部副武器都在冷却时：只播一声 `trigger_click` 反馈，不产生其它副作用
 * （这时**不会**落到近战 —— 手上明明有副武器却挥了一刀是最容易误伤的设计）。
 */
object SubWeaponClientHandler {

    /** 装填占用的兜底时长（真正的释放靠服务端状态同步，见 `MeleeClientHandler.syncServerDrivenLocks`） */
    private const val MIN_RELOAD_LOCK_TICKS = 20

    /** 副武器枪口焰窗口的起点（与主武器 `fireRotTimer = 0.001` 同一个写法，见 `MuzzleFlashRenderer`） */
    private const val SUB_WEAPON_FLASH_START = 0.001

    /**
     * 本次"扣扳机"还要点射出去几发（`Burst`）。
     *
     * 与主武器的 `ClientEventHandler.burstFireAmount` 同一个思路，区别是它由 [tick] 推进 ——
     * **松开 G 之后剩余的点射也要打完**，只活在"按住的那些 tick"里的话，点一下 G 只会出一发。
     */
    private var burstRemaining: Int = 0

    /** [burstRemaining] 属于哪把枪：换枪之后不该接着打（`GunData` 的身份在同步之间是稳定的） */
    private var burstOwner: GunData? = null

    /**
     * 尝试用副武器消费这次 G。
     *
     * **首批触发只在按键上升沿**（[justPressed]）；按住不放的后续 tick 只有**能连发的模式**
     * （`Auto`，或还没打完的 `Burst`）才会继续触发，其余模式什么都不做但也**吞掉整个按键**
     * （既不连发，也不掉到近战）。
     *
     * **G 只负责"开火"**：装填是服务端自动做的（`SubWeaponRuntime.tick` 里
     * `shouldStartReloading` → `tryStartReload`），客户端不再发"请装填"的请求。
     *
     * @param justPressed 本次 tick 是不是"刚按下"（由 `MeleeClientHandler` 做边沿检测）
     * @return `true` = 本次 G 已经归副武器（无论成功开火，还是只给了一声"没反应"的反馈）；
     *   `false` = 主武器上**没有任何副武器**，调用方应当把 G 当成 V 走近战入口。
     */
    @JvmStatic
    fun tryTrigger(
        player: Player,
        data: GunData,
        state: GunActionLock.State,
        justPressed: Boolean = true,
    ): Boolean {
        val instances = SubWeaponRuntime.installed(data, client = true)
        if (instances.isEmpty()) return false

        if (justPressed) {
            // 新的一次扣扳机：点射从满发开始算
            burstOwner = data
            burstRemaining = burstAmountOf(instances)
        } else if (!holdsFire(instances)) {
            // 半自动/蓄力：按住不放的后续 tick 什么都不做（G 仍然整个归副武器）
            return true
        }

        // 动作被占用 / 主武器自己在换弹拉栓时什么都不做
        if (state.isLocked) return true
        if (data.reloading() || data.charging() || data.bolt.actionTimer.get() > 0) return true

        fire(player, data, state, instances, repeat = !justPressed, feedback = justPressed)
        return true
    }

    /**
     * 每客户端 tick 推进一次（由 `ClientEventHandler.handleGunMelee` 调用，**与 G 是否按下无关**）。
     *
     * 只做一件事：把松开 G 之后没打完的点射走完。没有待打完的点射时它就是一个提前 return。
     */
    @JvmStatic
    fun tick(player: Player, data: GunData, state: GunActionLock.State, keyDown: Boolean) {
        if (burstRemaining <= 0 || burstOwner !== data) return
        // 按住时由 [tryTrigger] 的连发路径负责，这里插一脚会变成一 tick 双发
        if (keyDown) return
        if (state.isLocked) return
        if (data.reloading() || data.charging() || data.bolt.actionTimer.get() > 0) return

        fire(
            player,
            data,
            state,
            SubWeaponRuntime.installed(data, client = true),
            repeat = true,
            feedback = false,
        )
    }

    /**
     * 副武器开火的**第一人称表现**：换开火动画 + 把枪口焰交给副武器。
     *
     * **由服务端确认后触发**（`SubWeaponFiredMessage` 报文）：这一发真的打出去了才播 ——
     * 与 1P 开火音同一个口径。客户端按下 G 的那一刻不再自己播，所以"装填期间按 G 也演一遍
     * 开火动画"不可能发生（动画是纯表现，没有判定会兜底）。
     *
     * **动画名由配件数据说了算**（`SubWeaponInfo.Animation`，不写就是 `["fire_sub_weapon"]`）：
     * 短名按宿主枪 id 拼成 `animation.ak_12.fire_sub_weapon`，候选里第一个存在的 clip 胜出，
     * 全部落空就退回宿主枪自己的 `GunAnimation.Fire` —— "给某把枪做了副武器开火动画就用它、
     * 没做就照常播 `fire`"由一条数据表达，与刺刀的动作候选链（`MeleeAction.Animation`）同一套机制。
     *
     * 枪口焰一并交给副武器（`ClientEventHandler.subWeaponFireRotTimer`）：这期间主武器的
     * `flare` 骨骼一帧都不画，改画副武器模型自己的 `flare`（见 `MuzzleFlashRenderer`）。
     */
    @JvmStatic
    fun playFireAnimation(player: Player, instance: SubWeaponRuntime.Instance) {
        val info = instance.info

        (FirstPersonRenderHandler.getActiveAnimationInstance(InteractionHand.MAIN_HAND) as? GeoGunAnimationInstance)
            ?.triggerFire(player.mainHandItem, info.fireAnimationCandidates(), info.hasExplicitFireAnimation)

        ClientEventHandler.subWeaponFireRotTimer = SUB_WEAPON_FLASH_START
    }

    /**
     * 真正触发一次：挑出这次要操作的槽位 → 占用动作锁 → 发报文。
     *
     * **节奏完全交给冷却表**：服务端每发按 `1200 / RPM` 写一次（写在主武器 NBT 上，同步很及时），
     * 连发时每 tick 都走到这里，冷却没走完的槽位自然被过滤掉 —— 客户端不自己算射速，
     * 免得两边各算一次出现"比服务端还快"或者"谁都不动"。
     *
     * @param repeat `true` = 按住/点射的后续触发：只认**能连发的**槽位，并且会跳过客户端已经知道
     *   "正在装填/拉栓"的槽位（少发几个注定会被拒的报文）。
     *   `false` = 刚扣下扳机：所有槽位都算，而且**刻意不看客户端那份副武器状态** ——
     *   它是同步过来的、永远慢一拍，拿它当门禁会出现"服务端明明已经装好了、客户端却判定打不出去、
     *   连报文都不发"（表现就是"装填结束了按 G 什么都没发生"）。
     *   开火与否交给服务端一个人拍板（`SubWeaponFireMessage` 里那次 `canShoot`）。
     * @param feedback 什么都没触发时是否给一声"没反应"的反馈（只在刚扣扳机时为真；
     *   连发节奏里的空窗每 tick 响一声就没法听了）
     */
    private fun fire(
        player: Player,
        data: GunData,
        state: GunActionLock.State,
        instances: List<SubWeaponRuntime.Instance>,
        repeat: Boolean,
        feedback: Boolean,
    ) {
        val triggered = ArrayList<SubWeaponRuntime.Instance>(instances.size)
        var lockTicks = 0

        for (instance in instances) {
            if (repeat) {
                if (!instance.repeatsWhileHeld()) continue
                // 客户端已经知道它在装填/拉栓：不必再刷报文（这里判错最多是晚一 tick 再打，
                // 不会像"首次触发"那样把真正打出去的那一发吞掉）
                if (instance.data.reloading() || instance.data.bolt.actionTimer.get() > 0) continue
            }

            if (data.cooldown.isCoolingDown(instance.cooldownKey)) {
                debug { "subweapon ${instance.slotName} skipped: cooling down" }
                continue
            }

            triggered += instance
            lockTicks = maxOf(lockTicks, instance.cooldownTicks())
        }

        if (triggered.isEmpty()) {
            if (!feedback) return

            debug { "subweapon: every installed sub-weapon is on cooldown" }
            // 占一小段锁：避免同一帧内被别的入口重复触发
            state.acquire(GunAction.SUB_WEAPON, MIN_RELOAD_LOCK_TICKS)
            player.playSound(ModSounds.TRIGGER_CLICK.get(), 1f, 1f)
            return
        }

        state.acquire(GunAction.SUB_WEAPON, lockTicks.coerceAtLeast(1))
        if (burstRemaining > 0) {
            burstRemaining--
        }

        sendPacketToServer(
            SubWeaponFireMessage(
                slots = triggered.map { it.slotName },
                spread = ClientEventHandler.gunSpread,
                zoom = ClientEventHandler.zoom,
                uuid = ClientEventHandler.lockedEntity?.uuid,
            )
        )

        debug { "subweapon trigger: slots=${triggered.map { it.slotName }} lock=$lockTicks gun=${data.id}" }
    }

    /**
     * 按住 G 期间这个槽位还要不要继续触发。
     *
     * - `Auto`：连发，冷却走完就打；
     * - `Burst`：把这次扣扳机剩下的点射打完（打完就停，要再按一次才会开新的一轮）；
     * - `Semi` 与蓄力类（`Hold`/`Charge`）：一次按键只打一发。
     */
    private fun SubWeaponRuntime.Instance.repeatsWhileHeld(): Boolean =
        when (data.selectedFireModeInfo().mode) {
            FireMode.AUTO -> true
            FireMode.BURST -> burstRemaining > 0
            else -> false
        }

    /** 这次扣扳机要打几发点射；没有 `Burst` 模式的副武器就是 0 */
    private fun burstAmountOf(instances: List<SubWeaponRuntime.Instance>): Int =
        instances.asSequence()
            .filter { it.data.selectedFireModeInfo().mode == FireMode.BURST }
            .map { it.data.get(GunProp.BURST_AMOUNT) }
            .maxOrNull()
            ?.coerceAtLeast(0)
            ?: 0

    /** 按住 G 时还要不要继续触发：只要有一个槽位能连发就算（装了多个副武器是边角情况） */
    private fun holdsFire(instances: List<SubWeaponRuntime.Instance>): Boolean =
        instances.any { it.repeatsWhileHeld() }

    private inline fun debug(message: () -> String) {
        if (DisplayConfig.MELEE_DEBUG_LOG.get()) {
            Mod.LOGGER.info("[SubWeapon] {}", message())
        }
    }
}
