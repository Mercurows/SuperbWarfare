package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.client.MouseMovementHandler
import com.atsuishio.superbwarfare.config.client.ControlConfig
import com.atsuishio.superbwarfare.data.gun.ActiveGun
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.send.MouseMoveMessage
import com.atsuishio.superbwarfare.network.message.send.TurretAngleMessage
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import com.atsuishio.superbwarfare.tools.notInGame
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import net.minecraft.client.CameraType
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec2
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ViewportEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import kotlin.math.abs

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object ClientMouseHandler {
    @JvmField
    var posO: Vec2 = Vec2(0f, 0f)

    @JvmField
    var posN: Vec2 = Vec2(0f, 0f)

    @JvmField
    var lerpSpeedX: Double = 0.0

    @JvmField
    var lerpSpeedY: Double = 0.0

    @JvmField
    var speedX: Double = 0.0

    @JvmField
    var speedY: Double = 0.0

    @JvmField
    var freeCameraPitch: Double = 0.0

    @JvmField
    var freeCameraYaw: Double = 0.0

    @JvmField
    var custom3pDistance: Double = 0.0

    @JvmField
    var custom3pDistanceLerp: Double = 0.0

    @JvmField
    var mouseXMoveTick: Double = 0.0

    @JvmField
    var mouseYMoveTick: Double = 0.0

    @JvmField
    var lerpNacelleSpeedX: Double = 0.0

    @JvmField
    var lerpNacelleSpeedY: Double = 0.0

    @JvmField
    var nacelleCameraPitch: Double = 0.0

    @JvmField
    var nacelleCameraYaw: Double = 0.0

    @SubscribeEvent
    fun handleClientTick(event: TickEvent.ClientTickEvent) {
        val player = localPlayer ?: return
        if (event.phase == TickEvent.Phase.START) {
            this.accumulateDirectTurretInput(player)
            return
        }

        posO = posN
        posN = MouseMovementHandler.getMousePos()
        val speed = 256f
        val moveSpeedX = Mth.clamp(posN.x - posO.x, -speed, speed)
        val moveSpeedY = Mth.clamp(posN.y - posO.y, -speed, speed)

        if (!notInGame) {
            val directVehicle = player.vehicle as? VehicleEntity
            if (directVehicle != null &&
                directVehicle.hasTurret() &&
                directVehicle.turretControllerIndex == directVehicle.getSeatIndex(player)
            ) {
                val directMode = mc.options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle
                sendPacketToServer(
                    TurretAngleMessage(directMode, directVehicle.turretYRot, directVehicle.turretXRot)
                )
            }
        }

        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && (vehicle.vehicleType == VehicleType.AIRPLANE || vehicle.vehicleType == VehicleType.HELICOPTER)) {
            val y = if (ControlConfig.INVERT_AIRCRAFT_CONTROL.get()) -1 else 1
            val sensitivity = vehicle.mouseSensitivity

            speedX = sensitivity * moveSpeedX * (if (ClientEventHandler.zoomVehicle && !ClientEventHandler.isNacelleCam(
                    player
                )
            ) 0.3 else 1.0)
            speedY =
                y * sensitivity * moveSpeedY * (if (ClientEventHandler.zoomVehicle && !ClientEventHandler.isNacelleCam(
                        player
                    )
                ) 0.4 else 1.0)

            mouseXMoveTick = Mth.lerp(0.1, mouseXMoveTick, speedX)
            mouseYMoveTick = Mth.lerp(0.1, mouseYMoveTick, speedY)

            if (vehicle.vehicleType == VehicleType.AIRPLANE) {
                lerpSpeedX = Mth.lerp((0.006 * abs(mouseXMoveTick)).coerceAtLeast(0.12), lerpSpeedX, speedX)
                lerpSpeedY = Mth.lerp((0.005 * abs(mouseYMoveTick)).coerceAtLeast(0.12), lerpSpeedY, speedY)
            } else {
                lerpSpeedX = Mth.lerp((0.0045 * abs(mouseXMoveTick)).coerceAtLeast(0.1), lerpSpeedX, speedX * 0.5)
                lerpSpeedY = Mth.lerp((0.0035 * abs(mouseYMoveTick)).coerceAtLeast(0.1), lerpSpeedY, speedY * 0.5)
            }

            lerpNacelleSpeedX =
                Mth.lerp((0.05 * abs(mouseXMoveTick)).coerceAtLeast(0.13), lerpNacelleSpeedX, speedX * 1.4)
            lerpNacelleSpeedY =
                Mth.lerp((0.05 * abs(mouseYMoveTick)).coerceAtLeast(0.13), lerpNacelleSpeedY, speedY * 1.4)

            // 盘旋模式下禁止操控
            if (vehicle.loiterActive && vehicle.computed().engineType == EngineType.AIRCRAFT) {
                return
            }

            if (player != vehicle.firstPassenger || vehicle.getSeatIndex(player) != 0) return

            if (notInGame) {
                sendPacketToServer(MouseMoveMessage(0.0, 0.0))
            } else if (ClientEventHandler.isFreeCam(player) || ClientEventHandler.isNacelleCam(player)) {
                sendPacketToServer(MouseMoveMessage(0.0, 0.0))
            } else {
                // 三轴全部本体局部操控：鼠标 X/Y 直接对应本体 yaw/pitch，
                // 不再按 roll 做欧拉万向混合（既然引擎侧也是局部轴）。
                sendPacketToServer(MouseMoveMessage(lerpSpeedX, lerpSpeedY))
            }

        }

    }

    private var directTurretLastPos: Vec2? = null
    private fun accumulateDirectTurretInput(player: Player) {
        val vehicle = player.vehicle as? VehicleEntity
        // 第三人称非瞄准不累积：回归旧“炮塔追随视线”瞄准
        val directMode = mc.options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle
        val active = vehicle != null &&
                directMode &&
                vehicle.hasTurret() &&
                vehicle.turretControllerIndex == vehicle.getSeatIndex(player) &&
                !vehicle.isWreck &&
                !notInGame &&
                !ClientEventHandler.isFreeCam(player) &&
                !ClientEventHandler.isNacelleCam(player) &&
                mc.mouseHandler.isMouseGrabbed &&
                mc.isWindowActive

        if (!active) {
            directTurretLastPos = null
            directTurretLastPitchDelta = 0.0
            return
        }

        val current = MouseMovementHandler.getMousePos()
        val last = directTurretLastPos
        directTurretLastPos = current
        if (last == null) return

        val dx = Mth.clamp(current.x - last.x, -256.0f, 256.0f).toDouble()
        val dy = Mth.clamp(current.y - last.y, -256.0f, 256.0f).toDouble()
        if (dx == 0.0 && dy == 0.0) return

        val baseSensitivity = mc.options.sensitivity().get() * 0.6 + 0.2
        val sensitivity = changeSensitivity(baseSensitivity)
        if (sensitivity <= 0.0) return

        val h = sensitivity * sensitivity * sensitivity * 8.0 * 0.15
        val invert = (if (mc.options.invertYMouse().get()) -1.0 else 1.0) * invertY()

        var yawDeg = dx * h
        var pitchDeg = dy * h * invert

        if (mc.options.cameraType == CameraType.FIRST_PERSON) {
            val roll = vehicle.roll
            val absRoll = abs(roll)
            if (absRoll > 1f) {
                val a = (absRoll / 90f).coerceAtMost(1f)
                var i = if (roll < 0f) 1.0 else -1.0
                if (absRoll > 90f) {
                    i *= 1 - (absRoll - 90f) / 90f
                }
                val newYawDeg = (1 - a) * yawDeg + a * directTurretLastPitchDelta * i
                val newPitchDeg = (1 - a) * pitchDeg + a * yawDeg * (if (roll < 0f) -1.0 else 1.0)
                yawDeg = newYawDeg
                pitchDeg = newPitchDeg
            }
        }
        directTurretLastPitchDelta = dy * h * invert

        vehicle.addDirectTurretInput(yawDeg.toFloat(), pitchDeg.toFloat())
    }

    private var directTurretLastPitchDelta = 0.0

    @Suppress("unused")
    @SubscribeEvent
    fun handleClientTick(event: ViewportEvent.ComputeCameraAngles) {
        val player = localPlayer ?: return

        if (notInGame) {
            freeCameraYaw = 0.0
            freeCameraPitch = 0.0
            return
        }

        val times = mc.deltaFrameTime

        freeCameraYaw -= 0.2f * times * lerpSpeedX
        freeCameraPitch += 0.15f * times * lerpSpeedY

        val vehicle = player.vehicle
        val hover = vehicle is VehicleEntity && vehicle.vehicleType == VehicleType.HELICOPTER && vehicle.hoverMode

        if (!ClientEventHandler.isFreeCam(player)) {
            var s = if (mc.options.cameraType == CameraType.FIRST_PERSON) 0.6 else 0.2
            if (hover) {
                s *= 0.5
            }
            freeCameraYaw = Mth.lerp(s * times, freeCameraYaw, 0.0)
            freeCameraPitch = Mth.lerp(s * times, freeCameraPitch, 0.0)
        }

        while (freeCameraYaw > 180F) {
            freeCameraYaw -= 360
        }
        while (freeCameraYaw <= -180F) {
            freeCameraYaw += 360
        }
        while (freeCameraPitch > 180F) {
            freeCameraPitch -= 360
        }
        while (freeCameraPitch <= -180F) {
            freeCameraPitch += 360
        }

        custom3pDistanceLerp = Mth.lerp(times.toDouble(), custom3pDistanceLerp, custom3pDistance)


        if (ClientEventHandler.isNacelleCam(player)) {
            nacelleCameraYaw -= 0.2f * times * lerpNacelleSpeedX
            nacelleCameraPitch += 0.2f * times * lerpNacelleSpeedY
        } else {
            nacelleCameraYaw = 0.0
            nacelleCameraPitch = 0.0
        }

        while (nacelleCameraYaw > 180F) {
            nacelleCameraYaw -= 360
        }
        while (nacelleCameraYaw <= -180F) {
            nacelleCameraYaw += 360
        }
        while (nacelleCameraPitch > 180F) {
            nacelleCameraPitch -= 360
        }
        while (nacelleCameraPitch <= -180F) {
            nacelleCameraPitch += 360
        }
    }

    /**
     * 反转鼠标
     */
    @JvmStatic
    fun invertY(): Int {
        val player = localPlayer ?: return 1
        val vehicle = player.vehicle as? VehicleEntity ?: return 1

        if ((vehicle.vehicleType == VehicleType.AIRPLANE || vehicle.vehicleType == VehicleType.HELICOPTER)
            && vehicle.firstPassenger == player
        ) {
            return if (ControlConfig.INVERT_AIRCRAFT_CONTROL.get()) -1 else 1
        }
        return 1
    }

    @JvmStatic
    fun changeSensitivity(original: Double): Double {
        val player = localPlayer ?: return original
        if (player.hasEffect(ModMobEffects.SHOCK.get()) && !player.isSpectator) {
            return 0.0
        }

        if (ClientEventHandler.isNacelleCam(player)) {
            return 0.0
        }

        if (player.vehicle is VehicleEntity) {
            val vehicle = player.vehicle as VehicleEntity
            val index = vehicle.getSeatIndex(player)
            if (ClientEventHandler.isFreeCam(player) && vehicle.useAircraftCamera(index)) {
                return 0.0
            }
        }

        if (player.isUsingItem && player.useItem.`is`(ModItems.ARTILLERY_INDICATOR.get()) && mc.options.cameraType == CameraType.FIRST_PERSON) {
            return original / (1 + 0.2 * ClientEventHandler.artilleryIndicatorZoom).coerceAtLeast(0.1)
        }

        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.banHand(player)) {
            return vehicle.getSensitivity(
                original,
                ClientEventHandler.zoomVehicle,
                vehicle.getSeatIndex(player),
                vehicle.onGround()
            )
        }

        // 当前操控的枪（部署中的副武器也算）：开镜灵敏度读的是**正在操作那把**的数据
        val activeStack = ActiveGun.stackOf(player)
        if (GunItem.isOperable(activeStack)) {
            val data = GunData.from(activeStack)
            val customSens = data.sensitivity.get()
            val zoom = ClientEventHandler.customZoom.coerceAtLeast(1.0)

            if (!activeStack.isEmpty && mc.options.cameraType == CameraType.FIRST_PERSON) {
                return original / (1 + (0.2 * (zoom - (0.3 * customSens)) * ClientEventHandler.zoomTime))
                    .coerceAtLeast(0.1) * (ControlConfig.MOUSE_SENSITIVITY.get() / 100f)
            }
        }

        return original
    }
}
