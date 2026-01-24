package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.client.MouseMovementHandler
import com.atsuishio.superbwarfare.config.client.ControlConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.vehicle.Tom6Entity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.send.MouseMoveMessage
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import net.minecraft.client.CameraType
import net.minecraft.util.Mth
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

    private fun notInGame(): Boolean {
        val mc = mc
        if (mc.player == null) return true
        if (mc.overlay != null) return true
        if (mc.screen != null) return true
        if (!mc.mouseHandler.isMouseGrabbed) return true
        return !mc.isWindowActive
    }

    @SubscribeEvent
    fun handleClientTick(event: TickEvent.ClientTickEvent) {
        val player = localPlayer ?: return
        if (event.phase == TickEvent.Phase.START) return

        if (notInGame()) {
            speedX = 0.0
            speedY = 0.0
            lerpSpeedX = 0.0
            lerpSpeedY = 0.0
        }

        posO = posN
        posN = MouseMovementHandler.getMousePos()

        val stack = player.mainHandItem

        if (stack.`is`(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using")
            && stack.getOrCreateTag().getBoolean("Linked")
        ) {
            val drone =
                EntityFindUtil.findDrone(player.level(), stack.getOrCreateTag().getString("LinkedDrone")) ?: return

            if (notInGame()) {
                sendPacketToServer(MouseMoveMessage(0.0, 0.0))
                return
            }
            speedX = (drone.mouseSensitivity / ClientEventHandler.droneFovLerp) * (posN.x - posO.x)
            speedY = (drone.mouseSensitivity / ClientEventHandler.droneFovLerp) * (posN.y - posO.y)

            lerpSpeedX = Mth.lerp(0.3, lerpSpeedX, speedX)
            lerpSpeedY = Mth.lerp(0.3, lerpSpeedY, speedY)

            sendPacketToServer(MouseMoveMessage(lerpSpeedX, lerpSpeedY))
            return
        }

        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && player == vehicle.firstPassenger
            && (vehicle.vehicleType == VehicleType.AIRPLANE || vehicle.vehicleType == VehicleType.HELICOPTER)
        ) {
            if (notInGame()) {
                sendPacketToServer(MouseMoveMessage(0.0, 0.0))
                return
            }

            var y = 1
            if (ControlConfig.INVERT_AIRCRAFT_CONTROL.get()) {
                y = -1
            }

            speedX = vehicle.mouseSensitivity * (posN.x - posO.x) * (if (ClientEventHandler.zoomVehicle) 0.3 else 1.0)
            speedY =
                y * vehicle.mouseSensitivity * (posN.y - posO.y) * (if (ClientEventHandler.zoomVehicle) 0.4 else 1.0)

            mouseXMoveTick = Mth.lerp(0.1, mouseXMoveTick, speedX)
            mouseYMoveTick = Mth.lerp(0.1, mouseYMoveTick, speedY)

            if (vehicle.vehicleType == VehicleType.AIRPLANE) {
                lerpSpeedX = Mth.lerp((0.006 * abs(mouseXMoveTick)).coerceAtLeast(0.12), lerpSpeedX, speedX)
                lerpSpeedY = Mth.lerp((0.005 * abs(mouseYMoveTick)).coerceAtLeast(0.12), lerpSpeedY, speedY)
            } else {
                lerpSpeedX = Mth.lerp((0.0045 * abs(mouseXMoveTick)).coerceAtLeast(0.1), lerpSpeedX, speedX * 0.5)
                lerpSpeedY = Mth.lerp((0.0035 * abs(mouseYMoveTick)).coerceAtLeast(0.1), lerpSpeedY, speedY * 0.5)
            }

            var i = 0.0
            if (vehicle.roll < 0) {
                i = 1.0
            } else if (vehicle.roll > 0) {
                i = -1.0
            }

            if (Mth.abs(vehicle.roll) > 90) {
                i *= (1 - (Mth.abs(vehicle.roll) - 90) / 90)
            }

            if (!ClientEventHandler.isFreeCam(player)) {
                if (mc.options.cameraType == CameraType.FIRST_PERSON) {
                    if (vehicle !is Tom6Entity) {
                        sendPacketToServer(
                            MouseMoveMessage(
                                (1 - abs(vehicle.roll) / 90) * lerpSpeedX + (abs(vehicle.roll) / 90) * lerpSpeedY * i,
                                (1 - abs(vehicle.roll) / 90) * lerpSpeedY + (abs(vehicle.roll) / 90) * lerpSpeedX * if (vehicle.roll < 0) -1.0 else 1.0
                            )
                        )
                    }
                } else {
                    sendPacketToServer(MouseMoveMessage(lerpSpeedX, lerpSpeedY))
                }
            } else {
                sendPacketToServer(MouseMoveMessage(0.0, 0.0))
            }
        }
    }

    @Suppress("unused")
    @SubscribeEvent
    fun handleClientTick(event: ViewportEvent.ComputeCameraAngles) {
        val player = localPlayer ?: return

        if (notInGame()) {
            freeCameraYaw = 0.0
            freeCameraPitch = 0.0
            return
        }

        val times = mc.deltaFrameTime

        freeCameraYaw -= 0.2f * times * lerpSpeedX
        freeCameraPitch += 0.15f * times * lerpSpeedY
        if (!ClientEventHandler.isFreeCam(player)) {
            freeCameraYaw = Mth.lerp(0.6 * times, freeCameraYaw, 0.0)
            freeCameraPitch = Mth.lerp(0.6 * times, freeCameraPitch, 0.0)
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

        val stack = player.mainHandItem

        if (stack.`is`(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using")
            && stack.getOrCreateTag().getBoolean("Linked")
        ) {
            return 0.0
        }

        if (ClientEventHandler.isFreeCam(player)) {
            return 0.0
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

        if (stack.item is GunItem) {
            val data = GunData.from(stack)
            val customSens = data.sensitivity.get()

            if (!player.mainHandItem.isEmpty && mc.options.cameraType == CameraType.FIRST_PERSON) {
                return original / (1 + (0.2 * (data.zoom() - (0.3 * customSens)) * ClientEventHandler.zoomTime))
                    .coerceAtLeast(0.1) * (ControlConfig.MOUSE_SENSITIVITY.get() / 100f)
            }
        }

        return original
    }
}