package com.atsuishio.superbwarfare.client.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.gun.SubWeaponClientHandler.tick
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.ActiveGun
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.subweapon.SubWeaponRuntime
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.send.SubWeaponDeployMessage
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import net.minecraft.world.entity.player.Player

/**
 * 客户端副武器切换（G 键）
 */
object SubWeaponClientHandler {

    var lastRequestedSlot: String? = null
        private set

    /** 请求发出去之后、确认回来之前，不再重复发（避免连点刷包） */
    private var pending = false

    /** 兜底解锁的 tick 计数：确认一直没回来的话，过一会儿允许再请求 */
    private var pendingTicks = 0

    /**
     * 尝试用 [slot]（`null` = 切回主武器）消费这次 G
     */
    @JvmStatic
    fun request(player: Player, data: GunData, slot: AttachmentType?) {
        // 没有副武器：G 是空操作
        if (SubWeaponRuntime.installed(data, client = true).isEmpty()) return

        if (pending) {
            debug { "deploy request ignored: waiting for the server confirmation" }
            return
        }

        val target = slot?.name

        // 已经在目标状态上：只给一声反馈，不刷包（连点同一状态没有意义）
        val current = ActiveGun.activeSlot(data)?.name ?: ""
        if (current == (target ?: "")) {
            player.playSound(ModSounds.TRIGGER_CLICK.get(), 1f, 1f)
            return
        }

        // 等确认期间先占住动作锁，避免"切枪途中顺手打出一发"
        val state = GunActionLock.of(data)
        state.acquire(GunAction.SUB_WEAPON, PENDING_LOCK_TICKS)

        pending = true
        pendingTicks = PENDING_TIMEOUT_TICKS
        lastRequestedSlot = target

        sendPacketToServer(SubWeaponDeployMessage(slot = target, clientActive = current))
        debug { "deploy request: $current -> ${target ?: "MAIN"} on ${data.id}" }
    }

    /**
     * 每客户端 tick 推进一次（由 `MeleeClientHandler.tick` 调用，**与 G 是否按下无关**）
     */
    @JvmStatic
    fun tick(data: GunData?) {
        if (!pending) return
        if (--pendingTicks > 0) return

        pending = false

        // 兜底释放：只在锁还停在 SUB_WEAPON 时动手，别把别人（换弹/近战）的占用清掉
        if (data != null) {
            val state = GunActionLock.of(data)
            if (state.activeAction == GunAction.SUB_WEAPON) state.release(GunAction.SUB_WEAPON)
        }

        localPlayer?.playSound(ModSounds.TRIGGER_CLICK.get(), 1f, 1f)
        debug { "deploy request timed out; the G key and the action lock are usable again" }
    }

    /**
     * 服务端确认切换成功
     */
    @JvmStatic
    fun onDeployed(player: Player, slot: String) {
        pending = false
        lastRequestedSlot = slot.ifEmpty { null }

        val target = slot.ifEmpty { "MAIN" }
        ClientEventHandler.resetGunTransientState()

        // 瞄准意图按新操控的那把枪重新判一次
        val operated = ActiveGun.stackOf(player)
        if (!GunItem.isOperable(operated) || !GunResource.compute(operated).canZoom) {
            ClientEventHandler.zoom = false
            ClientEventHandler.stopWeaponSeekSound(player)
        }

        // 切换本身占一段锁（时长按当前操控那把枪的 DrawTime 估），期间不接受开火/近战
        val data = ActiveGun.dataOf(player)
        if (data != null) {
            val drawTicks = data.get(com.atsuishio.superbwarfare.data.gun.GunProp.DRAW_TIME)
                .coerceAtLeast(1) + DEPLOY_LOCK_EXTRA_TICKS
            GunActionLock.of(data).force(GunAction.SUB_WEAPON, drawTicks)
        }

        debug { "deployed -> $target" }
    }

    /** 服务端拒绝了这次切换：什么都不做（不演动画、不动状态机），只给一声反馈 */
    @JvmStatic
    fun onDeployRejected(player: Player) {
        pending = false

        val data = ActiveGun.dataOf(player)
        if (data != null) {
            val state = GunActionLock.of(data)
            if (state.activeAction == GunAction.SUB_WEAPON) state.release(GunAction.SUB_WEAPON)
        }

        player.playSound(ModSounds.TRIGGER_CLICK.get(), 1f, 1f)
        debug { "deploy rejected by the server" }
    }

    /** 确认没回来时，"等确认"占用的**安全上限**；[tick] 会在这之后兜底释放 */
    private const val PENDING_LOCK_TICKS = 12

    /** 确认一直没回来时的兜底解锁时长（tick） */
    private const val PENDING_TIMEOUT_TICKS = 40

    /** 确认回来后，实际切换动画占用的锁在 `DrawTime` 之上再加一点握手余量 */
    private const val DEPLOY_LOCK_EXTRA_TICKS = 4

    private val localPlayer: Player?
        get() = com.atsuishio.superbwarfare.tools.localPlayer

    private inline fun debug(message: () -> String) {
        if (DisplayConfig.MELEE_DEBUG_LOG.get()) {
            Mod.LOGGER.info("[SubWeapon] {}", message())
        }
    }
}
