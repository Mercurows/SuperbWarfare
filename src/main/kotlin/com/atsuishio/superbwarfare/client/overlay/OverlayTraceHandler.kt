package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.tools.TraceTool
import com.atsuishio.superbwarfare.tools.getEntityReach
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import kotlin.math.sqrt

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT)
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

    @JvmField
    var playerViewBlockResult: BlockHitResult? = null

    @SubscribeEvent
    fun onOverlayTraceClientTick(event: ClientTickEvent.Post) {
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
        playerViewBlockResult = traceViewBlocks(player, player.eyePosition, player.getViewVector(1f), MAX_TRACE_DISTANCE)

        val reachDistance = player.getEntityReach()
        val reachEntity = TraceTool.findLookingEntity(player, reachDistance)
        playerReachEntity = reachEntity
        if (reachEntity != null) {
            maxRangeEntity = reachEntity
            return
        }

        val maxEntity = TraceTool.findLookingEntity(player, MAX_TRACE_DISTANCE)
        maxRangeEntity = maxEntity
    }

    /**
     * 把外部传入的方向归一化。载具数据/物理结构投影等来源可能给出未归一化、
     * 甚至非有限的向量；直接乘上 512 会生成几百万格的射线，造成客户端严重卡顿。
     */
    private fun safeDirection(direction: Vec3, fallback: Vec3): Vec3 {
        if (!isFinite(direction)) return fallback
        val lengthSqr = direction.lengthSqr()
        if (!lengthSqr.isFinite() || lengthSqr < 1.0E-8) return fallback
        return direction.scale(1.0 / sqrt(lengthSqr))
    }

    private fun isFinite(vec: Vec3): Boolean =
        vec.x.isFinite() && vec.y.isFinite() && vec.z.isFinite()

    /** 与 HUD 的视线检测保持一致；短距渲染查询可传入当前帧的起点，不依赖上一 tick 的缓存。 */
    @JvmStatic
    fun traceViewBlocks(entity: Entity, origin: Vec3, direction: Vec3, distance: Double): BlockHitResult {
        val fallbackDirection = entity.getViewVector(1f)
        val normalized = safeDirection(direction, fallbackDirection)
        val safeDistance = distance.coerceIn(0.0, MAX_TRACE_DISTANCE)
        val end = origin.add(normalized.scale(safeDistance))
        if (!isFinite(origin) || !isFinite(end)) {
            val missPos = if (isFinite(origin)) origin else entity.eyePosition
            return BlockHitResult.miss(missPos, Direction.DOWN, BlockPos.containing(missPos))
        }
        return entity.level().clip(
            ClipContext(origin, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity)
        )
    }

    @JvmStatic
    fun handleCameraTrace(player: Player) {
        val camera = mc.gameRenderer.mainCamera
        var viewPos = camera.position
        var viewVec = safeDirection(Vec3(camera.lookVector), Vec3(0.0, 0.0, 1.0))
        val distance = try {
            VehicleConfig.VEHICLE_INFO_DISPLAY_DISTANCE.get().toDouble()
        } catch (_: Exception) {
            196.0
        }.coerceIn(0.0, MAX_TRACE_DISTANCE)

        val vehicle = player.vehicle
        if (vehicle is VehicleEntity
            && vehicle.hasWeapon(vehicle.getSeatIndex(player))
            && !ClientEventHandler.isNacelleCam(player)
        ) {
            viewVec = safeDirection(vehicle.getShootDirectionForHud(player, 1f), viewVec)
            viewPos = vehicle.getShootPosForHud(player, 1f)
        }

        if (!isFinite(viewPos)) {
            viewPos = player.eyePosition
        }

        blockMaxRangeResult = player.level().clip(
            ClipContext(
                viewPos, viewPos.add(viewVec.scale(MAX_TRACE_DISTANCE)),
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

        val maxRangeRes = TraceTool.cameraFindLookingEntity(player, viewPos, viewVec, MAX_TRACE_DISTANCE)
        maxRangeEntity = maxRangeRes
    }

    @JvmStatic
    fun clear() {
        playerReachEntity = null
        maxRangeEntity = null
        cameraEntity = null
        cameraMaxRangeEntity = null
        blockMaxRangeResult = null
        playerViewBlockResult = null
    }

    private const val MAX_TRACE_DISTANCE = 512.0
}
