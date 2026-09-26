package com.atsuishio.superbwarfare.client.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.animation.gun.GeoGunAnimationInstance
import com.atsuishio.superbwarfare.client.gun.SubWeaponClientHandler.playFireAnimation
import com.atsuishio.superbwarfare.client.gun.SubWeaponClientHandler.tryTrigger
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.GunData
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
 * 冷却中跳过、已经在装填/拉栓的跳过、剩下的进 [SubWeaponFireMessage.slots]；
 * 动作占用取所有实际触发者里最长的一个，避免"遍历触发"被动作锁逐个拦掉。
 *
 * **客户端不决定"开火还是装填"**：那是服务端的事（见报文的类注释）。
 * 这里只做四件事 —— 挑出这一次 G 要操作的槽位、上动作锁给个节奏、发报文、**播开火表现**
 * （动画 + 枪口焰归属，见 [playFireAnimation]）。
 * **开火音也不在这里播**：服务端在真的打出这一发之后会把音效参数发过来
 * （`LocalSoundMessage`），客户端只负责用主武器同一套口径出声
 * （参数来源是 `GunItem.resolveFire1PSounds`）—— 这样"其实在装填，按 G 却响了一声"不可能发生。
 * 冷却也由服务端写（写在主武器冷却表上），客户端只读 —— 单一事实来源，
 * 免得两边各写一次导致"谁都不动"的死角。
 *
 * 全部副武器都在冷却时：只播一声 `trigger_click` 反馈，不产生其它副作用
 * （这时**不会**落到近战 —— 手上明明有副武器却挥了一刀是最容易误伤的设计）。
 */
object SubWeaponClientHandler {

    /** 装填占用的兜底时长（真正的释放靠服务端状态同步，见 `MeleeClientHandler.syncServerDrivenLocks`） */
    private const val MIN_RELOAD_LOCK_TICKS = 20

    /**
     * 尝试用副武器消费这次 G。
     *
     * **半自动**：只有 [justPressed]（按键上升沿）为真时才真正开火；
     * 按住不放的后续 tick 会把这次按键**吞掉但不做任何事**（既不连发，也不掉到近战）。
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

        // 有副武器就吞掉整个按键（包括按住不放的后续 tick）—— 但只在上升沿真正触发一次
        if (!justPressed) return true

        // 动作被占用 / 主武器自己在换弹拉栓时什么都不做
        if (state.isLocked) return true
        if (data.reloading() || data.charging() || data.bolt.actionTimer.get() > 0) return true

        val triggered = ArrayList<SubWeaponRuntime.Instance>(instances.size)
        var lockTicks = 0

        for (instance in instances) {
            // 只看冷却（它写在主武器 NBT 上，同步很及时）。
            //
            // **刻意不看客户端的 `canShoot`**：客户端那份副武器状态是同步过来的、永远慢一拍，
            // 用它当门禁会出现"服务端明明已经装好了、客户端却判定打不出去、连报文都不发"
            // —— 表现就是"装填结束了按 G 什么都没发生"。
            // 开火与否交给服务端一个人拍板（`SubWeaponFireMessage` 里那次 `canShoot`）。
            if (data.cooldown.isCoolingDown(instance.cooldownKey)) {
                debug { "subweapon ${instance.slotName} skipped: cooling down" }
                continue
            }

            triggered += instance
            lockTicks = maxOf(lockTicks, instance.cooldownTicks())
        }

        if (triggered.isEmpty()) {
            debug { "subweapon: every installed sub-weapon is on cooldown" }
            // 占一小段锁：避免同一帧内被别的入口重复触发
            state.acquire(GunAction.SUB_WEAPON, MIN_RELOAD_LOCK_TICKS)
            player.playSound(ModSounds.TRIGGER_CLICK.get(), 1f, 1f)
            return true
        }

        state.acquire(GunAction.SUB_WEAPON, lockTicks.coerceAtLeast(1))

        sendPacketToServer(
            SubWeaponFireMessage(
                slots = triggered.map { it.slotName },
                spread = ClientEventHandler.gunSpread,
                zoom = ClientEventHandler.zoom,
                uuid = ClientEventHandler.lockedEntity?.uuid,
            )
        )

        playFireAnimation(player, triggered)

        // 开火音**不在这里播**：副武器这一发到底打没打出去由服务端拍板（弹匣空 / 正在装填 /
        // 冷却中都会被拒），而客户端那份副武器状态是同步过来的、可能还停在装配那一刻 ——
        // 用它当门禁就会出现"其实在装填，按 G 却响了一声开火音"。
        // 现在由服务端在**真的开火之后**把音效参数发给射手（`LocalSoundMessage`），
        // 客户端只负责用主武器同一套口径出声（`ClientEventHandler.playGunFire1PSound` 的参数来源
        // 是 `GunItem.resolveFire1PSounds`）。

        debug { "subweapon trigger: slots=${triggered.map { it.slotName }} lock=$lockTicks gun=${data.id}" }
        return true
    }

    /**
     * 副武器开火的**第一人称表现**：换开火动画 + 把枪口焰交给副武器。
     *
     * **动画名由配件数据说了算**（`SubWeaponInfo.Animation`，不写就是 `["fire_sub_weapon"]`）：
     * 短名按宿主枪 id 拼成 `animation.ak_12.fire_sub_weapon`，候选里第一个存在的 clip 胜出，
     * 全部落空就退回宿主枪自己的 `GunAnimation.Fire` —— "给某把枪做了副武器开火动画就用它、
     * 没做就照常播 `fire`"由一条数据表达，与刺刀的动作候选链（`MeleeAction.Animation`）同一套机制。
     *
     * **在客户端触发**（与近战、主武器开火同一个信任模型）：动画是纯客户端表现，
     * 而这一发到底成不成（空仓 / 正在装填）由服务端在报文里拍板，客户端预测与否都不影响判定；
     * 代价是服务端拒掉这一发时动画已经播了 —— 与近战挥空完全一致。
     * **开火音仍然只在服务端真的打出这一发之后才响**，两者刻意分开（见 [tryTrigger] 的说明）。
     *
     * 枪口焰一并交给副武器（`ClientEventHandler.subWeaponFireRotTimer`）：这期间主武器的
     * `flare` 骨骼一帧都不画，改画副武器模型自己的 `flare`（见 `MuzzleFlashRenderer`）。
     */
    private fun playFireAnimation(player: Player, triggered: List<SubWeaponRuntime.Instance>) {
        // 同时开火多个副武器是边角情况，而开火动画只有一条：按槽位顺序取**第一个**给出了候选的
        // （配件显式写 `"Animation": []` = 明确不要候选，会被跳过）
        val source = triggered.firstOrNull { it.info.fireAnimationCandidates().isNotEmpty() }
        val candidates = source?.info?.fireAnimationCandidates().orEmpty()
        // 只有"数据里显式写了候选、却一支都对不上"才值得报 error：默认候选落空是正常情况
        val reportMissing = source?.info?.hasExplicitFireAnimation == true

        (FirstPersonRenderHandler.getActiveAnimationInstance(InteractionHand.MAIN_HAND) as? GeoGunAnimationInstance)
            ?.triggerFire(player.mainHandItem, candidates, reportMissing)

        ClientEventHandler.subWeaponFireRotTimer = SUB_WEAPON_FLASH_START
    }

    /** 副武器枪口焰窗口的起点（与主武器 `fireRotTimer = 0.001` 同一个写法，见 `MuzzleFlashRenderer`） */
    private const val SUB_WEAPON_FLASH_START = 0.001

    private inline fun debug(message: () -> String) {
        if (DisplayConfig.MELEE_DEBUG_LOG.get()) {
            Mod.LOGGER.info("[SubWeapon] {}", message())
        }
    }
}
