package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.tools.TraceTool
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(Dist.CLIENT)
object OverlayTraceHandler {
    @JvmField
    var playerReachEntity: Entity? = null

    @JvmField
    var maxRangeEntity: Entity? = null

    @JvmField
    var cameraEntity: Entity? = null

    @JvmField
    var cameraMaxRangeEntity: Entity? = null

    @JvmField
    var blockMaxRangeResult: BlockHitResult? = null

    @SubscribeEvent
    fun onOverlayTraceClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.START) return
        val player = localPlayer
        if (player == null) {
            clear()
            return
        }

        handlePlayerTrace(player)
        handleCameraTrace(player)
    }

    @JvmStatic
    fun handlePlayerTrace(player: Player) {
        val reachDistance = player.getEntityReach()
        val reachEntity = TraceTool.findLookingEntity(player, reachDistance)
        playerReachEntity = reachEntity
        if (reachEntity != null) {
            maxRangeEntity = reachEntity
            return
        }

        val maxEntity = TraceTool.findLookingEntity(player, reachDistance)
        maxRangeEntity = maxEntity
    }

    @JvmStatic
    fun handleCameraTrace(player: Player) {
        val camera = mc.gameRenderer.mainCamera
        var viewPos = camera.position
        var viewVec = Vec3(camera.lookVector)
        val distance = try {
            VehicleConfig.VEHICLE_INFO_DISPLAY_DISTANCE.get().toDouble()
        } catch (_: Exception) {
            196.0
        }

        val vehicle = player.vehicle
        if (vehicle is VehicleEntity
            && vehicle.hasWeapon(vehicle.getSeatIndex(player))
            && !ClientEventHandler.isNacelleCam(player)
        ) {
            viewVec = vehicle.getShootDirectionForHud(player, 1f)
            viewPos = vehicle.getShootPosForHud(player, 1f)
        }

        blockMaxRangeResult = player.level().clip(
            ClipContext(
                viewPos, viewPos.add(viewVec.scale(512.0)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player
            )
        )

        val cameraRes = TraceTool.cameraFindLookingEntity(player, viewPos, viewVec, distance)
        if (cameraRes is VehicleEntity) {
            val decoy = TraceTool.findLookDecoy(player, viewPos, viewVec, distance)
            if (decoy != null) {
                cameraEntity = null
                maxRangeEntity = null
                return
            }
        }
        cameraEntity = cameraRes
        if (cameraEntity != null) {
            maxRangeEntity = cameraRes
            return
        }

        val maxRangeRes = TraceTool.cameraFindLookingEntity(player, viewPos, viewVec, 512.0)
        maxRangeEntity = maxRangeRes
    }

    @JvmStatic
    fun clear() {
        playerReachEntity = null
        maxRangeEntity = null
        cameraEntity = null
        maxRangeEntity = null
        blockMaxRangeResult = null
    }
}