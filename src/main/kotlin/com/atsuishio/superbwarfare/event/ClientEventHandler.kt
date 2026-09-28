package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.api.event.ClientGunFireEvent
import com.atsuishio.superbwarfare.api.event.ClientVehicleFireEvent
import com.atsuishio.superbwarfare.client.ClientSyncedEntityHandler
import com.atsuishio.superbwarfare.client.animation.AnimationCurves
import com.atsuishio.superbwarfare.client.animation.gun.GeoGunAnimationInstance
import com.atsuishio.superbwarfare.client.gun.GunAction
import com.atsuishio.superbwarfare.client.gun.GunActionLock
import com.atsuishio.superbwarfare.client.gun.MeleeClientHandler
import com.atsuishio.superbwarfare.client.gun.SubWeaponClientHandler
import com.atsuishio.superbwarfare.client.lighting.LightPositionRegistry
import com.atsuishio.superbwarfare.client.lighting.MuzzleFlashHelper
import com.atsuishio.superbwarfare.client.lighting.VehicleLightingHandler
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay
import com.atsuishio.superbwarfare.client.overlay.OverlayTraceHandler
import com.atsuishio.superbwarfare.client.overlay.VehicleMainWeaponHudOverlay
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer
import com.atsuishio.superbwarfare.client.shader.ThermalShaderHandler
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.attachment.SubWeaponInfo
import com.atsuishio.superbwarfare.data.gun.*
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler.boltMove
import com.atsuishio.superbwarfare.event.ClientEventHandler.cameraRot
import com.atsuishio.superbwarfare.event.ClientEventHandler.currentMeleeDuration
import com.atsuishio.superbwarfare.event.ClientEventHandler.currentMeleeIndex
import com.atsuishio.superbwarfare.event.ClientEventHandler.firePosTimer
import com.atsuishio.superbwarfare.event.ClientEventHandler.fireRotTimer
import com.atsuishio.superbwarfare.event.ClientEventHandler.handleWeaponFire
import com.atsuishio.superbwarfare.event.ClientEventHandler.isGunMeleeActive
import com.atsuishio.superbwarfare.event.ClientEventHandler.resetGunTransientState
import com.atsuishio.superbwarfare.event.ClientEventHandler.subWeaponFireRotTimer
import com.atsuishio.superbwarfare.event.ClientEventHandler.subWeaponRecoilTimer
import com.atsuishio.superbwarfare.event.ClientEventHandler.zoomTime
import com.atsuishio.superbwarfare.init.*
import com.atsuishio.superbwarfare.item.attachment.SubWeaponItem
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.item.misc.MonitorItem
import com.atsuishio.superbwarfare.network.message.send.*
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.tools.*
import com.atsuishio.superbwarfare.world.saveddata.TDMSavedData
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.ChatFormatting
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.*
import net.minecraft.world.entity.npc.AbstractVillager
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.BellBlock
import net.minecraft.world.level.block.CrossCollisionBlock
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.client.event.*
import net.neoforged.neoforge.client.gui.VanillaGuiLayers
import net.neoforged.neoforge.common.util.TriState
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW
import software.bernie.geckolib.animation.AnimationProcessor
import software.bernie.geckolib.cache.`object`.GeoBone
import top.theillusivec4.curios.api.CuriosApi
import java.util.*
import kotlin.experimental.or
import kotlin.math.*

@EventBusSubscriber(Dist.CLIENT)
object ClientEventHandler {
    @JvmField
    var zoomTime: Double = 0.0

    @JvmField
    var zoomPos: Double = 0.0

    @JvmField
    var zoomPosZ: Double = 0.0

    /**
     * 脚架视图过渡进度：0 为正常持枪视角（`idle_view`），1 为卧姿架设视角（`bipod_view`）。
     */
    @JvmField
    var bipodViewTime: Double = 0.0

    // 脚架视图过渡时长（秒）。getDelta() 返回的是每帧推进的 tick 数（Minecraft#getDeltaFrameTime），
    // 1 秒累计 TICKS_PER_SECOND，因此换算成内部进度单位需要乘上它。
    private const val BIPOD_VIEW_DURATION_SECONDS = 0.35f
    private const val TICKS_PER_SECOND = 20f

    @JvmField
    var swayTime: Double = 0.0

    @JvmField
    var swayX: Double = 0.0

    @JvmField
    var swayY: Double = 0.0

    @JvmField
    var moveTime: Double = 0.0

    @JvmField
    var sprintTime: Double = 0.0

    @JvmField
    var movePosX: Double = 0.0

    @JvmField
    var movePosY: Double = 0.0

    @JvmField
    var moveRotZ: Double = 0.0

    @JvmField
    var sprintBasicRotX: Double = 0.0

    @JvmField
    var sprintBasicRotY: Double = 0.0

    @JvmField
    var sprintBasicRotZ: Double = 0.0

    @JvmField
    var sprintPosX: Double = 0.0

    @JvmField
    var sprintPosY: Double = 0.0

    @JvmField
    var sprintBasicPosX: Double = 0.0

    @JvmField
    var sprintBasicPosY: Double = 0.0

    @JvmField
    var sprintBasicPosZ: Double = 0.0

    @JvmField
    var movePosHorizon: Double = 0.0

    @JvmField
    var velocityY: Double = 0.0

    @JvmField
    var turnRot = doubleArrayOf(0.0, 0.0, 0.0)

    @JvmField
    var cameraRot = doubleArrayOf(0.0, 0.0, 0.0)

    @JvmField
    var fireRecoilTime: Double = 0.0

    @JvmField
    var firePosTimer: Double = 0.0

    @JvmField
    var fireRotTimer: Double = 0.0

    /**
     * 副武器开火窗口的起始值（与三期的 `SUB_WEAPON_FLASH_START` 同一个数）。
     *
     * `MuzzleFlashRenderer` 判的是 `0 < t < 0.3`，而 [handleWeaponFire] 每 tick 给它加
     * `0.24 * times`（`times` 以 tick 计），所以从 0.001 起大约可见 1 tick。
     */
    private const val SUB_WEAPON_FLASH_START = 0.001

    /**
     * 副武器开火时**镜头后坐**相位的起始值（与 `SUB_WEAPON_FLASH_START` 同一个数：同一发里两条
     * 窗口一起开）。
     *
     * 形状照抄 [firePosTimer] —— `handleWeaponFire` 每 tick 加 `0.16 * times`、涨到 2.0 归零，
     * 于是正好扫过 `decayingOscillation(0.6, 2, 2, ·)` 的四又三分之一周期。
     */
    private const val SUB_WEAPON_RECOIL_START = 0.001

    /**
     * 副武器（下挂榴弹这类）开火的枪口焰计时。
     *
     * 与 [fireRotTimer] 共用一套阈值（`0 < t < 0.3` 期间可见、涨到 3.0 归零），但**刻意不复用**它：
     * `fireRotTimer` 还会带动整把枪的后坐表现（`handleShootAnimationV2` 读它），而副武器的**枪身**
     * 由它自己的开火动画（`fire_sub_weapon`）负责，两边叠加会抖两下。（**镜头**那一份后坐是另一条
     * 与枪身无关的相位：[subWeaponRecoilTimer]。）
     *
     * 它同时是"这一簇枪口焰属于副武器"的标记：大于 0 时 `MuzzleFlashRenderer` 把火焰画在
     * **副武器模型自己的 `flare` 骨骼**上，主武器的 `flare` 这段期间一帧都不画
     * （副武器连 `flare` 骨骼都没有时就什么都不画，不退回主武器的枪口）。
     */
    @JvmField
    var subWeaponFireRotTimer: Double = 0.0

    /**
     * 副武器开火时**镜头后坐**的相位计时（0 → 2.0，与 [firePosTimer] 同一形状）。
     *
     * ## 为什么不能直接借 [firePosTimer]
     *
     * 那是**一物三用**的：枪身位形（`handleShootAnimationV2`）、拉栓（[boltMove]）、
     * 以及镜头（抬枪 + [cameraRot] 滚转）。副武器那一发的**枪身**两样都由宿主枪自己的开火动画
     * 表现（`fire_sub_weapon`，动的是宿主枪的 `root`，峰值 7.4° / 4.7），借它会连拉栓一起借来
     * ——**打榴弹时步枪拉栓**，与 §11.10.10 的"打榴弹时步枪抛壳"同族。
     *
     * 所以这里只留"相位"这**一个**语义：`RECOIL_X` / `RECOIL_Y` 打在哪，镜头就跟着动多少 ——
     * 抬枪那一项（`handleGunRecoil`）与滚转那一项（[handleWeaponFire] 的 `shake`，幅度是
     * `25000 · RECOIL_X · RECOIL_Y`）。⚠ 这两个消费者**此前都挂在 `firePosTimer > 0` 上**，
     * 而副武器那一发 `fireRecoilTime == 0.0` → `firePosTimer` 恒为 0 → `RECOIL_X` 一个字节都
     * 读不到（`RecoilY` 只剩"水平偏 `player.yRot`"那一条还活着）。
     *
     * 与 [subWeaponFireRotTimer] 同进同出（同一发一起开、主武器一开火一起清零），
     * 于是 `handleGunRecoil` / [handleWeaponFire] 里两个相位**二选一**，不会叠加成"抖两下"。
     */
    @JvmField
    var subWeaponRecoilTimer: Double = 0.0

    @JvmField
    var boltMove: Double = 0.0

    @JvmField
    var firePosZ: Double = 0.0

    @JvmField
    var customAnimSpeed: Double = 1.0

    @JvmField
    var recoilHorizon: Double = 0.0

    @JvmField
    var recoilY: Double = 0.0

    @JvmField
    var recoilForce: Double = 0.0

    @JvmField
    var droneFov: Double = 1.0

    @JvmField
    var droneFovLerp: Double = 1.0

    @JvmField
    var currentFov: Double = 0.0

    @JvmField
    var bowPullTimer: Double = 0.0

    @JvmField
    var bowPower: Double = 0.0

    @JvmField
    var bowPullPos: Double = 0.0

    @JvmField
    var gunSpread: Double = 0.0

    @JvmField
    var fireSpread: Double = 0.0

    @JvmField
    var fireCooldown: Double = 0.0

    @JvmField
    var lookDistance: Double = 0.0

    @JvmField
    var cameraLocation: Double = 0.6

    // 切换载具武器的冷却时间
    @JvmField
    var switchVehicleWeaponCooldown: Int = 0

    @JvmField
    var drawTime: Double = 1.0

    @JvmField
    var shellIndex: Int = 0

    @JvmField
    var shellIndexTime = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

    @JvmField
    var randomShell = doubleArrayOf(0.0, 0.0, 0.0)

    @JvmField
    var customZoom: Double = 0.0

    @JvmField
    var artilleryIndicatorZoom: Double = 1.0

    @JvmField
    var artilleryIndicatorCustomZoom: Double = 0.0

    @JvmField
    var clientTimer: MillisTimer = MillisTimer()

    @JvmField
    var clientTimerVehicle: MillisTimer = MillisTimer()

    // 正在按住开火键
    @JvmField
    var holdingFireKey: Boolean = false

    @JvmField
    var bowPull: Boolean = false

    @JvmField
    var chargeActive: Boolean = false

    @JvmField
    var chargeProgress: Double = 0.0

    @JvmField
    var chargePower: Double = 1.0

    @JvmField
    var zoom: Boolean = false

    @JvmField
    var breath: Boolean = false

    @JvmField
    var stamina: Float = 0f

    @JvmField
    var switchTime: Double = 0.0

    @JvmField
    var moveFadeTime: Double = 0.0

    @JvmField
    var sprintFadeTime: Double = 0.0

    @JvmField
    var exhaustion: Boolean = false

    @JvmField
    var holdFireVehicle: Boolean = false

    @JvmField
    var zoomVehicle: Boolean = false

    @JvmField
    var burstFireAmount: Int = 0

    @JvmField
    var customRpm: Int = 0

    /**
     * **旧版 GeckoLib 近战计数器（已废弃，不再由新近战系统驱动）**。
     *
     * V2 渲染路径的近战状态按枪隔离存在 [com.atsuishio.superbwarfare.client.gun.GunActionLock] 里，
     * 这个全局字段只为了让**尚未迁移的旧 GeckoLib 枪械**（`GunGeoItem` / `SecondaryCataclysmItem`）
     * 继续编译通过，永远是 0——旧枪械的近战动画不会再触发，这是既定取舍（旧路径不迁移）。
     *
     * 新代码一律用 [isGunMeleeActive] / [currentMeleeDuration] / [currentMeleeIndex]。
     */
    @JvmField
    @Deprecated("Use GunActionLock / ClientEventHandler.isGunMeleeActive instead")
    var gunMelee: Int = 0

    // 按住开火键的持续tick
    @JvmField
    var holdingFireKeyTicks: Int = 0

    @JvmField
    var holdingFireKeyTicks0: Float = 0f

    @JvmField
    var shouldPlayDischargeSound: Boolean = true

    @JvmField
    var revolverPreTime: Double = 0.0

    @JvmField
    var revolverWheelPreTime: Double = 0.0

    @JvmField
    var shakeTime: Double = 0.0

    @JvmField
    var shakeRadius: Double = 0.0

    @JvmField
    var shakeAmplitude: Double = 0.0

    @JvmField
    var shakePos = doubleArrayOf(0.0, 0.0, 0.0)

    @JvmField
    var shakeType: Double = 0.0

    @JvmField
    var lerpShake: Double = 0.0

    @JvmField
    var usingLunge: Boolean = false

    @JvmField
    var lungeAttack: Int = 0

    @JvmField
    var lungeDraw: Int = 0

    @JvmField
    var lungeSprint: Int = 0

    // 智慧芯片锁定的实体
    @JvmField
    var lockedEntity: Entity? = null

    @JvmField
    var dismountCountdown: Int = 0

    @JvmField
    var aimVillagerCountdown: Int = 0

    @JvmField
    var lastCameraType: CameraType? = null

    @JvmField
    var cameraPitch: Float = 0f

    @JvmField
    var cameraYaw: Float = 0f

    @JvmField
    var cameraRoll: Float = 0f

    // Tracks the PoseStack that received the vehicle push in bobHurt.
    @JvmField
    var vehiclePoseStack: PoseStack? = null

    // 禁止冲刺♿时长tick
    @JvmField
    var noSprintTicks: Float = 0f

    @JvmField
    var canDoubleJump: Boolean = false

    @JvmField
    var holdArtilleryIndicator: Int = 0

    @JvmField
    var holdToEjection: Int = 0

    @JvmField
    var isEditing: Boolean = false

    /**
     * 改装状态下当前正在编辑的配件槽位，与 [com.atsuishio.superbwarfare.client.screens.WeaponEditScreen.EditButton] 的 type 一致，
     * -1 表示未选中任何配件。
     */
    @JvmField
    var editingAttachmentType: Int = -1

    /**
     * 改装时视线聚焦的平滑偏移量（相对 IDLE_VIEW_BONE，模型空间），
     * 由 GeoGunRenderer 每帧向目标偏移插值。
     */
    @JvmField
    var editFocusOffset: Vector3f = Vector3f()

    /**
     * 改装未聚焦时浮动预览绕 Y 轴的旋转角（弧度），
     * 由 GeoGunRenderer 根据鼠标水平位置计算并插值，避免视角平移时卡进模型。
     */
    @JvmField
    var editFocusYaw: Float = 0f

    /**
     * 改装未聚焦时浮动预览绕 X 轴的旋转角（弧度），
     * 由 GeoGunRenderer 根据鼠标垂直位置计算并插值，避免视角平移时卡进模型。
     */
    @JvmField
    var editFocusPitch: Float = 0f

    /**
     * 改装镜头从配件聚焦回退到浮动预览的剩余缓动时长（秒）。
     * 聚焦配件时由 GeoGunRenderer 重置为完整时长，按下 ESC 回到预览后逐帧递减；
     * 期间使用较慢的平滑速度，使镜头平滑回退而非瞬间跳回预览位。
     */
    @JvmField
    var editFocusReturnTime: Float = 0f

    @JvmField
    var shootCoolDown: Int = 0

    // 锁定类武器用
    @JvmField
    var nearestEntity: Entity? = null

    @JvmField
    var seekingEntity: Entity? = null

    @JvmField
    var lockingEntity: Entity? = null

    @JvmField
    var seekingPos: Vec3? = null

    @JvmField
    var lockingPos: Vec3? = null

    @JvmField
    var seekingTime: Int = 0

    @JvmField
    var guideType: Int = 0

    @JvmField
    var lockOn: Boolean = false

    // 锁定类载具用
    @JvmField
    var nearestEntityVehicle: Entity? = null

    @JvmField
    var seekingEntityVehicle: Entity? = null

    @JvmField
    var lockingEntityVehicle: Entity? = null

    @JvmField
    var seekingPosVehicle: Vec3? = null

    @JvmField
    var lockingPosVehicle: Vec3? = null

    @JvmField
    var seekingTimeVehicle: Int = 0

    @JvmField
    var lockOnVehicle: Boolean = false

    @JvmField
    var lastOperatingGunUUID: UUID? = null

    @JvmField
    var keysCache: Short = 0

    /** 自动盘旋双击夺回操控权：上次按下前进键的tick */
    @JvmField
    var loiterLastForwardTapTick: Int = -20

    /** 自动盘旋双击夺回操控权：前进键连击计数 */
    @JvmField
    var loiterForwardTapCount: Int = 0

    /** 卸载乘客按住：按住卸载乘客键的持续tick，每20tick(1秒)卸载一位乘客 */
    @JvmField
    var unloadPassengersHoldTicks: Int = 0

    /** 卸载乘客双击：上次按下卸载乘客键的tick */
    @JvmField
    var unloadPassengersLastTapTick: Int = -20

    /** 卸载乘客双击：卸载乘客键连击计数 */
    @JvmField
    var unloadPassengersTapCount: Int = 0

    /** 卸载乘客双击：上一帧卸载乘客键是否按下，用于检测上升沿 */
    @JvmField
    var wasUnloadPassengersDown: Boolean = false

    /** 断开牵引双击：上次按下断开牵引键的tick */
    @JvmField
    var disconnectTowingLastTapTick: Int = -20

    /** 断开牵引双击：断开牵引键连击计数 */
    @JvmField
    var disconnectTowingTapCount: Int = 0

    /** 断开牵引双击：上一帧断开牵引键是否按下，用于检测上升沿 */
    @JvmField
    var wasDisconnectTowingDown: Boolean = false

    @JvmField
    var tdmSavedData: TDMSavedData = TDMSavedData()

    @JvmField
    var activeThermalImaging: Boolean = false

    // 原VectorUtil的属性
    @JvmField
    var fov: Double = 70.0

    @JvmField
    var modelViewMatrix: Matrix4f? = null

    @JvmField
    var projectionMatrix: Matrix4f? = null

    private var lastX: Float = 0f
    private var lastY: Float = 0f

    @JvmField
    var bombHitPosO: Vec3 = Vec3.ZERO

    @JvmField
    var bombHitPos: Vec3 = Vec3.ZERO

    @JvmField
    var missileLockingPos: BlockPos? = null

    @JvmField
    var movingZoom: Double = 1.25

    @SubscribeEvent
    fun handleWeaponTurn(event: ViewportEvent.ComputeFov) {
        val player = localPlayer ?: return
        val xRotOffset = Mth.lerp(event.partialTick.toFloat(), player.xBobO, player.xBob)
        val yRotOffset = Mth.lerp(event.partialTick.toFloat(), player.yBobO, player.yBob)
        val xRot = player.getViewXRot(event.partialTick.toFloat()) - xRotOffset
        val yRot = player.getViewYRot(event.partialTick.toFloat()) - yRotOffset
        turnRot[0] = (0.05 * xRot).coerceIn(-20.0, 20.0) * (1 - 0.05 * zoomTime)
        turnRot[1] = (0.025 * yRot).coerceIn(-20.0, 20.0) * (1 - 0.05 * zoomTime)
        turnRot[2] = (0.05 * yRot).coerceIn(-20.0, 20.0) * (1 - 0.5 * zoomTime)
    }

    @JvmStatic
    fun isFreeCam(player: Player): Boolean {
        val vehicle = player.vehicle
        return vehicle is VehicleEntity && vehicle.allowFreeCam() && ModKeyMappings.FREE_CAMERA.isDown
    }

    @JvmStatic
    fun isNacelleCam(player: Player): Boolean {
        val vehicle = player.vehicle

        if (vehicle is VehicleEntity) {
            val data = vehicle.getGunData(player)
            if (data != null) {
                return data.get(GunProp.USE_NACELLE_CAMERA) && zoomVehicle
            }
        }

        return false
    }

    private fun isMoving(): Boolean {
        val player = localPlayer ?: return false
        return mc.options.keyLeft.isDown
                || mc.options.keyRight.isDown
                || mc.options.keyUp.isDown
                || mc.options.keyDown.isDown
                || player.isSprinting
    }

    @SubscribeEvent
    fun handleClientTick(event: ClientTickEvent.Post) {
        val player = localPlayer ?: return

        if (mc.fps <= 20) {
            handleGunShoot()
            handleVehicleGunShoot()
        }

        // ⚠ 这里是**主手物品**，四期刻意不改：
        // `handleGunMelee` 的近战恒用主武器（§9.8.2，副武器没有近战），
        // `handleLungeAttack` 用的是长矛物品，两者都只认"物理上拿在手里的东西"。
        val stack = player.mainHandItem
        if (notInGame && !ClickEventHandler.switchZoom) {
            zoom = false
        }

        if (player.onGround() && canDoubleJump) {
            canDoubleJump = false
        }

        recoilForce *= 0.55

        ClientSyncedEntityHandler.clean()
        isProne(player)
        handleVariableDecrease()
        aimAtVillager(player)
        CrossHairOverlay.handleRenderDamageIndicator()
        staminaSystem()
        handlePlayerSprint()
        handleLungeAttack(player, stack)
        handleGunMelee(player, stack)
        weaponZooming(stack)
        lockWeaponSeeking(player, stack)
        vehicleWeaponSeeking(player)
        handleThermalImaging(player)
        handleHandsomeGoggles(player)
        handleShootDelay(player, stack)
        handleControlVehicle(player, stack)
        handleArtilleryIndicator(player, stack)
        calculateBombHitPos(player)

        LightPositionRegistry.tick()
    }

    @JvmStatic
    fun hasThermalImagingGoggles(): Boolean {
        return CuriosApi.getCuriosInventory(localPlayer).map {
            it.findFirstCurio(ModItems.THERMAL_IMAGING_GOGGLES.get()).isPresent
        }.orElseGet { false }
    }

    fun handleThermalImaging(player: Player) {
        var hasThermalImagingGoggles = hasThermalImagingGoggles()
        val vehicle = player.vehicle

        if (vehicle is VehicleEntity) {
            val index = vehicle.getSeatIndex(player)
            if (index != -1) {
                val seat = vehicle.computed().seats().getOrNull(index)
                if (seat != null && seat.hasThermalImaging) {
                    hasThermalImagingGoggles = true
                }
            }
        }

        if (!activeThermalImaging || !hasThermalImagingGoggles) {
            activeThermalImaging = false
            turnOffThermalImaging()
        } else if (Minecraft.getInstance().gameRenderer.currentEffect() == null) {
            turnOnThermalImaging()
        }
    }

    @JvmStatic
    fun turnOnThermalImaging() {
        ThermalShaderHandler.setActive(true)
        mc.gameRenderer.loadEffect(Mod.loc("shaders/post/night_vision.json"))
    }

    @JvmStatic
    fun turnOffThermalImaging() {
        if (ThermalShaderHandler.isActive()) {
            mc.gameRenderer.shutdownEffect()
            ThermalShaderHandler.setActive(false)
        }
    }

    @JvmStatic
    var handsomeGogglesActive: Boolean = false

    @JvmStatic
    fun isWearingHandsomeGoggles(player: Player): Boolean {
        return player.getItemBySlot(EquipmentSlot.HEAD).`is`(ModItems.HANDSOME_GOGGLES.get())
    }

    @JvmStatic
    fun handleHandsomeGoggles(player: Player) {
        val wearing = isWearingHandsomeGoggles(player)
        val isFirstPerson = mc.options.cameraType == CameraType.FIRST_PERSON
        val shouldBeActive = wearing && isFirstPerson

        if (shouldBeActive && !handsomeGogglesActive) {
            handsomeGogglesActive = false  // reset so turnOn actually loads
            turnOnHandsomeGoggles()
        } else if (!shouldBeActive && handsomeGogglesActive) {
            turnOffHandsomeGoggles()
        }
    }

    @JvmStatic
    fun turnOnHandsomeGoggles() {
        handsomeGogglesActive = true
        mc.gameRenderer.loadEffect(Mod.loc("shaders/post/handsome_goggles.json"))
    }

    @JvmStatic
    fun turnOffHandsomeGoggles() {
        handsomeGogglesActive = false
        mc.gameRenderer.shutdownEffect()
    }

    /**
     * 枪管旋转（`GunAnimation.Hold` 那一层）的扳机判据：开火键按着，加特林开镜也算。
     *
     * **故意不含 `canShoot`**：旋转是扳机驱动的电机，不是「这一发现在打得出去」。过热（热量到
     * 100 上锁、降到 80 以下才解锁）、背包弹药打空、换弹这些只该停子弹、不该停枪管——绑在一起
     * 时，连射到过热枪管就跟着停转、退热后又自己转起来，看着就是「旋转时有时无」。
     * [GeoGunAnimationInstance] 的旋转层与下面那声旋转音效共用这一个判据，两边不会一个响一个不转。
     */
    fun isBarrelSpinTriggered(stack: ItemStack): Boolean {
        return holdingFireKey || (zoom && stack.`is`(ModItems.MINIGUN.get()))
    }

    /**
     *  处理武器射击延迟
     */
    fun handleShootDelay(player: Player, stack: ItemStack) {
        val item = stack.item as? GunItem
        // 手持副武器时按普通物品处理
        if (item != null && GunItem.isHeldWeapon(stack)) {
            val data = GunData.from(stack)

            var uuid: UUID? = null
            try {
                uuid = data.gunDataTag.getUUID("UUID")
            } catch (_: Exception) {
            }

            if (notInGame) {
                burstFireAmount = 0
            }

            // 切枪时记得重置状态
            //
            // ⚠ 这里读的是**主手那件物品**的 UUID（上面的 `stack` 就是 `player.mainHandItem`），
            // 所以它表达的是"玩家真的换了手上的枪" —— 主/副武器切换不会走到这里
            // （操控对象换了、手里那把没换，§9.8.2）。这是**有意的**：
            // 副武器没有自己的 UUID（合成栈是凭空造的），拿 `ActiveGun` 的栈来判会一取就是 null，
            // 于是每次切换都当成"换了把枪"演一次切枪动画。
            if (uuid == null || uuid != lastOperatingGunUUID) {
                resetGunStatus()
                resetLungeMineStatus()
            }
            lastOperatingGunUUID = uuid

            val spinTriggered = isBarrelSpinTriggered(stack)

            // 加特林特有的旋转音效：与旋转层同一个判据，只跟扳机走——过热/空仓时枪管照样转，
            // 声音就不该断（留在下面的 canShoot 里的话，过热时会出现「枪管在转但没有声音」的错配）
            if (spinTriggered && stack.`is`(ModItems.MINIGUN.get())) {
                val rpm = data.get(GunProp.RPM) / 3600F
                player.playSound(ModSounds.MINIGUN_ROTATE.get(), 1f, 0.7f + rpm)
            }

            if (spinTriggered && item.canShoot(data, player)) {
                val maxHoldTicks = data.selectedFireModeInfo().chargeConfig()?.effectiveDuration
                    ?: data.get(GunProp.SHOOT_DELAY)
                holdingFireKeyTicks = (holdingFireKeyTicks + 1).coerceAtMost(maxHoldTicks + 1)

                // Spawn light flashes for raycast tools (RepairTool / Taser) when holding fire key
                MuzzleFlashHelper.spawnToolFlash(player, stack)

                // QL特有的樱花特效
                if (stack.`is`(ModItems.QL_1031.get()) && player.tickCount % 5 == 0) {
                    val random = (Math.random() - 0.5) * 2
                    player.level().addParticle(
                        ParticleTypes.CHERRY_LEAVES,
                        player.x + random,
                        player.eyeY + 0.5 * random,
                        player.z + random,
                        0.0,
                        0.0,
                        0.0
                    )
                }
            }
        } else {
            lastOperatingGunUUID = null
        }
    }

    fun handleArtilleryIndicator(player: Player, stack: ItemStack) {
        if ((stack.`is`(ModItems.ARTILLERY_INDICATOR.get()) || (stack.`is`(ModItems.MONITOR.get())
                    && player.offhandItem.`is`(ModItems.ARTILLERY_INDICATOR.get()))) && holdingFireKey
        ) {
            holdArtilleryIndicator = (holdArtilleryIndicator + 1).coerceIn(0, 20)
            if (holdArtilleryIndicator >= 19 && shootCoolDown == 0) {
                sendPacketToServer(ArtilleryIndicatorFireMessage)
                shootCoolDown = 10
            }
        } else {
            holdArtilleryIndicator = 0
        }

        if (shootCoolDown > 0) {
            shootCoolDown--
        }
    }

    fun calculateBombHitPos(player: Player) {
        val vehicle = player.vehicle as? VehicleEntity ?: return
        val gunData = vehicle.getGunData(player)

        bombHitPosO = bombHitPos
        bombHitPos = if (gunData != null && gunData.get(GunProp.CROSSHAIR) == "@AirBomb") {
            vehicle.bombHitPos(player)
        } else {
            Vec3.ZERO
        }
    }

    fun handleControlVehicle(player: Player, stack: ItemStack) {
        val tag = NBTTool.getTag(stack)

        var keys: Short = 0
        val vehicle = player.vehicle

        // 正在游戏内控制载具或无人机
        if (!notInGame && (vehicle is VehicleEntity && vehicle.firstPassenger == player) ||
            (stack.`is`(ModItems.MONITOR.get())
                    && tag.getBoolean(MonitorItem.USING)
                    && tag.getBoolean(MonitorItem.LINKED))
        ) {
            if (ModKeyMappings.MOVE_LEFT.isDown) {
                keys = keys or 0b000000001
            }
            if (ModKeyMappings.MOVE_RIGHT.isDown) {
                keys = keys or 0b000000010
            }
            if (ModKeyMappings.MOVE_FORWARD.isDown) {
                keys = keys or 0b000000100
            }
            if (ModKeyMappings.MOVE_BACKWARD.isDown) {
                keys = keys or 0b000001000
            }
            if (ModKeyMappings.MOVE_SPACE.isDown) {
                keys = keys or 0b000010000
            }
            if (ModKeyMappings.MOVE_SHIFT.isDown) {
                keys = keys or 0b000100000
            }
            if (ModKeyMappings.RELEASE_DECOY.isDown) {
                keys = keys or 0b001000000
            }
            if (holdFireVehicle) {
                keys = keys or 0b010000000
            }
            if (ModKeyMappings.MOVE_CTRL.isDown) {
                keys = keys or 0b100000000
            }
        }

        if (keys != keysCache) {
            // 盘旋模式下阻止操控包发往服务端，但检测双击前进键夺回操控权
            val blockLoiter = vehicle is VehicleEntity
                    && vehicle.loiterActive
                    && vehicle.computed().engineType == EngineType.AIRCRAFT
            if (!blockLoiter) {
                sendPacketToServer(VehicleMovementMessage(keys))
            } else {
                // 检测双击前进键(W)在0.5s(10tick)内夺回操控权
                val forwardBit = 0b000000100
                val forwardJustPressed = (keys.toInt() and forwardBit) != 0 && (keysCache.toInt() and forwardBit) == 0
                if (forwardJustPressed) {
                    val currentTick = player.tickCount
                    if (currentTick - loiterLastForwardTapTick <= 10) {
                        sendPacketToServer(LoiterOverrideMessage)
                        loiterForwardTapCount = 0
                        loiterLastForwardTapTick = -20
                    } else {
                        loiterForwardTapCount = 1
                        loiterLastForwardTapTick = currentTick
                        player.displayClientMessage(
                            Component.translatable(
                                "tips.superbwarfare.loiter_override_hint",
                                ModKeyMappings.MOVE_FORWARD.key.displayName.string
                            ), true
                        )
                    }
                }
            }
            keysCache = keys
        }

        if (vehicle is VehicleEntity && vehicle.allowEjection(vehicle.getSeatIndex(player)) && ModKeyMappings.DISMOUNT.isDown()) {
            holdToEjection = (holdToEjection + 1).coerceIn(0, 10)
            if (holdToEjection >= 10) {
                sendPacketToServer(PlayerStopRidingMessage(true))
                stopVehicleReloadSound(player)
            }
        } else {
            holdToEjection = 0
        }

        // 卸载乘客：按住每隔1秒卸载最后一位，双击卸载全部
        if (vehicle is VehicleEntity && vehicle.firstPassenger == player && vehicle.passengers.size > 1) {
            val unloadDown = ModKeyMappings.UNLOAD_PASSENGERS.isDown
            val unloadJustPressed = unloadDown && !wasUnloadPassengersDown
            wasUnloadPassengersDown = unloadDown

            if (unloadDown) {
                // 按住：每隔1秒(20tick)卸载序号最靠后的一位乘客
                unloadPassengersHoldTicks++
                if (unloadPassengersHoldTicks >= 20) {
                    sendPacketToServer(VehicleUnloadPassengersMessage(false))
                    unloadPassengersHoldTicks = 0
                }
            } else {
                unloadPassengersHoldTicks = 0
            }

            // 双击：在0.5s(10tick)内检测两次按下，卸载全部乘客
            if (unloadJustPressed) {
                val currentTick = player.tickCount
                if (currentTick - unloadPassengersLastTapTick <= 10) {
                    sendPacketToServer(VehicleUnloadPassengersMessage(true))
                    unloadPassengersTapCount = 0
                    unloadPassengersLastTapTick = -20
                    unloadPassengersHoldTicks = 0
                } else {
                    unloadPassengersTapCount = 1
                    unloadPassengersLastTapTick = currentTick
                    player.displayClientMessage(
                        Component.translatable(
                            "tips.superbwarfare.unload_passengers_hint",
                            ModKeyMappings.UNLOAD_PASSENGERS.key.displayName.string,
                            ModKeyMappings.UNLOAD_PASSENGERS.key.displayName.string
                        ), true
                    )
                }
            }
        } else {
            wasUnloadPassengersDown = false
            unloadPassengersHoldTicks = 0
        }

        // 检测双击断开牵引键在0.5s(10tick)内，断开载具的牵引关系
        if (vehicle is VehicleEntity && vehicle.firstPassenger == player) {
            val towingDown = ModKeyMappings.DISCONNECT_TOWING.isDown
            val towingJustPressed = towingDown && !wasDisconnectTowingDown
            wasDisconnectTowingDown = towingDown
            if (towingJustPressed) {
                val currentTick = player.tickCount
                if (currentTick - disconnectTowingLastTapTick <= 10) {
                    sendPacketToServer(VehicleDisconnectTowingMessage)
                    disconnectTowingTapCount = 0
                    disconnectTowingLastTapTick = -20
                } else {
                    disconnectTowingTapCount = 1
                    disconnectTowingLastTapTick = currentTick
                    player.displayClientMessage(
                        Component.translatable(
                            "tips.superbwarfare.disconnect_towing_hint",
                            ModKeyMappings.DISCONNECT_TOWING.key.displayName.string
                        ), true
                    )
                }
            }
        } else {
            wasDisconnectTowingDown = false
        }
    }

    fun lockWeaponSeeking(player: Player, stack: ItemStack) {
        // 当前操控的枪（部署中的副武器也算）—— 锁定参数读的是**正在操作那把**的数据
        if (GunItem.isOperable(stack)) {
            val data = GunData.from(stack)
            val lockTime = data.get(GunProp.SEEK_TIME)
            // 搜寻角度
            val fovAdjust = mc.options.fov().get() / 80f
            val seekAngle = data.get(GunProp.SEEK_ANGLE) * fovAdjust
            val range = data.get(GunProp.SEEK_RANGE)
            val maxGuidedRange = data.get(GunProp.MAX_GUIDED_RANGE)
            val canGuidedByRadar = data.get(GunProp.CAN_GUIDED_BY_RADAR)
            val affectedByStealthTarget = data.get(GunProp.AFFECTED_BY_STEALTH_TARGET)
            val cameraPos = mc.gameRenderer.mainCamera.position

            if (zoomTime > 0.7) {
                nearestEntity = SeekTool.Builder(player)
                    .withinRangeSeekWeapon(range, maxGuidedRange, affectedByStealthTarget, canGuidedByRadar)
                    .withinAngle(seekAngle)
                    .baseFilter()
                    .heightRange(data.get(GunProp.MIN_TARGET_HEIGHT), data.get(GunProp.MAX_TARGET_HEIGHT))
                    .smokeFilter()
                    .noVehicle()
                    .noClip()
                    .buildWithClosestSeekWeapon(canGuidedByRadar)

                val decoy = TraceTool.findLookDecoy(player, cameraPos, player.getViewVector(1f), range)
                if (decoy != null && decoy.type.`is`(ModTags.EntityTypes.DECOY)) {
                    nearestEntity = decoy
                    seekFailure(player)
                }

                if (data.get(GunProp.SEEK_TYPE) == SeekType.HOLD_FIRE) {
                    if (nearestEntity == null || player.isShiftKeyDown) {
                        // 锁定方块
                        val result = player.level().clip(
                            ClipContext(
                                player.eyePosition,
                                player.eyePosition.add(player.getViewVector(1f).scale(512.0)),
                                ClipContext.Block.VISUAL,
                                ClipContext.Fluid.ANY,
                                player
                            )
                        )
                        seekingPos = result.location

                        if (seekingTime > lockTime + 2 && !lockOn) {
                            lockOn = true
                        }

                        //锁定失败
                        if (lockingPos != null &&
                            (player.lookAngle.angleTo(
                                player.eyePosition.vectorTo(lockingPos!!)
                            ) > seekAngle || !noClip(player, lockingPos!!))
                        ) {
                            seekingTime = 0
                            seekFailure(player)
                        }

                        if (holdingFireKey) {
                            if (seekingPos != null && seekingPos!!.distanceToSqr(player.eyePosition) < range * range) {
                                seekingTime++
                                if (seekingTime == 1) {
                                    lockingPos = seekingPos
                                }
                            } else {
                                seekingTime = 0
                                lockingPos = null
                            }
                            guideType = 1
                        } else {
                            if (lockOn) {
                                if (lockingPos != null) {
                                    sendPacketToServer(ShootMessage(gunSpread, zoom, null, lockingPos!!.toVector3f()))
                                }
                                lockOn = false
                            }
                            seekFailure(player)
                        }
                    } else {
                        // 锁定实体
                        if (seekingTime > lockTime + 2 && !lockOn) {
                            lockingEntity = seekingEntity
                            lockOn = true
                        }

                        //锁定失败
                        if (seekingEntity != null && (
                                    player.lookAngle.angleTo(
                                        player.eyePosition.vectorTo(
                                            VectorTool.lerpGetEntityBoundingBoxCenter(
                                                seekingEntity!!,
                                                1f
                                            )
                                        )
                                    ) > seekAngle || !SeekTool.NOT_IN_SMOKE.test(seekingEntity) || !noClip(
                                        player,
                                        seekingEntity!!
                                    ))
                        ) {
                            seekFailure(player)
                        }

                        if (holdingFireKey) {
                            if (seekingEntity == null) {
                                seekingEntity = nearestEntity
                            }
                            if (nearestEntity != null && lockingPos == null) {
                                seekingTime++
                                if ((!seekingEntity!!.passengers.isEmpty() || seekingEntity is VehicleEntity)
                                    && player.tickCount % 3 == 0 && !lockOn
                                ) {
                                    sendPacketToServer(
                                        SeekingWeaponWarningMessage(
                                            false,
                                            seekingEntity!!.uuid
                                        )
                                    )
                                }
                                guideType = 0
                            }
                        } else {
                            if (lockOn) {
                                if (lockingEntity != null) {
                                    sendPacketToServer(
                                        ShootMessage(
                                            gunSpread,
                                            zoom,
                                            lockingEntity!!.uuid,
                                            lockingEntity!!.eyePosition.toVector3f()
                                        )
                                    )
                                }
                                lockOn = false
                            }
                            seekFailure(player)
                        }
                    }
                } else if (data.get(GunProp.SEEK_TYPE) == SeekType.HOLD_ZOOM) {
                    // 瞄准锁定只能锁实体
                    if (seekingTime > lockTime + 2 && !lockOn) {
                        lockingEntity = seekingEntity
                        lockOn = true
                    }

                    // 锁定失败
                    if (seekingEntity != null && (player.lookAngle.angleTo(
                            player.eyePosition
                                .vectorTo(VectorTool.lerpGetEntityBoundingBoxCenter(seekingEntity!!, 1f))
                        ) > seekAngle || !SeekTool.NOT_IN_SMOKE.test(seekingEntity) || !noClip(
                            player,
                            seekingEntity!!
                        ))
                    ) {
                        seekFailure(player)
                    }

                    if (zoomTime > 0.7) {
                        if (seekingEntity == null) {
                            seekingEntity = nearestEntity
                        }
                        if (nearestEntity != null && data.hasEnoughAmmoToShoot(player)) {
                            seekingTime++
                            if ((!seekingEntity!!.passengers.isEmpty()
                                        || seekingEntity is VehicleEntity) && player.tickCount % 3 == 0 && !lockOn
                            ) {
                                sendPacketToServer(SeekingWeaponWarningMessage(false, seekingEntity!!.getUUID()))
                            }
                        }
                    } else {
                        seekFailure(player)
                    }

                    if (lockOn && holdingFireKey && lockingEntity != null) {
                        sendPacketToServer(
                            ShootMessage(
                                gunSpread,
                                zoom,
                                lockingEntity!!.getUUID(),
                                lockingEntity!!.eyePosition.toVector3f()
                            )
                        )
                        holdingFireKey = false
                    }
                }
            } else {
                seekFailure(player)
            }

            if (nearestEntity != null && nearestEntity!!.type.`is`(ModTags.EntityTypes.DECOY)) {
                seekFailure(player)
            }

            if (lockingEntity != null && !lockingEntity!!.isAlive) {
                seekFailure(player)
            }

            if (seekingTime == 2) {
                playLockingSound(data, player)
            }

            if (seekingTime > lockTime) {
                playLockedSound(data, player)
                if (guideType == 0 && lockingEntity != null && (!lockingEntity!!.passengers.isEmpty()
                            || lockingEntity is VehicleEntity) && player.tickCount % 2 == 0
                ) {
                    sendPacketToServer(
                        SeekingWeaponWarningMessage(
                            true,
                            lockingEntity!!.uuid
                        )
                    )
                }
            }
        }
    }

    fun vehicleWeaponSeeking(player: Player) {
        val vehicle = player.vehicle as? VehicleEntity ?: return
        val data = vehicle.getGunData(player) ?: return
        val seekWeaponInfo = data.get(GunProp.SEEK_WEAPON_INFO) ?: return

        // 锁定所需时间
        val lockTime = seekWeaponInfo.seekTime
        // 搜寻角度
        val seekAngle = seekWeaponInfo.seekAngle
        // 搜索范围
        val seekRange = seekWeaponInfo.seekRange
        // 视角位置
        val cameraPos = mc.gameRenderer.mainCamera.position
        // 搜寻方向
        val seekVec = vehicle.getSeekVec(player, 1f) ?: return
        // 最小目标高度
        val minTargetHeight = seekWeaponInfo.minTargetHeight
        // 最大目标高度
        val maxTargetHeight = seekWeaponInfo.maxTargetHeight
        // 最小目标碰撞箱大小
        val minTargetSize = seekWeaponInfo.minTargetSize
        // 能被友方雷达引导的最大锁定范围
        val maxGuidedRange = seekWeaponInfo.maxGuidedRange
        // 能否友方雷达引导
        val canGuidedByRadar = seekWeaponInfo.canGuidedByRadar
        // 是否能被隐身目标影响
        val affectedByStealthTarget = seekWeaponInfo.affectedByStealthTarget

        nearestEntityVehicle = SeekTool.Builder(player)
            .withinRangeSeekWeapon(seekRange, maxGuidedRange, affectedByStealthTarget, canGuidedByRadar)
            .withinAngle(cameraPos, seekVec, seekAngle)
            .baseFilter()
            .heightRange(minTargetHeight, maxTargetHeight)
            .sizeBiggerThan(minTargetSize)
            .smokeFilter()
            .noVehicle()
            .noClip()
            .notFriendly()
            .buildWithClosest(cameraPos, seekVec, canGuidedByRadar)

        val decoy = TraceTool.findLookDecoy(player, cameraPos, seekVec, seekRange)
        if (decoy != null && decoy.type.`is`(ModTags.EntityTypes.DECOY)) {
            nearestEntityVehicle = decoy
            seekFailure(player)
        }

        if (seekWeaponInfo.onlyLockBlock) {
            // 锁定方块
            val result = player.level().clip(
                ClipContext(
                    cameraPos, cameraPos.add(seekVec.scale(seekRange)),
                    ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, player
                )
            )
            seekingPosVehicle = result.location

            if (seekingTimeVehicle > lockTime + 2 && !lockOnVehicle) {
                lockOnVehicle = true
            }

            // 锁定失败
            if (lockingPosVehicle != null && (seekVec.angleTo(cameraPos.vectorTo(lockingPosVehicle!!)) > seekAngle
                        || !noClip(player, lockingPosVehicle!!))
            ) {
                seekFailure(player)
            }

            if (ModKeyMappings.VEHICLE_SEEK.isDown) {
                if (seekingPosVehicle != null && seekingPosVehicle!!.distanceToSqr(cameraPos) < seekRange * seekRange) {
                    seekingTimeVehicle++
                    if (seekingTimeVehicle == 1) {
                        lockingPosVehicle = seekingPosVehicle
                    }
                } else {
                    seekFailure(player)
                }
            } else {
                seekFailure(player)
            }
        } else if (seekWeaponInfo.onlyLockEntity) {
            // 锁定实体
            if (seekingTimeVehicle > lockTime + 2 && !lockOnVehicle) {
                lockingEntityVehicle = seekingEntityVehicle
                lockOnVehicle = true
            }

            if (ModKeyMappings.VEHICLE_SEEK.isDown()) {
                if (seekingEntityVehicle == null) {
                    seekingEntityVehicle = nearestEntityVehicle
                }
                if (seekingEntityVehicle != null && lockingPosVehicle == null) {
                    seekingTimeVehicle++
                    if ((!seekingEntityVehicle!!.getPassengers()
                            .isEmpty() || seekingEntityVehicle is VehicleEntity) && player.tickCount % 3 == 0 && !lockOnVehicle
                    ) {
                        sendPacketToServer(
                            SeekingWeaponWarningMessage(
                                false,
                                seekingEntityVehicle!!.getUUID()
                            )
                        )
                    }
                }
            } else {
                seekFailure(player)
            }
        }

        // 锁定失败
        if (seekingEntityVehicle != null &&
            (seekVec.angleTo(
                cameraPos.vectorTo(
                    VectorTool.lerpGetEntityBoundingBoxCenter(
                        seekingEntityVehicle!!,
                        1f
                    )
                )
            ) > seekAngle
                    || !SeekTool.NOT_IN_SMOKE.test(seekingEntityVehicle)
                    || !noClip(player, seekingEntityVehicle!!))
        ) {
            seekFailure(player)
        }

        if (lockingEntityVehicle != null && !lockingEntityVehicle!!.isAlive) {
            seekFailure(player)
        }

        if (seekingTimeVehicle == 2) {
            playLockingSound(data, player)
        }

        if (seekingTimeVehicle > lockTime) {
            playLockedSound(data, player)
            if (seekWeaponInfo.onlyLockEntity && lockingEntityVehicle != null && (!lockingEntityVehicle!!.passengers.isEmpty()
                        || lockingEntityVehicle is VehicleEntity) && player.tickCount % 2 == 0
            ) {
                sendPacketToServer(
                    SeekingWeaponWarningMessage(
                        true,
                        lockingEntityVehicle!!.getUUID()
                    )
                )
            }
        }
    }

    fun seekFailure(player: Player) {
        seekingTimeVehicle = 0
        lockOnVehicle = false
        lockingEntityVehicle = null
        seekingEntityVehicle = null
        lockingPosVehicle = null
        seekingTime = 0
        lockOn = false
        lockingEntity = null
        seekingEntity = null
        lockingPos = null
        VehicleMainWeaponHudOverlay.lock = false
        stopVehicleSeekSound(player)
    }

    fun playLockingSound(data: GunData, player: Player) {
        val soundInfo = data.get(GunProp.SOUND_INFO)
        val sound = soundInfo.locking
        player.playSound(sound, 2f, 1f)
    }

    fun playLockedSound(data: GunData, player: Player) {
        val soundInfo = data.get(GunProp.SOUND_INFO)
        val sound = soundInfo.locked
        player.playSound(sound, 2f, 1f)
    }

    fun noClip(entity: Entity, e: Entity): Boolean {
        return entity.level()
            .clip(
                ClipContext(
                    entity.eyePosition,
                    e.eyePosition,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    entity
                )
            )
            .type != HitResult.Type.BLOCK
    }

    fun noClip(entity: Entity, pos: Vec3): Boolean {
        return entity.level()
            .clip(
                ClipContext(
                    entity.eyePosition,
                    pos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    entity
                )
            )
            .type != HitResult.Type.BLOCK
    }

    fun weaponZooming(stack: ItemStack) {
        if (GunItem.isOperable(stack)) {
            sendPacketToServer(WeaponZoomingMessage(zoomTime >= 0.7))
        }
    }

    // 耐力
    fun staminaSystem() {
        if (mc.isPaused) return
        if (localPlayer == null) return

        if (breath) {
            stamina += 0.5f
        } else if (stamina > 0) {
            stamina = (stamina - 0.5f).coerceAtLeast(0f)
        }

        if (stamina >= 100) {
            exhaustion = true
            breath = false
        }

        if (exhaustion && stamina <= 0) {
            exhaustion = false
        }

        if ((ModKeyMappings.BREATH.isDown() && zoom)) {
            switchTime = (switchTime + 0.65).coerceAtMost(5.0)
        } else if (switchTime > 0 && stamina == 0f) {
            switchTime = (switchTime - 0.15).coerceAtLeast(0.0)
        }
    }

    /**
     * 禁止玩家奔跑
     */
    fun handlePlayerSprint() {
        val player = localPlayer ?: return

        if (player.isShiftKeyDown
            || player.isPassenger
            || player.isInWater
            || zoom
        ) {
            noSprintTicks = 3f
        }

        if (noSprintTicks > 0) {
            noSprintTicks--
        }

        if (zoom || holdingFireKey) {
            player.isSprinting = false
        }
    }

    private fun handleVariableDecrease() {
        if (holdingFireKeyTicks > 0 && !holdingFireKey) {
            holdingFireKeyTicks--
            if (holdingFireKeyTicks == 0) {
                holdingFireKeyTicks0 = 0f
            }
        }

        if (dismountCountdown > 0) {
            dismountCountdown--
        }

        if (aimVillagerCountdown > 0) {
            aimVillagerCountdown--
        }

        if (switchVehicleWeaponCooldown > 0) {
            switchVehicleWeaponCooldown--
        }
    }

    @JvmStatic
    fun isProne(player: Player): Boolean {
        val level = player.level()
        if (player.pose == Pose.SWIMMING && !player.isSwimming) return true
        val forward = Vec3(player.lookAngle.x, 0.0, player.lookAngle.z).normalize()
        return player.isCrouching && level.getBlockState(
            BlockPos.containing(
                player.x + 0.7 * forward.x,
                player.y + 0.5,
                player.z + 0.7 * forward.z
            )
        ).canOcclude()
                && !level.getBlockState(
            BlockPos.containing(
                player.x + 0.7 * forward.x,
                player.y + 1.5,
                player.z + 0.7 * forward.z
            )
        ).canOcclude()
    }

    /**
     * 近战入口。
     *
     * 与旧实现的区别：
     * - 状态（连招下标 / 动作锁 / 本段时长）按**枪身份隔离**，放在 [com.atsuishio.superbwarfare.client.gun.GunActionLock]
     *   里，切枪不会拿新枪的数据误触发一次攻击（缺陷 1）；
     * - 旧的全局 `gunMelee` 计数器已不再被这里驱动（只为旧 GeckoLib 路径保留成编译占位），
     *   动画状态改问 [isGunMeleeActive]；
     * - 冷却判断里的原版物品冷却（`player.cooldowns.isOnCooldown(item)`）是死条件，已删（缺陷 11）；
     * - **动作锁的计时在这里无条件推进**（不只是手上有枪的时候），否则主手切走会让
     *   `FIRING`/`MELEE` 占用永远挂着。
     *
     * @param stack 主手物品
     */
    fun handleGunMelee(player: Player, stack: ItemStack) {
        // 动作锁计时：只要有活跃的动作状态就推进（帧率无关，每客户端 tick 一次）
        val gunData = if (GunItem.isHeldWeapon(stack)) GunData.from(stack) else null
        val actionState = gunData?.let { GunActionLock.of(it) }
        actionState?.tick()

        // ⚠ **部署中的副武器那把锁也必须推进**（四期返修，§11.10.12）。
        //
        // `SubWeaponClientHandler.onDeployed` 是在**当前操控的那把枪**上占用 `SUB_WEAPON` 的，
        // 而副武器被切出来时"当前操控的那把枪"**就是副武器** —— 上面那句只推进了**主手**那把锁，
        // 副武器那把是全仓**唯一没有任何人递减**的 `State`，于是 `SUB_WEAPON` 一占就永远挂着：
        //
        // - `handleGunShoot` 的 `GunActionLock.of(data).blocks(FIRING)` → **开不了火**；
        // - 近战门禁的 `GunActionLock.of(operated).blocks(MELEE)` → **近战不了**。
        //
        // 这个坑和本方法 KDoc 里那条"动作锁计时要无条件推进，否则主手切走会让 FIRING/MELEE
        // 永远挂着"是同一个：**任何一个可能被 `force`/`acquire` 的 `State` 都必须有人推进它。**
        if (gunData != null) {
            val operated = ActiveGun.dataOf(gunData, player.level().isClientSide)
            if (operated !== gunData) {
                GunActionLock.of(operated).tick()
            }
        }

        if (gunData == null || actionState == null) return

        // 切换请求的兜底超时（丢一次包不该把 G 永久锁死，也不能让动作锁一直挂着）
        SubWeaponClientHandler.tick(gunData)

        MeleeClientHandler.tick(
            player = player,
            stack = stack,
            meleeKeyDown = ModKeyMappings.MELEE.isDown(),
            subWeaponFireKeyDown = ModKeyMappings.SUBWEAPON_FIRE.isDown(),
            holdingFireKey = holdingFireKey,
            drawTime = drawTime,
            canOperate = !holdFireVehicle
                    && !notInGame
                    && !isEditing
                    && !(player.vehicle is VehicleEntity && (player.vehicle as VehicleEntity).banHand(player)),
            skipStateTick = true,
        )
    }

    /**
     * 这把枪当前是否正在挥击近战（动画状态机用）。
     *
     * 旧实现读全局的 `gunMelee` 计数器，现在读按枪隔离的动作状态。
     */
    @JvmStatic
    fun isGunMeleeActive(stack: ItemStack): Boolean {
        if (!GunItem.isHeldWeapon(stack)) return false
        return GunActionLock.of(stack).meleeTicks > 0
    }

    /** 当前这一段挥击的时长（动画按它拉伸） */
    @JvmStatic
    fun currentMeleeDuration(stack: ItemStack): Int {
        if (!GunItem.isHeldWeapon(stack)) return 0
        return GunActionLock.of(stack).meleeDuration
    }

    /** 当前这一段挥击锁存的下标（解析 clip 名用） */
    @JvmStatic
    fun currentMeleeIndex(stack: ItemStack): Int {
        if (!GunItem.isHeldWeapon(stack)) return 0
        return GunActionLock.of(stack).meleeActionIndex
    }

    fun handleLungeAttack(player: Player, stack: ItemStack) {
        if (stack.`is`(ModItems.LUNGE_MINE.get()) && lungeAttack == 0 && lungeDraw == 0 && usingLunge) {
            lungeAttack = 18
            usingLunge = false
            player.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1f)
        }

        if (stack.`is`(ModItems.LUNGE_MINE.get()) && ((lungeAttack >= 9 && lungeAttack <= 10.5) || lungeSprint > 0)) {
            val lookingEntity = OverlayTraceHandler.playerReachEntity

            val result = player.level().clip(
                ClipContext(
                    player.eyePosition,
                    player.eyePosition.add(player.lookAngle.scale(player.getBlockReach() + 0.5)),
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player
                )
            )

            val looking = Vec3.atLowerCornerOf(
                player.level().clip(
                    ClipContext(
                        player.eyePosition,
                        player.eyePosition.add(player.lookAngle.scale(player.getBlockReach() + 0.5)),
                        ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.NONE,
                        player
                    )
                ).blockPos
            )
            val blockState = player.level().getBlockState(
                BlockPos.containing(
                    looking.x,
                    looking.y,
                    looking.z
                )
            )

            if (lookingEntity != null) {
                sendPacketToServer(LungeMineAttackMessage(0, lookingEntity.getUUID(), result.location))
                lungeSprint = 0
                lungeAttack = 0
                lungeDraw = 15
            } else if ((blockState.canOcclude() || blockState.block is DoorBlock
                        || blockState.block is CrossCollisionBlock || blockState.block is BellBlock) && lungeSprint == 0
            ) {
                sendPacketToServer(LungeMineAttackMessage(1, player.getUUID(), result.location))
                lungeSprint = 0
                lungeAttack = 0
                lungeDraw = 15
            }
        }

        if (lungeSprint > 0) {
            lungeSprint--
        }

        if (lungeAttack > 0) {
            lungeAttack--
        }

        if (lungeDraw > 0) {
            lungeDraw--
        }
    }

    @SubscribeEvent
    fun handleWeaponFire(@Suppress("unused") event: RenderFrameEvent.Pre) {
        if (mc.fps > 20) {
            handleVehicleGunShoot()
            handleGunShoot()
        }
    }

    /**
     * 当前实际射速：`(基础 RPM + 每发累加值) * 全局倍率`。
     *
     * - 每发累加值可能为负（[GunProp.CUSTOM_RPM_MIN] 允许负数）、倍率也可能小于 1，
     *   所以这里必须保证结果 >= 1：rpm <= 0 会让 cooldown 变成非正数，
     *   下面按 cooldown 递减的开火补帧循环就永远结束不了。
     * - 开火节奏和 HUD 显示都走这里，避免两处算法跑偏。
     */
    fun effectiveRpm(data: GunData): Int =
        ((data.get(GunProp.RPM) + customRpm) * data.get(GunProp.RPM_MULTIPLIER)).roundToInt().coerceIn(1, 114514)

    fun handleGunShoot() {
        if (clientLevel == null) return
        val player = localPlayer ?: return

        if (notInGame) {
            holdingFireKey = false
        }

        val stack = ActiveGun.stackOf(player)
        val item = stack.item as? GunItem
        // 主手不是枪（或拿着副武器**物品**本身）时什么都不做。
        // 注意判据是 `isOperable` 而不是 `isHeldWeapon`：`ActiveGun` 可能返回**部署中的副武器栈**，
        // 而副武器的 `useAsWeaponInHand()` 是 false（§9.8.1 的坑）。
        if (item == null || !GunItem.isOperable(stack)) {
            clientTimer.stop()
            fireSpread = 0.0
            gunSpread = 0.0
            return
        }

        val data = GunData.from(stack)
        val resource = GunResource.compute(stack)
        val fireModeInfo = data.selectedFireModeInfo()
        val mode = fireModeInfo.mode
        val chargeConfig = fireModeInfo.chargeConfig()
        val singleShotMode = mode == FireMode.SEMI

        // 动作互斥：近战/副武器/换弹占用期间不开火。
        // 这里刻意放在最前面：被拒绝的入口不该产生任何副作用（包括后面的计时器推进）。
        if (GunActionLock.of(data).blocks(GunAction.FIRING)) {
            clientTimer.stop()
            fireSpread = 0.0
            return
        }

        val chargeDelay = chargeConfig?.effectiveDuration?.toDouble()
            ?: data.get(GunProp.SHOOT_DELAY).toDouble()

        val partialHoldingFireKeyTicks =
            Mth.lerp(getDelta().toDouble(), holdingFireKeyTicks0.toDouble(), holdingFireKeyTicks.toDouble())
        holdingFireKeyTicks0 = holdingFireKeyTicks.toFloat()

        if (partialHoldingFireKeyTicks > holdingFireKeyTicks
            && partialHoldingFireKeyTicks > chargeDelay * 0.25 && shouldPlayDischargeSound
        ) {
            val dischargeSound = resource.dischargeSound
            if (dischargeSound != null) {
                player.playSound(
                    dischargeSound,
                    partialHoldingFireKeyTicks.toFloat() * 0.03f,
                    0.6f + partialHoldingFireKeyTicks.toFloat() * 0.02f
                )
            }

            shouldPlayDischargeSound = false
            burstFireAmount = 0
        }

        if (fireModeInfo.isChargeMode()) {
            updateChargeFireState(player, data, fireModeInfo)
            return
        }

        if (!item.canShoot(data, player)) {
//            if (!data.meleeOnly()) {
//                holdingFireKey = false
//            }
            burstFireAmount = 0
        }

        // 精准度
        val times = getDelta().coerceAtMost(0.8f)

        val basicDev = data.get(GunProp.SPREAD)
        val walk = if (isMoving()) 0.3 * basicDev else 0.0
        val sprint = if (player.isSprinting) 0.25 * basicDev else 0.0
        val crouching = if (player.isCrouching) -0.15 * basicDev else 0.0
        val prone = if (isProne(player)) -0.3 * basicDev else 0.0
        val jump = if (player.onGround()) 0.0 else 0.35 * basicDev
        val ride = if (player.onGround()) -0.25 * basicDev else 0.0

        val zoomSpread = 1 - (1 - data.get(GunProp.ZOOM_SPREAD_RATE)) * zoomTime
        val spread =
            if (data.isShotgun) 1.2 * zoomSpread * (basicDev + 0.2 * (walk + sprint + crouching + prone + jump + ride) + fireSpread)
            else zoomSpread * (0.7 * basicDev + walk + sprint + crouching + prone + jump + ride + 0.8 * fireSpread)

        gunSpread = Mth.lerp(0.5 * times, gunSpread, spread)

        // 开火部分
        // 冲刺后恢复开火的快慢按**主武器**的重量（`ActiveGun.handlingData`）—— 这一项是手感不是弹道
        val weight = (ActiveGun.handlingData(player) ?: data).get(GunProp.WEIGHT)
        val speed = 5 / (weight + 4)

        fireCooldown = if (noSprintTicks == 0f && player.isSprinting && !zoom && !holdingFireKey) {
            (fireCooldown + 3 * times).coerceIn(0.0, 24.0)
        } else {
            (fireCooldown - 6 * speed * times).coerceIn(0.0, 40.0)
        }

        val rpm = effectiveRpm(data)
        val rps = rpm / 60.0

        // cooldown in ms
        val cooldown = (1000 / rps).roundToInt()

        // 左轮类
        if (clientTimer.progress == 0L && stack.`is`(ModItems.TRACHELIUM.get()) && holdingFireKey) {
            revolverPreTime = (revolverPreTime + 0.3 * times).coerceIn(0.0, 1.0)
            revolverWheelPreTime =
                (revolverWheelPreTime + 0.32 * times).coerceIn(0.0, if (revolverPreTime > 0.7) 1.0 else 0.55)
        } else {
            revolverPreTime = (revolverPreTime - 1.2 * times).coerceIn(0.0, 1.0)
        }

        val vehicle = player.vehicle
        if (((holdingFireKey || burstFireAmount > 0) && holdingFireKeyTicks >= data.get(GunProp.SHOOT_DELAY))
            && !(vehicle is VehicleEntity && vehicle.banHand(player))
            && !holdFireVehicle
            && item.canShoot(data, player)
            && !item.useSpecialFireProcedure(data)
            && fireCooldown == 0.0
            && sprintBasicRotX * sprintBasicRotY * sprintBasicRotZ < 0.0001
            && drawTime < 0.01
            && !notInGame
            && !isEditing
        ) {
            if (singleShotMode) {
                if (clientTimer.progress == 0L) {
                    clientTimer.start()
                    shootClient(player)
                }
            } else {
                if (!clientTimer.started()) {
                    clientTimer.start()
                    // 首发瞬间发射
                    clientTimer.progress = cooldown.toLong() + 1L
                }

                if (clientTimer.progress >= cooldown) {
                    var newProgress = clientTimer.progress

                    // 低帧率下的开火次数补偿
                    do {
                        shootClient(player)
                        newProgress -= cooldown
                    } while (newProgress - cooldown > 0)

                    clientTimer.progress = newProgress
                }
            }

            if (notInGame) {
                clientTimer.stop()
            }
        } else {
            if (!singleShotMode && clientTimer.progress >= cooldown) {
                clientTimer.stop()
            }
            fireSpread = 0.0
        }

        if (singleShotMode && clientTimer.progress >= cooldown) {
            clientTimer.stop()
        }

        if (GunData.from(stack).reload.normal() || GunData.from(stack).reload.empty()) {
            customRpm = 0
        }
    }

    private fun updateChargeFireState(player: Player, data: GunData, fireModeInfo: FireModeInfo) {
        val chargeConfig = fireModeInfo.chargeConfig() ?: return
        val item = data.item
        val vehicle = player.vehicle
        chargeActive = holdingFireKey
                && !notInGame
                && !isEditing
                && !holdFireVehicle
                && !(vehicle is VehicleEntity && vehicle.banHand(player))
                && item.canShoot(data, player)

        if (!chargeActive) {
            chargeProgress = 0.0
            chargePower = 0.0
            return
        }

        val progress = (holdingFireKeyTicks.toDouble() / chargeConfig.effectiveDuration.toDouble()).coerceIn(0.0, 1.0)
        chargeProgress = progress
        chargePower = chargeConfig.powerForProgress(progress)

        if (chargeConfig.trigger == ChargeTrigger.AUTO_AT_FULL
            && progress >= 1.0
            && clientTimer.progress == 0L
            && fireCooldown == 0.0
            && drawTime < 0.01
        ) {
            clientTimer.start()
            shootClient(player, chargePower)
            clientTimer.stop()
            chargeActive = false
            chargeProgress = 0.0
            chargePower = 0.0
        }
    }

    fun shootClient(player: Player, chargePower: Double = 1.0) {
        val stack = ActiveGun.stackOf(player)
        val item = stack.item as? GunItem ?: return
        if (!GunItem.isOperable(stack)) return

        val data = GunData.from(stack)
        if (!item.canShoot(data, player) || item.useSpecialFireProcedure(data)) return

        val mode = data.selectedFireModeInfo().mode
        if (mode != FireMode.AUTO) {
            holdingFireKey = false
        }

        if (data.get(GunProp.CLEAR_HOLD_PROGRESS_AFTER_SHOOT)) {
            holdingFireKeyTicks = 0
        }

        if (mode == FireMode.BURST && burstFireAmount == 1) {
            fireCooldown = data.get(GunProp.BURST_COOLDOWN).toDouble()
        }

        // 动作锁：开火占用 = 一个射击周期。连发期间会反复 acquire 同一个动作，
        // `blocks()` 允许"自己"通过，所以不会卡住连射。
        val cycleTicks = if (mode == FireMode.BURST && burstFireAmount > 0) {
            data.get(GunProp.BURST_COOLDOWN)
        } else {
            val rpm = effectiveRpm(data)
            ((60000.0 / rpm.coerceAtLeast(1)) / 50.0).toInt()
        }
        GunActionLock.of(data).force(GunAction.FIRING, cycleTicks.coerceAtLeast(1))

        if (burstFireAmount > 0) {
            burstFireAmount--
        }

        for (type in Perk.Type.entries) {
            val instance = data.perk.getInstances(type)
            customRpm = instance.maxOfOrNull { it.perk.getModifiedCustomRPM(customRpm, data, it) } ?: customRpm
        }

        val minCustomRpm = data.get(GunProp.CUSTOM_RPM_MIN)
        val maxCustomRpm = data.get(GunProp.CUSTOM_RPM_MAX)
        // 用 Mth.clamp 而不是 coerceIn：数据包写反上下限时也不会抛异常
        customRpm = Mth.clamp(customRpm + data.get(GunProp.RPM_ADD_AFTER_SHOOT), minCustomRpm, maxCustomRpm)

        // 判断是否为栓动武器（BoltActionTime > 0），并在开火后给一个需要上膛的状态
        // 这是纯客户端预测：用 updateLocal 只改内存，不写 stack、也不 bump revision。枪械数据由服务端
        // 权威修改后同步（GunItem.beforeShoot 会在服务端设同一个字段），本地预测会在下一次同步被覆盖。
        if (data.get(GunProp.BOLT_ACTION_TIME) > 0 && data.hasEnoughAmmoToShoot(player)) {
            data.updateLocal { it.copy(needBoltAction = true) }
        }

        revolverPreTime = 0.0
        revolverWheelPreTime = 0.0

        playGunClientSounds(player)
        handleClientShoot(chargePower)
    }

    fun handleClientShoot(chargePower: Double = 1.0) {
        val player = localPlayer ?: return
        val stack = ActiveGun.stackOf(player)
        if (!GunItem.isOperable(stack)) return
        val data = GunData.from(stack)

        sendPacketToServer(
            ShootMessage(
                gunSpread,
                zoom,
                if (lockedEntity != null) lockedEntity!!.getUUID() else null,
                null,
                chargePower
            )
        )

        // 开火窗口：**主武器与副武器各有一个，二选一**（§11.9-D）。
        //
        // - 副武器开火 → 开 `subWeaponFireRotTimer`：枪口焰画在**副武器模型自己的** `flare` 上
        //   （`MuzzleFlashRenderer` 的 subWeapon 分支），而且**不驱动整把枪的后坐** ——
        //   那一发的后坐由副武器自己的 `fire_sub_weapon` 动画负责，两边叠加会抖两下。
        // - 主武器开火 → 开 `fireRotTimer`，并把副武器的窗口清零：枪口焰立刻回到**主武器的**枪口。
        //
        // ⚠ 四期把"设置副武器那个窗口"的一行弄丢了：它原本在三期
        // `SubWeaponClientHandler.playFireAnimation` 里（`subWeaponFireRotTimer = 0.001`，
        // 由已删除的 `SubWeaponFiredMessage` 触发），四期删掉那条链路时**只搬来了清零的那一半**
        // （下面原来那行 `subWeaponFireRotTimer = 0.0`），于是：
        // 副武器开火时 `fireRotTimer` 照开 → **枪管前端喷火、整把枪跟着做后坐**，
        // 而榴弹筒自己一帧枪口焰都没有。见 §11.10.11。
        if (ActiveGun.isSubWeapon(data)) {
            subWeaponFireRotTimer = SUB_WEAPON_FLASH_START
            // 枪身位形与拉栓由副武器自己的开火动画（宿主枪的 `fire_sub_weapon`）表现，
            // 但**镜头**要有后坐 —— 另开一个只驱动镜头的相位窗口（见 `subWeaponRecoilTimer`）
            subWeaponRecoilTimer = SUB_WEAPON_RECOIL_START
            fireRecoilTime = 0.0
        } else {
            subWeaponFireRotTimer = 0.0
            // 镜头后坐回到主武器那一套：两个相位窗口不能同时开（否则抬枪抬两次）
            subWeaponRecoilTimer = 0.0
            fireRecoilTime = 10.0
        }

        // Spawn dynamic block light muzzle flash for firearms using unified muzzle node
        val flashParams = MuzzleFlashHelper.calculateFromStack(stack)
        if (flashParams != null) {
            MuzzleFlashHelper.spawnFlashCone(player.eyePosition, player.lookAngle, flashParams)
        }

        // 真实后坐（
        if (data.get(GunProp.RECOIL) != 0.0) {
            player.deltaMovement = player.deltaMovement.add(player.getViewVector(1f).scale(-data.get(GunProp.RECOIL)))
        }

        val gunRecoilY = data.get(GunProp.RECOIL_Y) * 10

        recoilY = (2 * Math.random() - 1).toFloat() * gunRecoilY

        if (shellIndex < 5) {
            shellIndex++
        }

        noSprintTicks = 7f

        shellIndexTime[shellIndex] = 0.001

        randomShell[0] = (1 + 0.2 * (Math.random() - 0.5))
        randomShell[1] = (0.2 + (Math.random() - 0.5))
        randomShell[2] = (0.7 + (Math.random() - 0.5))

        postEvent(ClientGunFireEvent(player, stack))
    }

    fun playGunClientSounds(player: Player) {
        val stack = ActiveGun.stackOf(player)
        val item = stack.item as? GunItem
        // 主手不是枪时什么都不播；部署中的副武器走的是同一条链路（§9.8.3）
        if (item == null || !GunItem.isOperable(stack)) return

        if (item == ModItems.SENTINEL.get()) {
            val cap = stack.getCapability(Capabilities.EnergyStorage.ITEM)
            val charged = cap != null && cap.energyStored > 0

            if (charged) {
                player.playSound(
                    ModSounds.SENTINEL_CHARGE_FIRE_1P.get(),
                    2f,
                    ((2 * Math.random() - 1) * 0.05f + 1.0f).toFloat()
                )
                return
            }
        }

        if (item == ModItems.SECONDARY_CATACLYSM.get()) {
            val cap = stack.getCapability(Capabilities.EnergyStorage.ITEM)
            val hasEnoughEnergy = cap != null && cap.energyStored > 3000

            val isChargedFire = zoom && hasEnoughEnergy

            if (isChargedFire) {
                player.playSound(
                    ModSounds.SECONDARY_CATACLYSM_FIRE_1P_CHARGE.get(),
                    2f,
                    ((2 * Math.random() - 1) * 0.05f + 1.0f).toFloat()
                )
                return
            }
        }

        val data = GunData.from(stack)

        playGunFire1PSound(player, data)

        val shooterHeight = player.eyePosition.distanceTo(
            (Vec3.atLowerCornerOf(
                player.level().clip(
                    ClipContext(
                        player.eyePosition,
                        player.eyePosition.add(
                            Vec3(0.0, -1.0, 0.0).scale(10.0)
                        ),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player
                    )
                ).blockPos
            ))
        )

        queueClientWorkIfDelayed((1 + 1.5 * shooterHeight).toInt()) {
            if (GunResource.compute(stack).ejectShell) {
                if (data.selectedAmmoConsumer().type == AmmoConsumer.AmmoConsumeType.PLAYER_AMMO) {
                    val ammoType: Ammo = data.selectedAmmoConsumer().playerAmmoType!!
                    when (ammoType) {
                        Ammo.SHOTGUN ->
                            player.playSound(
                                ModSounds.SHELL_CASING_SHOTGUN.get(),
                                (0.75 - 0.12 * shooterHeight).coerceAtLeast(0.0).toFloat(),
                                ((2 * Math.random() - 1) * 0.05f + 1.0f).toFloat()
                            )

                        Ammo.SNIPER, Ammo.HEAVY ->
                            player.playSound(
                                ModSounds.SHELL_CASING_50CAL.get(),
                                (1 - 0.15 * shooterHeight).coerceAtLeast(0.0).toFloat(),
                                ((2 * Math.random() - 1) * 0.05f + 1.0f).toFloat()
                            )

                        else ->
                            player.playSound(
                                ModSounds.SHELL_CASING_NORMAL.get(),
                                (1.5 - 0.2 * shooterHeight).coerceAtLeast(0.0).toFloat(),
                                ((2 * Math.random() - 1) * 0.05f + 1.0f).toFloat()
                            )
                    }
                } else {
                    player.playSound(
                        ModSounds.SHELL_CASING_NORMAL.get(),
                        (1.5 - 0.2 * shooterHeight).coerceAtLeast(0.0).toFloat(),
                        ((2 * Math.random() - 1) * 0.05f + 1.0f).toFloat()
                    )
                }
            }
        }
    }

    /**
     * 一把枪的**第一人称开火音**（含 `REFLECTIONS` 尾音），口径收在
     * [com.atsuishio.superbwarfare.item.gun.GunItem.resolveFire1PSounds] 里，与副武器共用同一份参数。
     *
     * 抽出来的原因：副武器的开火完全发生在服务端，而本地音必须在客户端播 ——
     * 服务端算好参数发给射手（`LocalSoundMessage`），客户端只负责出声。
     * 这样"未装填好按 G"不可能响（服务端根本没开火），音量/音高又与主武器逐字一致。
     *
     * @param data 要播开火音的枪械数据；主武器传手持栈的，副武器传它自己那份。
     */
    @JvmStatic
    fun playGunFire1PSound(player: Player, data: GunData) {
        for (sound in data.item.resolveFire1PSounds(data)) {
            player.playSound(sound.sound, sound.volume, sound.pitch)
        }
    }

    fun handleVehicleGunShoot() {
        if (clientLevel == null) return
        val player = localPlayer ?: return

        if (notInGame) {
            clientTimerVehicle.stop()
            holdFireVehicle = false
        }

        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.hasWeapon(vehicle.getSeatIndex(player))) {
            val gunData = vehicle.getGunData(vehicle.getSeatIndex(player)) ?: return

            if (!vehicle.canShoot(player)) {
                holdFireVehicle = false
                return
            }

            var rpm = vehicle.vehicleWeaponRpm(player)
            if (rpm == 0) {
                rpm = 240
            }

            val rps = rpm / 60.0
            val cooldown = (1000 / rps).roundToInt()

            if (holdFireVehicle) {
                if (gunData.get(GunProp.DEFAULT_FIRE_MODE) == "Semi") {
                    if (clientTimerVehicle.progress == 0L) {
                        clientTimerVehicle.start()
                        clientShootVehicle(player, vehicle, gunData)
                    }
                } else {
                    if (!clientTimerVehicle.started()) {
                        clientTimerVehicle.start()
                        // 首发瞬间发射
                        clientTimerVehicle.progress = cooldown.toLong() + 1L
                    }

                    if (clientTimerVehicle.progress >= cooldown) {
                        var newProgress = clientTimerVehicle.progress

                        // 低帧率下的开火次数补偿
                        do {
                            clientShootVehicle(player, vehicle, gunData)
                            newProgress -= cooldown
                        } while (newProgress - cooldown > 0)

                        clientTimerVehicle.progress = newProgress
                    }
                }

                if (notInGame) {
                    clientTimerVehicle.stop()
                }
            } else if (clientTimerVehicle.progress >= cooldown) {
                clientTimerVehicle.stop()
            }
        } else {
            clientTimerVehicle.stop()
        }
    }

    fun clientShootVehicle(player: Player, vehicle: VehicleEntity, gunData: GunData) {
        sendPacketToServer(
            VehicleFireMessage(
                if (lockingEntityVehicle != null) lockingEntityVehicle!!.uuid else null,
                if (lockingPosVehicle != null) lockingPosVehicle!!.toVector3f() else (if (gunData.get(GunProp.SEEK_WEAPON_INFO)?.inputBlockPos == true) missileLockingPos?.center?.toVector3f() else null)
            )
        )
        if (mc.options.cameraType == CameraType.FIRST_PERSON || zoomVehicle) {
            playVehicleClientSounds(player, vehicle)
        }
    }

    fun playVehicleClientSounds(player: Player, vehicle: VehicleEntity) {
        val gunData = vehicle.getGunData(vehicle.getSeatIndex(player)) ?: return
        val soundInfo = gunData.get(GunProp.SOUND_INFO)
        val sound = soundInfo.fire1P ?: return

        val pitch =
            if (vehicle.getWeaponHeat(player) <= 60) 1f else (1 - 0.011 * abs(60 - vehicle.getWeaponHeat(player))).toFloat()
        player.playSound(sound, 1f, pitch)
    }

    @SubscribeEvent
    fun handleWeaponBreathSway(@Suppress("unused") event: RenderFrameEvent.Pre) {
        val player = localPlayer ?: return
        val stack = ActiveGun.stackOf(player)
        val item = stack.item as? GunItem ?: return
        if (!GunItem.isOperable(stack)) return
        val vehicle = player.vehicle

        if (vehicle is VehicleEntity && player == vehicle.firstPassenger && vehicle.hidePassenger(player)) return

        val data = GunData.from(stack)

        val times = 2 * getDelta().coerceAtMost(0.8f)

        val pose: Float = if (player.isCrouching && player.bbHeight >= 1 && !isProne(player)) {
            0.85f
        } else if (isProne(player)) {
            if (data.attachment.hasBipod() || item.hasBipod(data)) 0f else 0.25f
        } else {
            1f
        }

        // 气息摇摆的幅度按**主武器**的重量（`ActiveGun.handlingData`）：手里那把枪是主武器
        val customWeight = (ActiveGun.handlingData(player) ?: data).get(GunProp.WEIGHT).toFloat().coerceIn(1f, 30f)

        if (!breath && zoom) {
            val newPitch = (
                    player.xRot - 0.01f * sin(0.03 * player.tickCount) * pose * Mth.nextDouble(
                        RandomSource.create(),
                        0.1,
                        1.0
                    ) * times * (1 - 0.033 * customWeight)
                    ).toFloat()
            player.xRot = newPitch
            player.xRotO = player.xRot

            val newYaw = (
                    player.yRot - 0.005f * cos(0.025 * (player.tickCount + 2 * Math.PI)) * pose * Mth.nextDouble(
                        RandomSource.create(),
                        0.05,
                        1.25
                    ) * times * (1 - 0.033 * customWeight)
                    ).toFloat()
            player.yRot = newYaw
            player.yRotO = player.yRot
        }
    }

    private fun getDelta(): Float {
        return mc.deltaFrameTime
    }

    @SubscribeEvent
    fun computeCameraAngles(event: ViewportEvent.ComputeCameraAngles) {
        if (clientLevel == null) return
        val entity = event.camera.entity as? LivingEntity ?: return
        val player = localPlayer ?: return
        val stack = entity.mainHandItem

        if (stack.`is`(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using")
            && stack.getOrCreateTag().getBoolean("Linked")
        ) {
            handleDroneCamera(event, entity)
        }

        val yaw = event.yaw
        val pitch = event.pitch
        val roll = event.roll

        shakeTime = Mth.lerp(0.05 * getDelta(), shakeTime, 0.0)

        val vehicle = player.vehicle
        if (shakeTime > 0) {
            val shakeRadiusAmplitude =
                (1 - player.position().distanceTo(Vec3(shakePos[0], shakePos[1], shakePos[2])) / shakeRadius)
                    .toFloat().coerceIn(0f, 1f)

            val onVehicle = vehicle != null
            if (shakeType > 0) {
                event.yaw =
                    (yaw + (shakeTime * sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * shakeType *
                            if (onVehicle) 0.1 else 1.0)).toFloat()
                event.pitch =
                    (pitch - (shakeTime * sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * shakeType *
                            if (onVehicle) 0.1 else 1.0)).toFloat()
                cameraRoll =
                    (roll - (shakeTime * sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude *
                            if (onVehicle) 0.1 else 1.0)).toFloat()
            } else {
                event.yaw =
                    (yaw - (shakeTime * sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * shakeType *
                            if (onVehicle) 0.1 else 1.0)).toFloat()
                event.pitch =
                    (pitch + (shakeTime * sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * shakeType *
                            if (onVehicle) 0.1 else 1.0)).toFloat()
                cameraRoll =
                    (roll + (shakeTime * sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude *
                            if (onVehicle) 0.1 else 1.0)).toFloat()
            }
        }

        cameraPitch = event.pitch
        cameraYaw = event.yaw
        cameraRoll *= 0.99f

        if (vehicle is VehicleEntity && vehicle.banHand(player)) return

        // 手持副武器时按普通物品处理
        if (GunItem.isHeldWeapon(stack)) {
            handleWeaponSway(entity)
            handleWeaponMove(entity)
            handleWeaponZoom(entity)
            handleWeaponBipodView(entity)
            handleWeaponFire(event, entity)
            handleWeaponShell()
            handleGunRecoil()
            handleBowPullAnimation(entity, stack)
            handleWeaponDraw(entity)
            handlePlayerCamera(event)
        }

        handleShockCamera(event, entity)
    }

    private fun handleDroneCamera(event: ViewportEvent.ComputeCameraAngles, entity: LivingEntity) {
        val stack = entity.mainHandItem
        val drone = EntityFindUtil.findDrone(entity.level(), stack.getOrCreateTag().getString("LinkedDrone")) ?: return
        cameraRoll =
            drone.getRoll(event.partialTick.toFloat() * (1 - (drone.getPitch(event.partialTick.toFloat()) / 90)))
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderHand(event: RenderHandEvent) {
        // Pop only the stack that actually received the push in bobHurt.
        if (vehiclePoseStack === event.poseStack) {
            event.poseStack.popPose()
            vehiclePoseStack = null
        }

        val player = localPlayer ?: return

        val leftHand = if (mc.options.mainHand().get() == HumanoidArm.RIGHT)
            InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND

        val rightHand = if (mc.options.mainHand().get() == HumanoidArm.RIGHT)
            InteractionHand.MAIN_HAND else InteractionHand.OFF_HAND

        val rightHandItem = player.getItemInHand(rightHand)

        if (event.hand == leftHand) {
            // 手持副武器时按普通物品处理
            if (GunItem.isHeldWeapon(rightHandItem)) {
                event.isCanceled = true
            }
            if (rightHandItem.`is`(ModItems.LUNGE_MINE.get())) {
                event.isCanceled = true
            }
            if (player.isUsingItem && player.useItem.`is`(ModItems.ARTILLERY_INDICATOR.get())) {
                event.isCanceled = true
            }
        }

        if (event.hand == rightHand) {
            // 手持副武器时按普通物品处理
            if (GunItem.isHeldWeapon(rightHandItem) && drawTime > 0.15) {
                event.isCanceled = true
            }
            if (player.isUsingItem && player.useItem.`is`(ModItems.ARTILLERY_INDICATOR.get())) {
                event.isCanceled = true
            }
        }

        val stack = player.mainHandItem
        if (stack.`is`(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using")
            && stack.getOrCreateTag().getBoolean("Linked")
        ) {
            if (EntityFindUtil.findDrone(player.level(), stack.getOrCreateTag().getString("LinkedDrone")) != null) {
                event.isCanceled = true
            }
        }

        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && (vehicle.banHand(player) ||
                    (!zoom && mc.options.cameraType == CameraType.FIRST_PERSON && ModKeyMappings.FREE_CAMERA.isDown()))
        ) {
            event.isCanceled = true
        }
    }

    private fun handleWeaponSway(entity: LivingEntity) {
        val player = entity as? Player ?: return
        val stack = ActiveGun.stackOf(player)
        val item = stack.item as? GunItem ?: return
        if (!GunItem.isOperable(stack)) return
        val data = GunData.from(stack)

        val times = 2 * getDelta().coerceAtMost(0.8f)
        val pose =
            if (player.isShiftKeyDown && player.bbHeight >= 1 && isProne(player)) {
                0.85
            } else if (isProne(player)) {
                if (data.attachment.hasBipod() || item.hasBipod(data)) 0.0 else 0.25
            } else {
                1.0
            }

        swayTime += 0.05 * times
        swayX = pose * -0.008 * sin(swayTime) * (1 - 0.95 * zoomTime)
        swayY = pose * 0.125 * sin(swayTime - 1.585) * (1 - 0.95 * zoomTime) - 3 * moveRotZ
    }

    private fun handleWeaponMove(entity: LivingEntity) {
        val player = entity as? Player ?: return
        val stack = ActiveGun.stackOf(player)
        if (!GunItem.isOperable(stack)) return
        val data = GunData.from(stack)
        val resource = GunResource.compute(stack)

        val times = 3.7f * getDelta().coerceAtMost(0.8f)
        val moveSpeed = entity.deltaMovement.horizontalDistance()
        val animSpeed =
            if (entity.onGround()) {
                if (entity.isSprinting) {
                    1.8
                } else {
                    2.0
                }
            } else {
                0.005
            }

        // 冲刺/行走摇摆的阻尼按**主武器**的重量（`ActiveGun.handlingData`）
        val customWeight = (ActiveGun.handlingData(player) ?: data).get(GunProp.WEIGHT).coerceIn(1.0, 50.0)

        if (!isEditing) {
            moveRotZ =
                if (!entity.isSprinting && mc.options.keyUp.isDown && firePosTimer == 0.0 && resource.movingTilt && !isProne(
                        player
                    )
                ) {
                    Mth.lerp(0.2 * times, moveRotZ, 0.14) * (1 - zoomTime)
                } else {
                    Mth.lerp(0.2 * times, moveRotZ, 0.0) * (1 - zoomTime)
                }

            if (entity.isSprinting && !data.reloading() && (firePosTimer == 0.0 || firePosTimer > 1.0) && !ModKeyMappings.FIRE.isDown && zoomTime < 0.99 && !isGunMeleeActive(
                    stack
                )
            ) {
                sprintBasicRotX = Mth.lerp(0.3f * times / (customWeight + 4), sprintBasicRotX, 1.0).coerceIn(0.0, 1.0)
                sprintBasicRotY = Mth.lerp(0.18f * times / (customWeight + 4), sprintBasicRotY, 1.0).coerceIn(0.0, 1.0)
                sprintBasicRotZ = Mth.lerp(0.3f * times / (customWeight + 4), sprintBasicRotZ, 1.0).coerceIn(0.0, 1.0)

                sprintBasicPosX = Mth.lerp(0.8f * times / (customWeight + 4), sprintBasicPosX, 1.0).coerceIn(0.0, 1.0)
                sprintBasicPosY = Mth.lerp(0.25f * times / (customWeight + 4), sprintBasicPosY, 1.0).coerceIn(0.0, 1.0)
                sprintBasicPosZ = Mth.lerp(0.8f * times / (customWeight + 4), sprintBasicPosZ, 1.0).coerceIn(0.0, 1.0)
            } else {
                sprintBasicRotX = Mth.lerp(1.4f * times / customWeight, sprintBasicRotX, 0.0).coerceIn(0.0, 1.0)
                sprintBasicRotY = Mth.lerp(0.96f * times / customWeight, sprintBasicRotY, 0.0).coerceIn(0.0, 1.0)
                sprintBasicRotZ = Mth.lerp(1.4f * times / customWeight, sprintBasicRotZ, 0.0).coerceIn(0.0, 1.0)

                sprintBasicPosX = Mth.lerp(0.8f * times / customWeight, sprintBasicPosX, 0.0).coerceIn(0.0, 1.0)
                sprintBasicPosY = Mth.lerp(0.8f * times / customWeight, sprintBasicPosY, 0.0).coerceIn(0.0, 1.0)
                sprintBasicPosZ = Mth.lerp(0.8f * times / customWeight, sprintBasicPosZ, 0.0).coerceIn(0.0, 1.0)
            }
        }

        if (isMoving()) {
            moveTime += 0.15 * animSpeed * times * moveSpeed * if (firePosTimer != 0.0) 0.4 else 1.0
            sprintTime += 0.15 * animSpeed * times * moveSpeed * (if (player.isSprinting) sprintBasicPosX else 1.0) * (if (firePosTimer != 0.0) 0.4 else 1.0)
            moveFadeTime = Mth.lerp(0.13 * times, moveFadeTime, 1.0)
        } else {
            moveFadeTime = Mth.lerp(0.1 * times, moveFadeTime, 0.0)
        }

        if (entity.isSprinting && !data.reloading() && (firePosTimer == 0.0 || firePosTimer > 1.0) && !ModKeyMappings.FIRE.isDown && zoomTime < 0.99 && !isGunMeleeActive(
                stack
            )
        ) {
            sprintFadeTime = if (entity.onGround()) {
                Mth.lerp(0.08 * times, sprintFadeTime, 1.0)
            } else {
                Mth.lerp(0.15 * times, sprintFadeTime, 0.0)
            }

            sprintPosX = 2 * sin(1 * PI * sprintTime) * sprintFadeTime
            sprintPosY = 1 * sin(2 * PI * sprintTime) * sprintFadeTime
        } else {
            sprintPosX = Mth.lerp(0.1 * times, sprintPosX, 0.0)
            sprintPosY = Mth.lerp(0.1 * times, sprintPosY, 0.0)
            sprintFadeTime = Mth.lerp(0.1 * times, sprintFadeTime, 0.0)
        }

        movePosX = 0.2 * sin(1 * PI * moveTime) * (1 - 0.4 * zoomTime) * moveFadeTime
        movePosY = -0.135 * sin(2 * PI * (moveTime - 0.25)) * (1 - 0.4 * zoomTime) * moveFadeTime

        val left = mc.options.keyLeft.isDown
        val right = mc.options.keyRight.isDown
        var pos = 0.0
        if (left) {
            pos = -0.04
        }
        if (right) {
            pos = 0.04
        }
        if (left && right) {
            pos = 0.0
        }

        movePosHorizon = Mth.lerp(0.1 * times, movePosHorizon, pos * (1 - 1 * zoomTime))

        val velocity = entity.deltaMovement.y + 0.078
        velocityY = (Mth.lerp(0.23 * times, velocityY, velocity) * (1 - 0.5 * zoomTime)).coerceIn(-0.8, 0.8)
    }

    @JvmStatic
    fun gunRootMove(
        animationProcessor: AnimationProcessor<*>,
        customX: Float,
        customY: Float,
        customZ: Float,
        useCustomAnim: Boolean
    ) {
        val root = animationProcessor.getBone("root")
        val walkPosX = movePosX.toFloat()
        val walkPosY = (swayY + movePosY).toFloat()
        val walkPosZ = 0f
        val walkRotX = swayX.toFloat()
        val walkRotY = (0.2f * movePosX).toFloat()
        val walkRotZ = (0.2f * movePosX).toFloat()

        val i = if (useCustomAnim) 0 else 1

        val basicSprintPosX = (sprintBasicPosX * (1.5 + customX)).toFloat() * i
        val basicSprintPosY =
            (sprintBasicPosY * (-2.35 + customY - 8 * AnimationCurves.PARABOLA.apply(sprintBasicPosY))).toFloat() * i
        val basicSprintPosZ = (sprintBasicPosZ * (-0.55 + customZ)).toFloat() * i

        val basicSprintRotX = (sprintBasicRotX * 39 * Mth.DEG_TO_RAD).toFloat() * i
        val basicSprintRotY = (sprintBasicRotY * 35.6 * Mth.DEG_TO_RAD).toFloat() * i
        val basicSprintRotZ = (sprintBasicRotZ * 34.7 * Mth.DEG_TO_RAD).toFloat() * i

        val gunPosX =
            (walkPosX + basicSprintPosX + sprintPosX * i + 20 * drawTime + 9.3f * movePosHorizon).toFloat() * (1 - 0.5 * zoomTime).toFloat()
        val gunPosY =
            (walkPosY + basicSprintPosY + sprintPosY * i - 40 * drawTime - 2f * velocityY).toFloat() * (1 - 0.5 * zoomTime).toFloat()
        val gunPosZ = (walkPosZ + basicSprintPosZ) * (1 - 1 * zoomTime).toFloat()
        val gunRotX =
            ((walkRotX + basicSprintRotX - Mth.DEG_TO_RAD * 60 * drawTime - 0.15f * velocityY) * (1 - 0.5 * zoomTime) + Mth.DEG_TO_RAD * turnRot[0]).toFloat()
        val gunRotY =
            ((walkRotY + basicSprintRotY + (0.2f * sprintBasicPosX * i) + Mth.DEG_TO_RAD * 300 * drawTime) * (1 - 0.75 * zoomTime) + Mth.DEG_TO_RAD * turnRot[1]).toFloat()
        val gunRotZ =
            ((walkRotZ + basicSprintRotZ + moveRotZ + Mth.DEG_TO_RAD * 90 * drawTime + 2.7f * movePosHorizon) * (1 - 0.5 * zoomTime) + Mth.DEG_TO_RAD * turnRot[2]).toFloat()

        root.posX = gunPosX
        root.posY = gunPosY
        root.posZ = gunPosZ
        root.rotX = gunRotX
        root.rotY = gunRotY
        root.rotZ = gunRotZ
    }

    @JvmStatic
    fun gunRootMoveV2(
        poseStack: PoseStack,
        customX: Float,
        customY: Float,
        customZ: Float,
        useCustomAnim: Boolean
    ) {
        val walkPosX = movePosX.toFloat()
        val walkPosY = (swayY + movePosY).toFloat()
        val walkPosZ = 0f
        val walkRotX = swayX.toFloat()
        val walkRotY = (0.2f * movePosX).toFloat()
        val walkRotZ = (0.2f * movePosX).toFloat()

        val i = if (useCustomAnim) 0 else 1

        val basicSprintPosX = (sprintBasicPosX * (1.5 + customX)).toFloat() * i
        val basicSprintPosY =
            (sprintBasicPosY * (-2.35 + customY - 8 * AnimationCurves.PARABOLA.apply(sprintBasicPosY))).toFloat() * i
        val basicSprintPosZ = (sprintBasicPosZ * (-0.55 + customZ)).toFloat() * i

        val basicSprintRotX = (sprintBasicRotX * 39 * Mth.DEG_TO_RAD).toFloat() * i
        val basicSprintRotY = (sprintBasicRotY * 35.6 * Mth.DEG_TO_RAD).toFloat() * i
        val basicSprintRotZ = (sprintBasicRotZ * 14.7 * Mth.DEG_TO_RAD).toFloat() * i

        val gunPosX =
            (walkPosX + basicSprintPosX + sprintPosX * i + 20 * drawTime + 9.3f * movePosHorizon - 0.5 * turnRot[1]).toFloat() * (1 - 0.5 * zoomTime).toFloat()
        val gunPosY =
            (walkPosY + basicSprintPosY + sprintPosY * i - 40 * drawTime - 2f * velocityY).toFloat() * (1 - 0.5 * zoomTime).toFloat()
        val gunPosZ = (walkPosZ + basicSprintPosZ) * (1 - 1 * zoomTime).toFloat()
        val gunRotX =
            ((walkRotX + basicSprintRotX - Mth.DEG_TO_RAD * 60 * drawTime - 0.15f * velocityY) * (1 - 0.5 * zoomTime) + Mth.DEG_TO_RAD * turnRot[0]).toFloat()
        val gunRotY =
            ((walkRotY + basicSprintRotY + (0.2f * sprintBasicPosX * i) + Mth.DEG_TO_RAD * 300 * drawTime) * (1 - 0.75 * zoomTime) + Mth.DEG_TO_RAD * turnRot[1]).toFloat()
        val gunRotZ =
            ((walkRotZ + basicSprintRotZ + moveRotZ + Mth.DEG_TO_RAD * 90 * drawTime + 2.7f * movePosHorizon) * (1 - 0.5 * zoomTime) + Mth.DEG_TO_RAD * turnRot[2]).toFloat()

        poseStack.translate(-gunPosX / 16, gunPosY / 16, gunPosZ / 16)

        poseStack.mulPose(Axis.XP.rotation(gunRotX))
        poseStack.mulPose(Axis.YP.rotation(gunRotY))
        poseStack.mulPose(Axis.ZP.rotation(gunRotZ))
    }

    private fun handleWeaponZoom(entity: LivingEntity) {
        val player = entity as? Player ?: return
        val stack = ActiveGun.stackOf(player)
        val data = GunData.from(stack)
        val times = getDelta()
        // ⚠ `ZoomTime` / `Weight` 恒读**主武器**（`ActiveGun.handlingData`）：
        // 副武器是挂在主武器身上的附件，端在手里的始终是主武器，挂上一支 GP-25
        // （`Weight 1.5` / `ZoomTime 1`）不该让 AK 的瞄准快到看不见对焦。
        // 下面 `data` 只留给"副武器正在换弹"这条门槛 —— 那是**动作状态**，跟着操控的枪走。
        val handling = ActiveGun.handlingData(player) ?: data
        val weight = (handling.stack.item as? GunItem)?.getCustomWeight(handling) ?: 0.0
        val duration = handling.get(GunProp.ZOOM_TIME).coerceAtLeast(1) + 0.4 * weight
        val stepIn = times / duration
        val stepOut = times / (duration * 0.75f)
        val vehicle = player.vehicle

        if (zoom
            && !(vehicle is VehicleEntity && vehicle.banHand(player))
            && !notInGame
            && drawTime < 0.01
            && !isEditing
            && !(data.reloading() && !data.get(GunProp.ZOOM_RELOAD))
        ) {
            if (fireCooldown <= 10) {
                zoomTime = (zoomTime + stepIn).coerceIn(0.0, 1.0)
            }
        } else {
            zoomTime = (zoomTime - stepOut).coerceIn(0.0, 1.0)
        }

        if (zoomPos > 0.8) {
            noSprintTicks = 5f
        }

        zoomPos = AnimationCurves.EASE_IN_OUT_QUINT.apply(zoomTime)
        zoomPosZ = AnimationCurves.PARABOLA.apply(zoomTime)
    }

    /**
     * 更新脚架视图过渡进度：卧姿且枪械自带脚架或配件带脚架时移向 `bipod_view`，否则回到 `idle_view`。
     * 仅影响非瞄准视角，瞄准时由 [zoomTime] 混合的瞄准定位点接管。
     *
     * 改装界面打开时同样退回 `idle_view`：改装聚焦以该视角为基准，若不退回，
     * 鼠标移动触发聚焦的瞬间模型会闪现。
     */
    private fun handleWeaponBipodView(entity: LivingEntity) {
        val player = entity as? Player ?: return
        val stack = ActiveGun.stackOf(player)
        val item = stack.item as? GunItem ?: return
        if (!GunItem.isOperable(stack)) return
        val data = GunData.from(stack)

        val deployed = !isEditing && isProne(player) && (data.attachment.hasBipod() || item.hasBipod(data))
        val step = getDelta() / (BIPOD_VIEW_DURATION_SECONDS * TICKS_PER_SECOND)

        bipodViewTime = if (deployed) {
            (bipodViewTime + step).coerceAtMost(1.0)
        } else {
            (bipodViewTime - step).coerceAtLeast(0.0)
        }
    }

    private fun handleWeaponFire(event: ViewportEvent.ComputeCameraAngles, entity: LivingEntity) {
        val times = (1.25f * customAnimSpeed * mc.deltaFrameTime.coerceAtMost(0.48f)).toFloat()
        val stack = ActiveGun.stackOf(entity)
        val data = GunData.from(stack)
        val amplitude = 25000.0 * data.get(GunProp.RECOIL_Y) * data.get(GunProp.RECOIL_X)

        if (fireRecoilTime > 0.0) {
            firePosTimer = 0.001
            fireRotTimer = if (fireRotTimer > 0) {
                0.12
            } else {
                0.001
            }
            fireRecoilTime -= 7 * times
            fireSpread += 0.1 * times
            firePosZ += (0.8 * firePosZ + 0.4) * (4 * Math.random() + 0.85) * times
            recoilForce += 0.5
        }

        fireSpread = (fireSpread - 0.1 * (fireSpread.pow(2) * times)).coerceIn(0.0, 2.0)
        firePosZ = (firePosZ - 0.7 * (firePosZ.pow(2) * times)).coerceIn(0.0, 2.5)
        firePosZ *= 0.99

        if (0.0 < firePosTimer) {
            firePosTimer += 0.16 * times
        }
        if (0.0 < fireRotTimer) {
            fireRotTimer += 0.24 * times
        }

        if (firePosTimer >= 2.0) {
            firePosTimer = 0.0
        }
        if (fireRotTimer >= 3.0) {
            fireRotTimer = 0.0
        }

        // 副武器枪口焰：只推进计时（阈值与 fireRotTimer 一致），**不带任何枪身后坐**
        if (0.0 < subWeaponFireRotTimer) {
            subWeaponFireRotTimer += 0.24 * times
            if (subWeaponFireRotTimer >= 3.0) {
                subWeaponFireRotTimer = 0.0
            }
        }

        // 副武器的**镜头**后坐相位：与 `firePosTimer` 同一形状（+0.16/tick、2.0 归零），
        // 但它只喂下面那个 `shake` 与 `handleGunRecoil` 的抬枪 —— 枪身位形与拉栓仍属
        // `fire_sub_weapon` 动画（见 `subWeaponRecoilTimer` 的 KDoc）
        if (0.0 < subWeaponRecoilTimer) {
            subWeaponRecoilTimer += 0.16 * times
            if (subWeaponRecoilTimer >= 2.0) {
                subWeaponRecoilTimer = 0.0
            }
        }

        boltMove = if (firePosTimer > 0 && firePosTimer <= 0.5) {
            1.2 * Mth.sin(2 * Mth.PI * firePosTimer.toFloat()).toDouble()
        } else {
            0.0
        }

        if (boltMove > 1) {
            boltMove = 1.0
        }

        if (entity is Player && entity.isSpectator) return

        // 镜头后坐的两个相位**二选一**（同一时刻最多一个 > 0，开火时见 handleClientShoot）：
        // 主武器用 `firePosTimer`，副武器用 `subWeaponRecoilTimer`。
        // ⚠ 副武器那一发以前读的是 `firePosTimer` —— 它恒为 0，而 `decayingOscillation` 在
        // `elapsedTime == 0` 处**恰好**是 `sin(0) == 0`，于是整项，连同里面的 `amplitude`
        // （`25000 · RECOIL_X · RECOIL_Y`），永远是 0：`RecoilX` 一个字节都读不到。
        val recoilPhase = if (firePosTimer > 0.0) firePosTimer else subWeaponRecoilTimer

        var shake = (
                MathTool.decayingOscillation(
                    0.6f,
                    2f,
                    2f,
                    recoilPhase.toFloat()
                ) * (1 + amplitude) * (DisplayConfig.WEAPON_SCREEN_SHAKE.get() / 100.0).toFloat()
                )

        if (recoilY > 0) {
            shake = -shake
        }

        lerpShake = Mth.lerp(event.partialTick * 0.5, lerpShake, shake)

        cameraRot[2] = lerpShake
    }

    @JvmStatic
    fun handleShootAnimation(
        bone: GeoBone,
        x: Float,
        y: Float,
        z: Float,
        rotX: Float,
        rotY: Float,
        rotZ: Float,
        zoomMultiply: Float,
        customSpeed: Float
    ) {
    }

    @JvmStatic
    fun handleShootAnimationV2(
        poseStack: PoseStack,
        x: Float,
        y: Float,
        z: Float,
        rotX: Float,
        rotY: Float,
        rotZ: Float,
        zoomMultiply: Float,
        customSpeed: Float
    ) {
        val player = localPlayer ?: return
        val stack = ActiveGun.stackOf(player)
        val item = stack.item as? GunItem ?: return
        if (!GunItem.isOperable(stack)) return

        customAnimSpeed = customSpeed.toDouble()

        val data = GunData.from(stack)

        val pose =
            if (player.isShiftKeyDown && player.bbHeight >= 1 && !isProne(player)) {
                0.85f
            } else if (isProne(player)) {
                if (data.attachment.hasBipod() || item.hasBipod(data)) {
                    0.5f
                } else {
                    0.75f
                }
            } else {
                1f
            }

        var zoomMultiply = zoomMultiply
        zoomMultiply = zoomMultiply.coerceIn(0f, 1f)

        val zoom = (1 - (1 - zoomMultiply) * zoomTime).toFloat() * pose

        val gunPosX = zoom * x * (recoilHorizon * (0.5f * firePosZ)).toFloat()
        val gunPosY =
            zoom * y * ((getBoneMoveY(firePosTimer.toFloat()) * 0.1 + 0.07f * firePosZ) * (1 - 0.25 * zoomTime)).toFloat()
        val gunPosZ =
            zoom * z * (getBoneMoveZ(firePosTimer.toFloat()) * 0.03 + 1.1f * firePosZ).toFloat() * (1 - 0.75 * zoomTime).toFloat()

        val gunRotX =
            zoom * rotX * (-getBoneRotX(fireRotTimer.toFloat()) * Mth.DEG_TO_RAD * 0.5f + 0.01f * firePosZ).toFloat() * (1 - 0.85 * zoomTime).toFloat()
        val gunRotY =
            (3 * zoom * rotY * getBoneRotY(fireRotTimer.toFloat()) * Mth.DEG_TO_RAD * recoilHorizon * (1 - 0.3 * zoomTime)).toFloat()
        val gunRotZ =
            (2 * zoom * rotZ * getBoneRotZ(fireRotTimer.toFloat()) * Mth.DEG_TO_RAD * recoilHorizon * (1 - 0.5 * zoomTime)).toFloat()

        poseStack.mulPose(Axis.XP.rotation(gunRotX))
        poseStack.mulPose(Axis.YP.rotation(gunRotY))
        poseStack.mulPose(Axis.ZP.rotation(gunRotZ))

        poseStack.translate(-gunPosX / 16, gunPosY / 16, gunPosZ / 16)


    }

    @JvmStatic
    fun getBoneRotX(t: Float): Float {
        return when {
            t <= 0.25f -> Mth.lerp(t / (0.25F - 0F), 0F, -5.82024F)
            t <= 0.5f -> Mth.lerp((t - 0.25F) / (0.5F - 0.25F), -5.82024F, -6.38564F)
            t <= 0.75f -> Mth.lerp((t - 0.5F) / (0.75F - 0.5F), -6.38564F, -6.0138F)
            t <= 1f -> Mth.lerp((t - 0.75F) / (1F - 0.75F), -6.0138F, -3.22698F)
            t <= 1.3333f -> Mth.lerp((t - 1F) / (1.3333F - 1F), -3.22698F, -0.42425F)
            t <= 1.75f -> Mth.lerp((t - 1.3333F) / (1.75F - 1.3333F), -0.42425F, 0.23068F)
            t <= 2.0833f -> Mth.lerp((t - 1.75F) / (2.0833F - 1.75F), 0.23068F, -0.09988F)
            t <= 2.4167f -> Mth.lerp((t - 2.0833F) / (2.4167F - 2.0833F), -0.09988F, 0.04509F)
            else -> Mth.lerp((t - 2.4167F) / (3F - 2.4167F), 0.04509F, 0F)
        }
    }

    @JvmStatic
    fun getBoneRotY(t: Float): Float {
        return when {
            t <= 0.25f -> Mth.lerp(t / (0.25F - 0F), 0F, 1.33042F)
            t <= 0.5f -> Mth.lerp((t - 0.25F) / (0.5F - 0.25F), 1.33042F, -0.61289F)
            t <= 0.75f -> Mth.lerp((t - 0.5F) / (0.75F - 0.5F), -0.61289F, -0.64862F)
            t <= 1f -> Mth.lerp((t - 0.75F) / (1F - 0.75F), -0.64862F, -0.95049F)
            t <= 1.3333f -> Mth.lerp((t - 1F) / (1.3333F - 1F), -0.95049F, 0.27786F)
            t <= 1.75f -> Mth.lerp((t - 1.3333F) / (1.75F - 1.3333F), 0.27786F, -0.21405F)
            t <= 2.0833f -> Mth.lerp((t - 1.75F) / (2.0833F - 1.75F), -0.21405F, 0.076F)
            t <= 2.4167f -> Mth.lerp((t - 2.0833F) / (2.4167F - 2.0833F), 0.076F, 0.01634F)
            else -> Mth.lerp((t - 2.4167F) / (3F - 2.4167F), 0.01634F, 0F)
        }
    }

    @JvmStatic
    fun getBoneRotZ(t: Float): Float {
        return when {
            t <= 0.25f -> Mth.lerp(t / (0.25F - 0F), 0F, 5.79388F)
            t <= 0.5f -> Mth.lerp((t - 0.25F) / (0.5F - 0.25F), 5.79388F, -1.91761F)
            t <= 0.75f -> Mth.lerp((t - 0.5F) / (0.75F - 0.5F), -1.91761F, -3.1926F)
            t <= 1f -> Mth.lerp((t - 0.75F) / (1F - 0.75F), -3.1926F, 1.89646F)
            t <= 1.3333f -> Mth.lerp((t - 1F) / (1.3333F - 1F), 1.89646F, 0.43549F)
            t <= 1.75f -> Mth.lerp((t - 1.3333F) / (1.75F - 1.3333F), 0.43549F, -0.46178F)
            t <= 2.0833f -> Mth.lerp((t - 1.75F) / (2.0833F - 1.75F), -0.46178F, 0.12379F)
            t <= 2.4167f -> Mth.lerp((t - 2.0833F) / (2.4167F - 2.0833F), 0.12379F, -0.04605F)
            else -> Mth.lerp((t - 2.4167F) / (3F - 2.4167F), -0.04605F, 0F)
        }
    }

    @JvmStatic
    fun getBoneMoveY(t: Float): Float {
        return when {
            t <= 0.1667f -> Mth.lerp(t / (0.1667F - 0F), 0F, 0.25313F)
            t <= 0.3333f -> Mth.lerp((t - 0.1667F) / (0.3333F - 0.1667F), 0.25313F, 0.69563F)
            t <= 0.5f -> Mth.lerp((t - 0.3333F) / (0.5F - 0.3333F), 0.69563F, 0.54937F)
            t <= 0.6667f -> Mth.lerp((t - 0.5F) / (0.6667F - 0.5F), 0.54937F, 0.05688F)
            t <= 0.8333f -> Mth.lerp((t - 0.6667F) / (0.8333F - 0.6667F), 0.05688F, -0.17F)
            t <= 1f -> Mth.lerp((t - 0.8333F) / (1F - 0.8333F), -0.17F, -0.28F)
            t <= 1.1667f -> Mth.lerp((t - 1F) / (1.1667F - 1F), -0.28F, -0.065F)
            t <= 1.3333f -> Mth.lerp((t - 1.1667F) / (1.3333F - 1.1667F), -0.065F, 0.05F)
            t <= 1.5833f -> Mth.lerp((t - 1.3333F) / (1.5833F - 1.3333F), 0.05F, 0.03F)
            else -> Mth.lerp((t - 1.5833F) / (2F - 1.5833F), 0.03F, 0F)
        }
    }

    @JvmStatic
    fun getBoneMoveZ(t: Float): Float {
        return when {
            t <= 0.1667f -> Mth.lerp(t / (0.1667F - 0F), 0F, 5.205F)
            t <= 0.3333f -> Mth.lerp((t - 0.1667F) / (0.3333F - 0.1667F), 5.205F, 2.775F)
            t <= 0.4167f -> Mth.lerp((t - 0.3333F) / (0.4167F - 0.3333F), 2.775F, 0.66F)
            t <= 0.5833f -> Mth.lerp((t - 0.4167F) / (0.5833F - 0.4167F), 0.66F, -0.005F)
            t <= 0.75f -> Mth.lerp((t - 0.5833F) / (0.75F - 0.5833F), -0.005F, -0.485F)
            t <= 0.9167f -> Mth.lerp((t - 0.75F) / (0.9167F - 0.75F), -0.485F, -0.095F)
            t <= 1.1667f -> Mth.lerp((t - 0.9167F) / (1.1667F - 0.9167F), -0.095F, 0.06F)
            t <= 1.3333f -> Mth.lerp((t - 1.1667F) / (1.3333F - 1.1667F), 0.06F, 0.1F)
            t <= 1.5833f -> Mth.lerp((t - 1.3333F) / (1.5833F - 1.3333F), 0.1F, -0.03F)
            else -> Mth.lerp((t - 1.5833F) / (2F - 1.5833F), -0.03F, 0F)
        }
    }

    private fun handleWeaponShell() {
        if (localPlayer == null) return

        val times = getDelta().coerceAtMost(0.8f)

        if (shellIndex >= 5) {
            shellIndex = 0
            shellIndexTime[0] = 0.001
        }

        for (i in 0..<5) {
            if (shellIndexTime[i] > 0) {
                shellIndexTime[i] = (shellIndexTime[i] + 8 * times).coerceAtMost(50.0)
            }
            if (shellIndexTime[i] == 50.0) {
                shellIndexTime[i] = 0.0
            }
        }
    }

    private fun handleGunRecoil() {
        val player = localPlayer ?: return
        val stack = ActiveGun.stackOf(player)
        val item = stack.item as? GunItem ?: return
        if (!GunItem.isOperable(stack)) return
        val data = GunData.from(stack)

        val times = getDelta().coerceAtMost(1.6f)

        // 后坐的**重量阻尼**按主武器（`ActiveGun.handlingData`）：后坐是打进来之后被"手里的质量"吃掉的，
        // 而手里是整把主武器加挂在它身上的副武器。至于后坐**幅度**（`RECOIL_X`）仍然是操控的枪的 —— 那一发是谁打的。
        val customWeight = (ActiveGun.handlingData(player) ?: data).get(GunProp.WEIGHT)
        val gunRecoilX = data.get(GunProp.RECOIL_X)

        recoilHorizon = Mth.lerp(0.2 * times, recoilHorizon, 0.0) + recoilY
        recoilY = 0.0

        // 计算后坐力
        val pose =
            if (player.isShiftKeyDown && player.bbHeight >= 1 && !isProne(player)) {
                0.7f
            } else if (isProne(player)) {
                if (data.attachment.hasBipod() || item.hasBipod(data)) {
                    0.1f
                } else {
                    0.5f
                }
            } else {
                1f
            }

        // 水平后坐
        val newYaw =
            player.yRot - (0.6 * recoilHorizon * pose * times * (0.5 + fireSpread) * 3.6 * (4 / (customWeight + 4))).toFloat()
        player.yRot = newYaw
        player.yRotO = player.yRot

        // 抬枪（俯仰）的相位与 `handleWeaponFire` 里的 `shake` 同源、同一条"二选一"规则：
        // 主武器是 `firePosTimer`，副武器是 `subWeaponRecoilTimer`。
        // ⚠ 副武器那一发曾经**恒不满足** `firePosTimer > 0`（它由 `fireRecoilTime` 打开，而副武器
        // 那一发 `fireRecoilTime == 0.0`），于是 `RECOIL_X` 整项都是死代码 —— 只剩下面那条水平偏
        // 还在消费 `RECOIL_Y`。（第二项里的 `recoilForce` 对副武器仍是 0：它的 `+= 0.5` 同样挂在
        // `fireRecoilTime` 上；副武器开火的 `RecoilX` 在 0.002 这一档，这一项本就只有 ~0.004°。）
        val recoilPhase = if (firePosTimer > 0.0) firePosTimer else subWeaponRecoilTimer

        if (recoilPhase > 0.0) {
            var rotateX =
                (70 * pose * gunRecoilX * sin(recoilPhase * PI * 2) * (2.2 - recoilPhase) * 4.8 * (4 / (customWeight + 4)) + recoilForce * recoilForce * gunRecoilX * pose * 4.8 * (4 / (customWeight + 4))).toFloat() * times

            if (rotateX < 0) {
                rotateX *= 1.8f
            }

            player.xRot -= rotateX
            player.xRotO = player.xRot
        }
    }

    private fun handleShockCamera(event: ViewportEvent.ComputeCameraAngles, entity: LivingEntity) {
        val player = entity as? Player ?: return
        if (player.isSpectator) return

        if (entity.hasEffect(ModMobEffects.SHOCK) && mc.options.cameraType == CameraType.FIRST_PERSON) {
            val shakeStrength = DisplayConfig.SHOCK_SCREEN_SHAKE.get().toFloat() / 100.0f
            if (shakeStrength <= 0.0f) return
            event.yaw = mc.gameRenderer.mainCamera.yRot +
                    Mth.nextDouble(RandomSource.create(), -3.0, 3.0).toFloat() * shakeStrength
            event.pitch = mc.gameRenderer.mainCamera.xRot +
                    Mth.nextDouble(RandomSource.create(), -3.0, 3.0).toFloat() * shakeStrength
        }
    }

    @JvmStatic
    fun handleReloadShake(boneRotX: Double, boneRotY: Double, boneRotZ: Double) {
        val player = localPlayer ?: return
        if (player.isSpectator) return

        val shakeStrength = DisplayConfig.WEAPON_SCREEN_SHAKE.get().toFloat() / 100.0f
        if (shakeStrength <= 0.0f) return

        cameraRot[0] = -boneRotX * shakeStrength
        cameraRot[1] = -boneRotY * shakeStrength
        cameraRot[2] = -boneRotZ * shakeStrength
    }

    private fun handlePlayerCamera(event: ViewportEvent.ComputeCameraAngles) {
        val yaw = event.yaw
        val pitch = event.pitch
        val roll = event.roll
        val times = getDelta().coerceAtMost(0.8f)
        val player = localPlayer

        if (GLFW.glfwGetKey(mc.window.window, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS) {
            cameraLocation = (cameraLocation - 0.05 * getDelta()).coerceIn(-0.6, 0.6)
        }

        if (GLFW.glfwGetKey(mc.window.window, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS) {
            cameraLocation = (cameraLocation + 0.05 * getDelta()).coerceIn(-0.6, 0.6)
        }

        if (player == null) return

        val lookingEntity = SeekTool.seekEntity(player, 520.0, 5.0)
        val range: Double =
            if (lookingEntity != null) {
                player.distanceTo(lookingEntity).coerceAtLeast(0.01f).toDouble()
            } else {
                player.position().distanceTo(
                    (Vec3.atLowerCornerOf(
                        player.level().clip(
                            ClipContext(
                                player.eyePosition,
                                player.eyePosition.add(player.lookAngle.scale(520.0)),
                                ClipContext.Block.OUTLINE,
                                ClipContext.Fluid.NONE,
                                player
                            )
                        ).blockPos
                    ))
                ).coerceAtLeast(0.01)
            }

        lookDistance = Mth.lerp(0.2 * times, lookDistance, range)

        val angle =
            if (lookDistance != 0.0 && cameraLocation != 0.0) {
                atan(abs(cameraLocation) / (lookDistance + 2.9)) * Mth.RAD_TO_DEG
            } else {
                0.0
            }



        event.pitch = (pitch + cameraRot[0] + 3 * velocityY).toFloat()
        if (mc.options.cameraType == CameraType.THIRD_PERSON_BACK) {
            event.yaw =
                (yaw + cameraRot[1] - angle * zoomPos).toFloat()
        } else {
            event.yaw =
                (yaw + cameraRot[1]).toFloat()
        }

        cameraRoll =
            (roll + cameraRot[2]).toFloat()
    }

    private fun handleBowPullAnimation(entity: LivingEntity, stack: ItemStack) {
        val times = 4 * getDelta().coerceAtMost(0.8f)
        val data = GunData.from(stack)
        val fireModeInfo = data.selectedFireModeInfo()
        if (!fireModeInfo.isChargeMode()) return

        if (chargeActive && fireModeInfo.mode == FireMode.CHARGE) {
            bowPull = true
            bowPower = chargePower
            bowPullTimer = chargeProgress * 1.4
        } else {
            bowPull = false
            bowPullTimer = (bowPullTimer - 0.021 * times).coerceAtLeast(0.0)
            bowPower = (bowPower - 0.04 * times).coerceAtLeast(0.0)
        }

        bowPullPos = 0.5 * cos(PI * (bowPullTimer.coerceIn(0.0, 1.0).pow(2) - 1).pow(2)) + 0.5
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun captureFov(event: ViewportEvent.ComputeFov) {
        if (event.usedConfiguredFov()) {
            fov = event.fov
        }
    }

    @SubscribeEvent
    fun onFovUpdate(event: ViewportEvent.ComputeFov) {
        val player = localPlayer ?: return
        val times = getDelta().coerceAtMost(1.6f)

        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.banHand(player) && zoomVehicle) {
            event.fov /= vehicle.getDefaultZoom(player)
            currentFov = event.fov
            return
        }

        // 当前操控的枪（部署中的副武器也算）—— 准心的抑制规则对两者一致
        val stack = ActiveGun.stackOf(player)

        val factor: Double =
            if (player.isUsingItem && player.useItem.`is`(ModItems.ARTILLERY_INDICATOR.get())
                && mc.options.cameraType == CameraType.FIRST_PERSON
            ) {
                4.0 + artilleryIndicatorCustomZoom
            } else {
                1.0
            }

        artilleryIndicatorZoom = Mth.lerp(0.3 * times, artilleryIndicatorZoom, factor)

        event.fov /= artilleryIndicatorZoom

        // ⚠ 判据必须是 `isOperable`（"这个栈能不能被当成一把枪操作"）而**不是** `isHeldWeapon`
        // （"这件物品拿在手上算不算枪"）。
        //
        // `stack` 是 `ActiveGun.stackOf(player)` —— **部署中的副武器**，而 `SubWeaponItem`
        // 的 `useAsWeaponInHand()` 是 `false`（§8.3.1：手持副武器**物品本身**时按普通物品处理）。
        // 用 `isHeldWeapon` 会让**整段 FOV/倍率计算被跳过** → 副武器瞄准时**完全没有放大**，
        // 而瞄准位形照旧生效（那一条走渲染侧的 `computeViewTransform`，早就是 `ActiveGun` 了），
        // 于是表现成"枪抬起来了、镜头一点没变"（§9.8.1 的那个坑，这里是最后一个漏改的读取点）。
        //
        // 手持副武器**物品本身**时不会有副作用：`ActiveGun.stackOf` 对那种情况返回 `EMPTY`，
        // 下面第一句就挡掉了。
        if (GunItem.isOperable(stack)) {
            if (!event.usedConfiguredFov()) {
                lastX = player.xRot
                lastY = player.yRot
                return
            }

            val p = if (stack.`is`(ModItems.BOCEK.get())) {
                bowPullPos * zoomTime
            } else {
                zoomPos
            }

            val data = GunData.from(stack)

            // ⚠ **倍率必须和「瞄准位形」取同一个来源**（§9.8.6）：
            //
            // - 副武器模型自己有 `iron_view` → 玩家眼睛贴在**副武器自己的**照门上，倍率也用它自己的；
            // - 没有（当前 GP-25 就是这种）→ `GeoGunRenderer` 的位形链会回退到**宿主枪**的
            //   `scope_view`/`iron_view`，那倍率也**必须**用宿主枪的 —— 包括它装着的瞄具倍率。
            //
            // 只改位形不改倍率就会变成"眼睛贴着主武器的 4 倍镜、FOV 却是 1 倍"
            // （四期实现时漏掉的一半，见 §11.10.10）。
            //
            // 判据分两步，两步都不能省：
            // ① **当前操控的是不是副武器** —— 用 `isSubWeapon`（物品是 `SubWeaponItem`），
            //    **不能**用 `isDeployed(data)`：部署状态写在**宿主枪**的 `ActiveSlot` 上，
            //    副武器自己那份数据里那个字段永远是空的；
            // ② **副武器有没有自己的瞄准位形** —— 问的是**宿主枪**（"它身上挂着的那个副武器
            //    模型有没有 `iron_view`"），与渲染侧共用同一个函数，不会两边各判一套。
            val hostGun = ActiveGun.mainGun(player)
            val zoomData =
                if (ActiveGun.isSubWeapon(data) && hostGun != null && !GeoGunRenderer.subWeaponHasOwnAimPose(hostGun)) {
                    hostGun
                } else {
                    data
                }

            val baseZoom = zoomData.zoom()
            val movingZoom = zoomData.movingZoom()

            this.movingZoom = if (movingZoom != null && !player.isShiftKeyDown) Mth.lerp(
                0.1 * times,
                this.movingZoom,
                if (isMoving() || abs(turnRot[1]) > 0.1 || abs(turnRot[0]) > 0.1) movingZoom else baseZoom
            ) else baseZoom
            customZoom = Mth.lerp(0.6 * times, customZoom, this.movingZoom + if (breath) 0.75 else 0.0)

            if (mc.options.cameraType.isFirstPerson) {
                event.fov /= (1 + p * (customZoom - 1))
            } else if (mc.options.cameraType == CameraType.THIRD_PERSON_BACK)
                event.fov /= (1 + p * 0.01)
            currentFov = event.fov

            // 智慧芯片
            if (zoom && !notInGame && drawTime < 0.01 && !isEditing) {
                if (player.isShiftKeyDown) {
                    lockedEntity = null
                } else {
                    val intelligentChipLevel = data.perk.getLevel(ModPerks.INTELLIGENT_CHIP)
                    val seekRange = 32.0 + 8.0 * (intelligentChipLevel - 1)

                    if (intelligentChipLevel > 0) {
                        if (lockedEntity == null || !lockedEntity!!.isAlive) {
                            lockedEntity = if (data.perk.has(ModPerks.PHASE_PENETRATING_BULLET.get())
                                || data.perk.has(ModPerks.BEAST_BULLET.get())
                            ) {
                                SeekTool.seekEntityThroughWall(player, seekRange, 16 / customZoom)
                            } else {
                                SeekTool.seekLivingEntity(player, seekRange, 16 / customZoom)
                            }
                        }
                        if (lockedEntity != null && lockedEntity!!.isAlive) {
                            val targetVec = lockedEntity!!.getEyePosition(event.partialTick.toFloat())
                            val playerVec = player.getEyePosition(event.partialTick.toFloat())

                            val hasGravity = data.perk.getLevel(ModPerks.MICRO_MISSILE) <= 0
                            val velocity =
                                if (stack.`is`(ModItems.BOCEK.get())) {
                                    zoomTime * 24
                                } else {
                                    data.get(GunProp.VELOCITY)
                                }

                            val toVec = RangeTool.calculateFiringSolution(
                                playerVec,
                                targetVec,
                                lockedEntity!!.deltaMovement.scale(0.5),
                                velocity,
                                if (hasGravity) data.get(GunProp.GRAVITY) else 0.0
                            )

                            look(player, toVec)

                            if (player.distanceTo(lockedEntity!!) > seekRange) {
                                lockedEntity = null
                            }
                        }
                    }
                }
            } else {
                lockedEntity = null
            }

            lastX = player.xRot
            lastY = player.yRot
        }

        if (stack.`is`(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using")
            && stack.getOrCreateTag().getBoolean("Linked")
        ) {
            droneFovLerp = Mth.lerp(0.1 * getDelta(), droneFovLerp, droneFov)
            event.fov /= droneFovLerp
            currentFov = event.fov
        }
    }

    fun look(player: Player, target: Vec3) {
        val d0 = target.x
        val d1 = target.y
        val d2 = target.z
        val d3 = sqrt(d0 * d0 + d2 * d2)

        val fromX = lastX
        val fromY = Mth.wrapDegrees(lastY)
        val toX = Mth.wrapDegrees(-(Mth.atan2(d1, d3) * 57.2957763671875)).toFloat()
        val toY = Mth.wrapDegrees((Mth.atan2(d2, d0) * 57.2957763671875) - 90F).toFloat()

        val diffY = Mth.wrapDegrees(toY - fromY)
        val finalY = Mth.wrapDegrees(fromY + diffY * 0.2F)

        player.xRot = Mth.wrapDegrees(Mth.lerp(0.2F, fromX, toX))
        player.yRot = Mth.wrapDegrees(finalY)
    }

    @SubscribeEvent
    fun setPlayerInvisible(event: RenderPlayerEvent.Pre) {
        val otherPlayer = event.entity
        val vehicle = otherPlayer.vehicle
        if (vehicle is VehicleEntity && vehicle.hidePassenger(otherPlayer)) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun handleRenderCrossHair(event: RenderGuiLayerEvent.Pre) {
        if (event.name != VanillaGuiLayers.CROSSHAIR) return
        val player = localPlayer ?: return

        // When combat HUD is hidden by server, suppress vanilla crosshair in ALL views
        if (MiscConfig.HIDE_COMBAT_HUD.get()) {
            // 当前操控的枪（部署中的副武器也算）：它有自己的准心，原版准心要收掉
            val held = ActiveGun.stackOf(player)
            if (GunItem.isOperable(held)) {
                event.isCanceled = true
                return
            }
            val vehicle = player.vehicle
            if (vehicle is VehicleEntity && vehicle.hasWeapon(vehicle.getSeatIndex(player))) {
                event.isCanceled = true
                return
            }
        }

        if (!mc.options.cameraType.isFirstPerson) return

        if (player.isUsingItem && player.useItem.`is`(ModItems.ARTILLERY_INDICATOR.get())) {
            event.isCanceled = true
        }

        val stack = ActiveGun.stackOf(player)
        // 主手拿着枪、或副武器被切了出来：都收掉原版准心
        if (GunItem.isOperable(stack)) {
            event.isCanceled = true
        }

        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.hasWeapon(vehicle.getSeatIndex(player))) {
            event.isCanceled = true
        }

        if (vehicle is VehicleEntity && vehicle.banHand(player)) {
            event.isCanceled = true
        }

        if (stack.`is`(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using")
            && stack.getOrCreateTag().getBoolean("Linked")
        ) {
            event.isCanceled = true
        }
    }

    /**
     * 载具banHand时，禁用快捷栏渲染
     */
    @SubscribeEvent
    fun handleAvoidRenderingHotbar(event: RenderGuiLayerEvent.Pre) {
        if (event.name != VanillaGuiLayers.HOTBAR) return
        val player = localPlayer ?: return
        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.banHand(player)) {
            event.isCanceled = true
        }
    }

    /**
     * **真的换了一把枪**（主手物品变了）：演一次切枪动画，并清掉上一把枪的全部残留。
     *
     * ⚠ **主/副武器切换不要调它。** 那种情况下手里那把枪根本没变，换的只是"当前操控的枪"——
     * 副武器是挂在同一把枪上的附件。调它会让主武器凭空做一次重新装备（`drawTime` 打回 1.0），
     * 还会把玩家按住的瞄准键作废（`zoom = false`）。那种情况调 [resetGunTransientState]。
     *
     * 调用点只有两个：`DrawClientMessage`（服务端在主手物品真换了时发）与
     * `handleShootDelay` 里的主手 UUID 变化。
     */
    fun resetGunStatus() {
        drawTime = 1.0
        zoom = false
        resetGunTransientState()
    }

    /**
     * 只清"上一把枪残留的运行时状态"，**不动 `drawTime`、也不动瞄准意图（`zoom`）**。
     *
     * 为什么必须清：下面这些字段在客户端是**全局的、不是按枪存的**（`burstFireAmount`、蓄力、
     * 自定义 RPM、索敌/锁定、抛壳计时……）。只要"当前操控的枪"换了就得清，
     * 否则 AK-12 打了一半的三连发会接着算到 GP-25 头上、蓄力进度会跨枪残留。
     *
     * 为什么不能顺带演切枪：这些状态归零和"手上的枪换了"是两件事。主/副武器切换只发生前者，
     * 而 `drawTime = 1.0` 是**重新装备的进度条** —— 副武器是挂在同一把枪上的附件，
     * 收起/端起来都不该让主武器做一次切枪动作（§9.8.2 / §9.8.8）。
     *
     * `zoomTime = 0.0` **留在这一侧**是有意的：瞄准意图（`zoom`）跨切换保留，
     * 但瞄准**进度**归零 → 切过去之后是"从肩上一路对进新枪的照门"，
     * 而不是让 FOV 在旧位形上直接跳到新枪的倍率（那一下是硬切，看不出瞄准点变了）。
     */
    fun resetGunTransientState() {
        for (i in 0..<5) {
            shellIndexTime[i] = 0.0
        }
        clientTimer.stop()
//        holdingFireKey = false
        holdingFireKeyTicks = 0
        holdingFireKeyTicks0 = 0f
        ClickEventHandler.switchZoom = false
        burstFireAmount = 0
        bowPull = false
        bowPullTimer = 0.0
        bowPower = 0.0
        bowPullPos = 0.0
        chargeActive = false
        chargeProgress = 0.0
        chargePower = 0.0
        noSprintTicks = 10f
        seekingTime = 0
        lockOn = false
        lockingEntity = null
        seekingEntity = null
        lockingPos = null
        isEditing = false
        // 切枪时清掉上一把枪累积的自定义 rpm，避免加成串到新枪上
        customRpm = 0
        editingAttachmentType = -1
        editFocusOffset.set(0f, 0f, 0f)
        editFocusYaw = 0f
        editFocusPitch = 0f
        editFocusReturnTime = 0f
        zoomTime = 0.0
    }

    fun resetLungeMineStatus() {
        lungeDraw = 30
        lungeSprint = 0
        lungeAttack = 0
        usingLunge = false
    }

    private fun handleWeaponDraw(entity: LivingEntity) {
        val times = getDelta()
        // ⚠ `DrawTime` / `Weight` 恒读**主武器**：`drawTime` 是"重新端枪"的进度条，
        // 而端在手里的始终是主武器（§9.8.8）。读副武器的话挂上 GP-25 之后
        // 整把 AK 的重新装备只要 `1 + 0.5 * 0` 个 tick，像是瞬移。
        val handling = ActiveGun.handlingData(entity) ?: return
        val weight = (handling.stack.item as? GunItem)?.getCustomWeight(handling) ?: 0.0
        val duration = handling.get(GunProp.DRAW_TIME).coerceAtLeast(1) + 0.5 * weight
        val decay = ln(100.0) / duration
        drawTime = (drawTime - decay * times * drawTime).coerceAtLeast(0.0)
    }

    @JvmStatic
    fun handleShells(x: Float, y: Float, vararg shells: GeoBone) {
        for ((i, element) in shells.withIndex()) {
            if (i >= 5) break
            element.posX = (-x * shellIndexTime[i] * ((150 - shellIndexTime[i]) / 150)).toFloat()
            element.posY = (y * randomShell[0] * shellIndexTime[i] - 0.025 * shellIndexTime[i].pow(2)).toFloat()
            element.rotX = (randomShell[1] * shellIndexTime[i]).toFloat()
            element.rotY = (randomShell[2] * shellIndexTime[i]).toFloat()
        }
    }

    fun aimAtVillager(player: Player) {
        if (aimVillagerCountdown > 0) return

        if (zoom) {
            val entity = OverlayTraceHandler.playerReachEntity as? AbstractVillager ?: return
            val entities = SeekTool.seekLivingEntities(entity, 16.0, 120.0)
            for (e in entities) {
                if (e == player) {
                    sendPacketToServer(AimVillagerMessage(entity.id))
                    aimVillagerCountdown = 80
                }
            }
        }
    }

    /**
     * 能否开启改枪GUI，只有在当前没有待发射的子弹，且物品为武器，主手持有的情况下才能开启
     *
     * @param stack 待改装武器
     * @param hand  持有武器的手
     * @return 能否成功打开GUI
     */
    @JvmStatic
    fun canOpenEditScreen(stack: ItemStack, hand: InteractionHand?): Boolean {
        // 手持副武器时按普通物品处理
        return burstFireAmount == 0 && GunItem.isHeldWeapon(stack) && hand == InteractionHand.MAIN_HAND
    }

    @JvmStatic
    fun onOpenEditScreen() {
        val player = localPlayer ?: return
        isEditing = true
        editingAttachmentType = -1
        holdingFireKey = false
        player.playSound(ModSounds.EDIT_MODE.get(), 1f, 1f)
    }

    @JvmStatic
    fun onCloseEditScreen() {
        isEditing = false
        editingAttachmentType = -1
        editFocusReturnTime = 0f
    }

    @JvmStatic
    fun editModelShake() {
        velocityY = 0.2
    }

    @JvmStatic
    fun stopSoundEvent(location: ResourceLocation, source: SoundSource) {
        mc.soundManager.stop(location, source)
    }

    @JvmStatic
    fun stopVehicleSeekSound(player: Player?) {
        if (player == null) return
        val vehicle = player.vehicle
        if (vehicle is VehicleEntity) {
            val gunData = vehicle.getGunData(player) ?: return
            val location = gunData.get(GunProp.SOUND_INFO).locking.location
            stopSoundEvent(location, SoundSource.PLAYERS)
        }
    }

    @JvmStatic
    fun stopWeaponSeekSound(player: Player?) {
        if (player == null) return
        // 当前操控的枪：切换主/副武器时要停掉的是**正在操作那把**的锁定音
        val stack = ActiveGun.stackOf(player)
        if (GunItem.isOperable(stack)) {
            val gunData = GunData.from(stack)
            val location = gunData.get(GunProp.SOUND_INFO).locking.location
            stopSoundEvent(location, SoundSource.PLAYERS)
        }
    }

    @JvmStatic
    fun stopVehicleReloadSound(player: Player?) {
        if (player == null) return
        val vehicle = player.vehicle
        if (vehicle is VehicleEntity) {
            val gunData = vehicle.getGunData(player) ?: return
            val location = gunData.get(GunProp.SOUND_INFO).vehicleReload.location
            stopSoundEvent(location, SoundSource.PLAYERS)
        }
    }

    @SubscribeEvent
    fun onRenderNameTag(event: RenderNameTagEvent) {
        val entity = event.entity as? Player ?: return
        val self = localPlayer ?: return
        if (self == entity) return
        if (self.vehicle !is VehicleEntity) return
        if (self.isPassengerOfSameVehicle(entity)) {
            event.setCanRender(TriState.FALSE)
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onPlayerLoggedIn(event: PlayerEvent.PlayerLoggedInEvent) {
        if (!DisplayConfig.ENABLE_VERSION_CHECK_WARNING.get()) return
        val player = event.entity ?: return
        if (ModVersionEventHandler.currentVersion == null || ModVersionEventHandler.previousVersion == null) return

        player.displayClientMessage(
            Component.translatable(
                "tips.superbwarfare.vehicle_reset_kit_1",
                Component.literal("" + ModVersionEventHandler.previousVersion).withStyle(ChatFormatting.YELLOW),
                Component.literal("" + ModVersionEventHandler.currentVersion).withStyle(ChatFormatting.YELLOW)
            )
                .withStyle(ChatFormatting.RED), false
        )
        player.displayClientMessage(
            Component.translatable(
                "tips.superbwarfare.vehicle_reset_kit_2",
                Component.literal("[").append(ModItems.VEHICLE_RESET_KIT.get().defaultInstance.hoverName)
                    .append("]").withStyle(ChatFormatting.GREEN)
            ), false
        )
        player.displayClientMessage(
            Component.translatable("tips.superbwarfare.vehicle_reset_kit_3")
                .withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.UNDERLINE), false
        )
    }

    @SubscribeEvent
    fun onFogColor(event: ViewportEvent.ComputeFogColor) {
        if (activeThermalImaging) {
            event.red = 0.1F
            event.green = 0.1F
            event.blue = 0.1F
        }
    }

    @SubscribeEvent
    fun onClientVehicleFire(event: ClientVehicleFireEvent) {
        val shooter = event.shooter
        val vehicle = event.vehicle
        val index = event.index

        VehicleLightingHandler.onVehicleFire(event)

        val ani = vehicle.getAnimationInstance() ?: return
        val name = event.weaponName
            ?: vehicle.getGunName(vehicle.getSeatIndex(shooter))
            ?: return
        ani.fire(name.camelToSnake(), index)
    }

    @SubscribeEvent
    fun onClientGunFire(event: ClientGunFireEvent) {
        val instance =
            FirstPersonRenderHandler.getActiveAnimationInstance(event.hand) as? GeoGunAnimationInstance ?: return

        // **部署中的副武器开火**：开火动画的候选链写在副武器自己的定义里
        // （`SubWeaponInfo.Animation`，默认 `["fire_sub_weapon"]`），短名由 `triggerFire`
        // 按**宿主枪**的 id 拼（`animation.ak_12.fire_sub_weapon`，§11.9-A）。
        //
        // ⚠ **这一句不能省。** `triggerFire` 是拿"候选链空不空"判断"开火的是不是副武器"的：
        // 不传候选 → 判定成主武器开火 → **打榴弹时步枪抛壳**，而且副武器专属的开火动画
        // 永远解析不到（候选链整条是死代码）。四期把参数留在原地却没接上，见 §11.10.10。
        val subWeaponInfo = subWeaponInfoOf(event.stack)
        instance.triggerFire(
            event.stack,
            subWeaponInfo?.fireAnimationCandidates() ?: emptyList(),
            subWeaponInfo?.hasExplicitFireAnimation == true,
        )
    }

    /** 这个栈是副武器物品时返回它自己的配件定义（含 `SubWeaponInfo`），否则 `null` */
    private fun subWeaponInfoOf(stack: ItemStack): SubWeaponInfo? {
        if (stack.item !is SubWeaponItem) return null
        val id = BuiltInRegistries.ITEM.getKey(stack.item)
        return AttachmentDefinition.from(id)?.subWeapon
    }
}
