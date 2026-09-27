package com.atsuishio.superbwarfare.client.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.gun.SubWeaponClientHandler.tick
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.send.SubWeaponDeployMessage
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime
import com.atsuishio.superbwarfare.tools.ActiveGun
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import net.minecraft.world.entity.player.Player

/**
 * 客户端副武器切换（G 键）。
 *
 * ## 四期的语义（§9.8.2）
 *
 * | 情况 | V | G |
 * |---|---|---|
 * | 什么都没装 | 主武器自身近战 | **什么都不做**（没有副武器可切） |
 * | 只装副武器，当前是主武器 | 主武器自身近战 | **切到副武器** |
 * | 只装副武器，当前是副武器 | **主武器**自身近战 | **切回主武器** |
 * | 多个副武器 | 同上 | 切到 `AttachmentType.entries` 顺序里的**第一个**（不做优先级/轮换） |
 *
 * ⚠ **G 与近战完全解耦**：V 是唯一的近战入口，G 永远不会触发近战。
 * 三期那条"没有副武器就落回近战入口（等同 V）"的语义已废除。
 *
 * ## 客户端不决定"切没切成"
 *
 * 这里只发请求（[SubWeaponDeployMessage]）并给一个节流反馈；
 * **真正的状态由服务端写**（`GunState.ActiveSlot` / `ActiveOwner`），
 * 客户端**收到 [com.atsuishio.superbwarfare.network.message.receive.SubWeaponDeployedMessage] 才演**。
 * 与三期给开火定的"服务端拍板"同一个口径 —— 只是拍板对象从"这一发打没打出去"
 * 换成了"这次切没切成"。
 *
 * 三期的整套东西都删了：`tryTrigger` / `fire` / `Semi`·`Auto`·`Burst` 手写状态机 /
 * `playFireAnimation` / 每发 `1200 / RPM` 的冷却预写。
 * 副武器被切出来之后，开火走 `ClickEventHandler.handleWeaponFirePress` → `FireKeyMessage`
 * 这条**普通链路**，扣扳机方式由**它自己那份枪数据**的 `DefaultFireMode` 决定。
 */
object SubWeaponClientHandler {

    /**
     * 上一次发出去的切换请求（`null` = 请求切回主武器）。
     *
     * 只用于日志对账与"重复请求去抖"：服务端接受与否完全看它自己的校验。
     */
    var lastRequestedSlot: String? = null
        private set

    /** 请求发出去之后、确认回来之前，不再重复发（避免连点刷包） */
    private var pending = false

    /** 兜底解锁的 tick 计数：确认一直没回来的话，过一会儿允许再请求 */
    private var pendingTicks = 0

    /**
     * 尝试用 [slot]（`null` = 切回主武器）消费这次 G。
     *
     * **切换当场就占用动作锁**（[GunAction.SUB_WEAPON]）：确认回来之前的这一小段窗口里，
     * 开火/换弹/近战都不该生效 —— 否则会出现"切着枪顺手打出一发"。
     * 占用时长取两把枪 `DrawTime` 的较大者（GP-25 数据里是 1 tick，几乎瞬时），
     * 并在确认回来时**重新按真实时长占一次**（`onDeployed`）。
     *
     * ⚠ **没有返回值，也不再需要返回值。** 以前它返回 `false` 表示"没副武器可切"，
     * 调用方据此把 G 落到近战入口（等同 V）—— 那条语义四期已废除（§9.8.2）：
     * **G 只负责切换，V 是唯一的近战入口**，所以"没副武器"只是**什么都不做**。
     * 留着返回值只会诱使调用方再写出一个落回近战的分支。
     *
     * @param slot 目标副武器槽位；`null` = 切回主武器。注意 `null` 在调用方有两种来源
     *   （"已经部署着，所以切回来" 与 "一把副武器都没装"），下面用已部署状态区分。
     */
    @JvmStatic
    fun request(player: Player, data: GunData, slot: AttachmentType?) {
        // 没有副武器：G 是空操作（不挥砍、不播音、不进动作锁）。见上面的 ⚠。
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
     * 每客户端 tick 推进一次（由 `MeleeClientHandler.tick` 调用，**与 G 是否按下无关**）。
     *
     * 干两件事：给 `pending` 一个兜底超时（免得一次丢包把 G 永久锁死），
     * 以及**超时时把动作锁一起放掉** —— 确认一直不回来时锁会一直挂着，
     * 玩家看到的是"什么都按不动"。
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
     * 服务端确认切换成功。
     *
     * ⚠ **两个方向都不演切枪动画，也都不解除瞄准**（§9.8.8）：
     *
     * - **不演切枪**：`resetGunStatus()` 会把 `drawTime` 打回 1.0，那正是"重新装备"的进度条。
     *   副武器是挂在同一把枪上的附件，**换的是"当前操控的枪"，不是"手上的东西"** ——
     *   两个方向都让主武器凭空做一次切枪动作是错的。真正换枪的路径是
     *   `LivingEquipmentChangeEvent` → `DrawClientMessage`，与这里无关。
     *   所以这里只调 `resetGunTransientState()`（清三连发/蓄力/自定义 RPM 那些**跨枪会串**的字段），
     *   它**不动** `drawTime`。
     *
     * - **不解除瞄准**：`zoom` 是"玩家按着瞄准键"这个**意图**，不是某把枪的状态 ——
     *   `handleWeaponZoom` 每 tick 读的是 `ActiveGun`，所以切过去之后倍率与位形本来就会
     *   自动换成新枪的（副武器的 `iron_view` → 宿主枪的 `scope_view`/`iron_view`）。
     *   手没有离开枪托，逼玩家松手再按一次是纯输入税。
     *   `resetGunTransientState()` 会把 `zoomTime` 归零 → 瞄准重新演一遍对焦，
     *   视觉上能看出"照门换了"，而不是在旧位形上硬切倍率。
     *
     *   唯一的例外：新枪**根本没有瞄准能力**（`CanZoom = false`）时必须清掉 `zoom`，
     *   否则 `zoomTime` 会一路涨到 1、空转出一个不存在的开镜位形。
     */
    @JvmStatic
    fun onDeployed(player: Player, slot: String) {
        pending = false
        lastRequestedSlot = slot.ifEmpty { null }

        val target = slot.ifEmpty { "MAIN" }
        ClientEventHandler.resetGunTransientState()

        // 瞄准意图按**新操控的那把枪**重新判一次（与 `handleWeaponZoomPress` 同一条门槛）
        val operated = ActiveGun.stackOf(player)
        if (!GunItem.isOperable(operated) || !GunResource.compute(operated).canZoom) {
            ClientEventHandler.zoom = false
            ClientEventHandler.stopWeaponSeekSound(player)
        }

        // 切换本身占一段锁（时长按当前操控那把枪的 DrawTime 估），期间不接受开火/近战。
        // 这里只是"不接受输入"，**不是**动画 —— 见上面的 ⚠。
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
